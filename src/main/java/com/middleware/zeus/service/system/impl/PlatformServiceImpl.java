package com.middleware.zeus.service.system.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.constants.NameConstant;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.DisasterRecoveryDto;
import com.middleware.caas.common.model.DisasterRecoveryInfo;
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

    @Autowired
    private MiddlewareWrapper middlewareWrapper;

    @Autowired
    private SystemConfigService systemConfigService;


    @Override
    public DisasterRecoveryDto queryAccessInfo(HttpServletRequest request) {
        DisasterRecoveryDto res = new DisasterRecoveryDto();
        JSONObject values = helmChartService.getZeusMysqlInstallValues();

        // 是否主平台
        res.setIsMaster("master-slave".equals(values.getString("type")));

        // 链接地址信息
        JSONObject args = values.getJSONObject("args");
        JSONObject _local = args.getJSONObject("local");
        JSONObject _relation = args.getJSONObject("relation");
        res.setLocal(convertParam(_local)).setRelation(convertParam(_relation));

        // 上次切换时间
        res.setLastSwitchTime(values.getDate("lastSwitchTime"));

        // 当前服务运行状态
        DisasterRecoveryInfo local = res.getLocal();
        if (local == null) {
            local = new DisasterRecoveryInfo();
        }
        local.setPhase(getZusMysqlPhase());
        res.setLocal(local);
        try {
            log.info("获取同步器状态");
            JSONObject response = platformClient.getMysqlReplicateStatus(request.getHeader("userToken"));
            JSONObject data = response.getJSONObject("data");
            if (data != null && response.getBoolean("success")) {
                res.setReplicatePhase(data.getString("replicatePhase"));
                res.setLastUpdateTime(data.getDate("lastUpdateTime"));
                res.getRelation().setPhase(data.getJSONObject("local").getString("phase"));
            } else {
                log.error("获取同步器状态失败,res={}",response);
            }
        }catch (Exception e){
            log.error("连接失败,连接地址异常或不存在",e);
        }
        return res;
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

    @Override
    public void switchPlatform(HttpServletRequest request) throws IOException {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        Boolean isMaster = "master-slave".equals(values.getString("type"));
        if (isMaster) {
            masterSwitch(request,values);
        } else {
            relationSwitch(values);
        }
    }

    private void relationSwitch(JSONObject values){
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        BeanSystemConfig conf = systemConfigService.getConfig("backupPlatformUid");
        if (conf == null) {
            throw new BusinessException(ErrorMessage.SWITCH_NO_POWER);
        }

        log.info("备平台接收切换请求，开始灾备切换");
        try{
            // 先检查是否有权限切换
            String uid = getMiddlewareUid();
            if (!conf.getConfigValue().equals(uid)) {
                throw new BusinessException(ErrorMessage.SWITCH_NO_POWER);
            }
            MysqlReplicateCR mr =
                    mysqlReplicateWrapper.getMysqlReplicate(zeusNamespace, NameConstant.ZEUS_MYSQL_REPLICATE);
            mr.getSpec().setEnable(false);
            mysqlReplicateWrapper.updateMysqlReplicate(mr);
            JSONObject args = newValues.getJSONObject("args");
            newValues.put("type", "master-slave");
            newValues.put("lastSwitchTime",new Date());
            newValues.getJSONObject("args").put("disasterRecoverySwitched",true);
            helmChartService.upgradeZeusMysql(values, newValues);
        }catch (Exception e){
            log.error("切换失败",e);
            throw new BusinessException(ErrorMessage.REMOTE_SWITCH_FAILED);
        }
    }

    private void masterSwitch(HttpServletRequest request, JSONObject values){
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        try {
            JSONObject res = platformClient.switchPlatform(request.getHeader("userToken"));
            log.info("切换结果:{}",res);
            if (res != null && res.getBoolean("success")) {
                log.info("切换成功，res = {}", res);
                newValues.put("type", "slave-slave");
                newValues.put("lastSwitchTime",new Date());
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
    public void saveAddr(DisasterRecoveryInfo info, String name, HttpServletRequest request) {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        JSONObject addrInfo = new JSONObject();
        addrInfo.put("protocol", info.getProtocol());
        addrInfo.put("host", info.getHost());
        addrInfo.put("port", info.getPort());
        addrInfo.put("name", name);
        // 备平台连接主平台后绑定主平台
        if (info.getIsRelation()) {
            saveRelationUid(request);
        } else {
            newValues.getJSONObject("args").put("local",addrInfo);
        }
        helmChartService.upgradeZeusMysql(values, newValues);
    }

    private void saveRelationUid(HttpServletRequest request){
        try{
            JSONObject res = platformClient.getUid(request.getHeader("userToken"));
            if (res != null && res.getBoolean("success")) {
                log.info("获取备平台uid成功,res={}",res);
                String uid = res.getString("data");
                if (StringUtils.isNotBlank(uid)) {
                    BeanSystemConfig conf = systemConfigService.getConfigForUpdate("backupPlatformUid");
                    if (conf == null) {
                        systemConfigService.addConfig("backupPlatformUid", uid);
                    } else {
                        systemConfigService.updateConfig("backupPlatformUid",uid);
                    }
                }
            }
        }catch (Exception e) {
            throw new BusinessException(ErrorMessage.CONNECT_REMOTE_HOST_FAILED);
        }
    }

    @Override
    public DisasterRecoveryDto getMysqlReplicateStatus() {
        log.info("获取同步器状态");
        MysqlReplicateCR mr =
                mysqlReplicateWrapper.getMysqlReplicate(zeusNamespace, NameConstant.ZEUS_MYSQL_REPLICATE);
        DisasterRecoveryDto res = new DisasterRecoveryDto();
        // 获取服务状态
        log.info("获取服务状态");
        res.setLocal(new DisasterRecoveryInfo().setPhase(getZusMysqlPhase()));
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
        if (mc == null || mc.getMetadata() == null || CollectionUtils.isEmpty(mc.getMetadata().getLabels())){
            return mc.getMetadata().getLabels().get("uid");
        }
        return null;
    }

    private String getZusMysqlPhase(){
        MiddlewareCR cr = middlewareWrapper.get(zeusNamespace, "mysqlcluster-"+NameConstant.ZEUS_MYSQL);
        if (cr == null || cr.getStatus() == null || cr.getStatus().getPhase() == null) {
            return "Unknown";
        }
        return cr.getStatus().getPhase();
    }


}
