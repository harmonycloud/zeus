package com.middleware.zeus.service.system.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.constants.NameConstant;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.DisasterRecoveryDto;
import com.middleware.caas.common.model.DisasterRecoveryInfo;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.tool.date.DateUtils;
import com.middleware.zeus.bean.BeanSystemConfig;
import com.middleware.zeus.integration.cluster.MiddlewareWrapper;
import com.middleware.zeus.integration.cluster.MysqlClusterWrapper;
import com.middleware.zeus.integration.cluster.MysqlReplicateWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MysqlCluster;
import com.middleware.zeus.integration.cluster.bean.MysqlReplicateCR;
import com.middleware.zeus.integration.cluster.bean.MysqlReplicateStatus;
import com.middleware.zeus.integration.platform.PlatformClient;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.PlatformService;
import com.middleware.zeus.service.system.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.middleware.caas.common.constants.NameConstant.RUNNING;

/**
 * @auther wangpenglei
 * @date 2023/3/22 10:35
 */
@Service
@Slf4j
public class PlatformServiceImpl implements PlatformService {
    @Value("${zeus.namespace:zeus}")
    private String zeusNamespace;

    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private MysqlReplicateWrapper mysqlReplicateWrapper;
    @Autowired
    private PlatformClient platformClient;
    @Autowired
    private MysqlClusterWrapper mysqlClusterWrapper;


    @Override
    public DisasterRecoveryDto queryAccessInfo() {
        DisasterRecoveryDto res = new DisasterRecoveryDto();
        JSONObject values = helmChartService.getZeusMysqlInstallValues();

        // 是否主平台
        res.setIsMaster("master-slave".equals(values.getString("type")));

        res.setLocal(getLocalPlatformAddress(values));
        res.setRelation(getRelationPlatformAddress(values));

        // 设置数据库状态
        res.getLocal().setPhase(getZusMysqlPhase());

        // 上次平台灾备切换时间
        if (values.containsKey("lastPlatformSwitchTime")) {
            res.setLastSwitchTime(
                DateUtils.parseDate(values.getString("lastPlatformSwitchTime"), DateUtils.YYYY_MM_DD_HH_MM_SS));
        }

        try {
            log.info("获取同步器状态");
            DisasterRecoveryDto disasterRecoveryDto;
            if (res.getIsMaster()) {
                JSONObject response =
                    platformClient.getMysqlReplicateStatus(CurrentUserRepository.getUser().getToken());
                JSONObject data = response.getJSONObject("data");
                disasterRecoveryDto = JSONObject.toJavaObject(data, DisasterRecoveryDto.class);
            } else {
                disasterRecoveryDto = getMysqlReplicateStatus();
            }
            if (disasterRecoveryDto != null) {
                res.setReplicatePhase(disasterRecoveryDto.getReplicatePhase());
                res.setLastUpdateTime(disasterRecoveryDto.getLastUpdateTime());
            }
        } catch (Exception e) {
            log.error("连接失败,连接地址异常或不存在", e);
        }
        return res;
    }

    @Override
    public void switchPlatform(Boolean isMaster) {
        if (isMaster) {
            masterSwitch();
        } else {
            relationSwitch();
        }
    }

    private void relationSwitch(){
        DisasterRecoveryInfo disasterRecoveryInfo = getRelationPlatformAddress(null);
        if (disasterRecoveryInfo == null) {
            throw new BusinessException(ErrorMessage.SWITCH_NO_POWER);
        }
        
        log.info("备平台接收切换请求，开始灾备切换");
        try{
            // 关闭数据同步
            MysqlReplicateCR mr =
                    mysqlReplicateWrapper.getMysqlReplicate(zeusNamespace, NameConstant.ZEUS_MYSQL_REPLICATE);
            mr.getSpec().setEnable(false);
            mysqlReplicateWrapper.updateMysqlReplicate(mr);
            // 切换mysql 模式为一主一从
            JSONObject values = helmChartService.getZeusMysqlInstallValues();
            JSONObject newValues = new JSONObject();
            newValues.putAll(values);
            newValues.put("type", "master-slave");
            newValues.put("lastPlatformSwitchTime", DateUtils.DateToString(new Date(), DateUtils.YYYY_MM_DD_HH_MM_SS));
            newValues.getJSONObject("args").put("disasterRecoverySwitched",true);
            helmChartService.upgradeZeusMysql(values, newValues);
        }catch (Exception e){
            log.error("切换失败",e);
            throw new BusinessException(ErrorMessage.REMOTE_SWITCH_FAILED);
        }
    }

    private void masterSwitch(){
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject newValues = new JSONObject();
        newValues.putAll(values);
        try {
            JSONObject res = platformClient.switchPlatform(CurrentUserRepository.getUser().getToken(), false);
            log.info("切换结果:{}",res);
            if (res != null && res.getBoolean("success")) {
                log.info("切换成功，res = {}", res);
                newValues.put("type", "slave-slave");
                newValues.getJSONObject("args").put("disasterRecoverySwitched",true);
                helmChartService.upgradeZeusMysql(values, newValues);
            } else {
                log.error("切换失败,res={}",res);
                throw new BusinessException(ErrorMessage.REMOTE_SWITCH_FAILED);
            }
        } catch (Exception e) {
            log.error("切换失败,连接地址异常或不存在", e);
            throw new BusinessException(ErrorMessage.CONNECT_REMOTE_HOST_FAILED);
        }
    }

    @Override
    public void saveAddr(DisasterRecoveryInfo info) {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        JSONObject addrInfo = new JSONObject();
        addrInfo.put("protocol", info.getProtocol());
        addrInfo.put("host", info.getHost());
        addrInfo.put("port", info.getPort());
        addrInfo.put("name", info.getName());
        if (info.getIsRelation()) {
            newValues.getJSONObject("args").put("relation", addrInfo);
            helmChartService.upgradeZeusMysql(values, newValues);
            if ("master-slave".equals(values.getString("type")) && values.getJSONObject("args").containsKey("local")) {
                platformClient.setAddress(CurrentUserRepository.getUser().getToken(), info);
                platformClient.setAddress(CurrentUserRepository.getUser().getToken(),
                    convertParam(values.getJSONObject("args").getJSONObject("local")));
            }
        } else {
            newValues.getJSONObject("args").put("local", addrInfo);
            helmChartService.upgradeZeusMysql(values, newValues);
            if ("master-slave".equals(values.getString("type"))
                && values.getJSONObject("args").containsKey("relation")) {
                platformClient.setAddress(CurrentUserRepository.getUser().getToken(), info);
                platformClient.setAddress(CurrentUserRepository.getUser().getToken(),
                    convertParam(values.getJSONObject("args").getJSONObject("local")));
            }
        }
    }

    @Override
    public DisasterRecoveryDto getMysqlReplicateStatus() {

        DisasterRecoveryDto res = new DisasterRecoveryDto();
        // 获取服务状态
        log.info("获取服务状态");
        res.setLocal(new DisasterRecoveryInfo().setPhase(getZusMysqlPhase()));

        log.info("获取同步器状态");
        MysqlReplicateCR mr =
                mysqlReplicateWrapper.getMysqlReplicate(zeusNamespace, NameConstant.ZEUS_MYSQL_REPLICATE);
        if (mr == null || mr.getStatus() == null || mr.getStatus().getPhase() == null){
            return res;
        }
        res.setReplicatePhase(mr.getStatus().getPhase());
        List<MysqlReplicateStatus.PodStatus> slaves = mr.getStatus().getSlaves();
        if (!CollectionUtils.isEmpty(slaves)) {
            final Date[] lastUpdateTime = {DateUtils.parseDate(slaves.get(0).getLastUpdateTime(), DateUtils.YYYY_MM_DD_HH_MM_SS)};
            slaves.forEach(po -> {
                if (po.getLastUpdateTime() != null) {
                    Date date = DateUtils.parseDate(po.getLastUpdateTime(), DateUtils.YYYY_MM_DD_HH_MM_SS);
                    if (date.after(lastUpdateTime[0])) {
                        lastUpdateTime[0] = date;
                    }
                }
            });
            res.setLastUpdateTime(lastUpdateTime[0]);
        }

        return res;
    }

    @Override
    public String getMiddlewareUid() {
        MysqlCluster mc = mysqlClusterWrapper.get(zeusNamespace, NameConstant.ZEUS_MYSQL);
        if (mc != null && mc.getMetadata() != null && !CollectionUtils.isEmpty(mc.getMetadata().getLabels())){
            return mc.getMetadata().getLabels().get("uid");
        }
        return null;
    }

    public DisasterRecoveryInfo getLocalPlatformAddress(JSONObject values) {
        if (values == null){
            values = helmChartService.getZeusMysqlInstallValues();
        }
        // 链接地址信息
        if (values.containsKey("args") && values.getJSONObject("args").containsKey("local")) {
            return convertParam(values.getJSONObject("args").getJSONObject("local"));
        }
        return null;
    }

    @Override
    public DisasterRecoveryInfo getRelationPlatformAddress(JSONObject values) {
        // 获取从平台链接地址
        if (values == null){
            values = helmChartService.getZeusMysqlInstallValues();
        }
        // 链接地址信息
        if (values.containsKey("args") && values.getJSONObject("args").containsKey("relation")) {
            return convertParam(values.getJSONObject("args").getJSONObject("relation"));
        }
        return null;
    }

    private String getZusMysqlPhase() {
        try {
            MysqlCluster mc = mysqlClusterWrapper.get(zeusNamespace, NameConstant.ZEUS_MYSQL);
            log.info("查询{}分区下middlewareCR{}状态", zeusNamespace, "mysqlcluster-" + NameConstant.ZEUS_MYSQL);
            if (mc != null && mc.getStatus() != null && mc.getStatus().getPhase() != null) {
                return mc.getStatus().getPhase();
            }
        } catch (Exception e) {
            log.debug("查询mysql状态失败", e);
        }
        return "Unknown";
    }

    private DisasterRecoveryInfo convertParam(JSONObject info) {
        if (info == null) {
            return null;
        }
        return new DisasterRecoveryInfo().setHost(info.getString("host")).
                setProtocol(info.getString("protocol")).
                setPort(info.getInteger("port")).
                setName(info.getString("name"));
    }

}
