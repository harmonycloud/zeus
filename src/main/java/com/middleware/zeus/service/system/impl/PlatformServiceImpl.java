package com.middleware.zeus.service.system.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.constants.NameConstant;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.DisasterRecoveryDto;
import com.middleware.zeus.common.model.DisasterRecoveryInfo;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.integration.cluster.MysqlClusterWrapper;
import com.middleware.zeus.integration.cluster.MysqlReplicateWrapper;
import com.middleware.zeus.integration.cluster.bean.MysqlCluster;
import com.middleware.zeus.integration.cluster.bean.MysqlReplicateCR;
import com.middleware.zeus.integration.cluster.bean.MysqlReplicateStatus;
import com.middleware.zeus.integration.platform.PlatformClient;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.PlatformService;
import com.middleware.zeus.util.K8sClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @auther wangpenglei
 * @date 2023/3/22 10:35
 */
@Service
@Slf4j
public class PlatformServiceImpl implements PlatformService {

    @Value("${zeus.namespace:zeus}")
    private String zeusNamespace;

    /**
     *  设置平台可用
     */
    public static final Map<String, Boolean> AVAILABLE = new HashMap<>();

    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private MysqlReplicateWrapper mysqlReplicateWrapper;
    @Autowired
    private PlatformClient platformClient;
    @Autowired
    private MysqlClusterWrapper mysqlClusterWrapper;

    @PostConstruct
    public void init(){
//        try {
//            JSONObject values = helmChartService.getZeusMysqlInstallValues();
//            boolean isSlave = "master-slave".equals(values.getString("type"));
//            if (!isSlave && values.getJSONObject("args") != null && values.getJSONObject("args").containsKey("disasterRecoverySwitched")
//                    && values.getJSONObject("args").getBoolean("disasterRecoverySwitched")){
//                AVAILABLE.put("available", false);
//            }
//        } catch (Exception e){
//            log.error("初始化平台灾备可访问失败");
//        }
    }


    @Override
    public DisasterRecoveryDto queryAccessInfo() {
        DisasterRecoveryDto res = new DisasterRecoveryDto();
        JSONObject values = helmChartService.getZeusMysqlInstallValues();

        // 是否主平台
        res.setIsMaster("master-slave".equals(values.getString("type")));

        res.setLocal(getLocalPlatformAddress(values));
        res.setRelation(getRelationPlatformAddress(values));

        // 设置数据库状态
        if (res.getLocal() != null){
            res.getLocal().setPhase(getZusMysqlPhase());
        }
        // 设置远程数据库状态
        if (res.getRelation() != null) {
            try {
                JSONObject relationMysqlPhase =
                        platformClient.getRelationMysqlPhase(CurrentUserRepository.getUser().getToken());
                if (relationMysqlPhase.getBoolean("success") && relationMysqlPhase.containsKey("data")) {
                    res.getRelation().setPhase(relationMysqlPhase.getString("data"));
                }
            } catch (Exception e){
                log.error("获取远程数据库状态失败", e);
            }
        }

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
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        // 根据切换信息判断是否直接返回， 避免主备平台循环调用
        // 如果已经切换了  则不再执行
        if("master-slave".equals(values.getString("type"))){
            return;
        }
        log.info("备平台接收切换请求，开始灾备切换");
        try{
            // 关闭数据同步
            MysqlReplicateCR mr =
                    mysqlReplicateWrapper.getMysqlReplicate(K8sClient.DEFAULT_CLIENT, zeusNamespace, NameConstant.ZEUS_MYSQL_REPLICATE);
            mr.getSpec().setEnable(false);
            mysqlReplicateWrapper.replace(K8sClient.DEFAULT_CLIENT, mr);
            // 切换mysql 模式为一主一从
            JSONObject newValues = new JSONObject();
            newValues.putAll(values);
            newValues.put("type", "master-slave");
            newValues.put("lastPlatformSwitchTime", DateUtils.DateToString(new Date(), DateUtils.YYYY_MM_DD_HH_MM_SS));
            helmChartService.upgradeZeusMysql(values, newValues);
            // 尝试关闭主平台
            try {
                platformClient.switchPlatform(CurrentUserRepository.getUser().getToken(), true);
            } catch (Exception e){
                log.error("灾备平台切换，更新主平台信息失败", e);
            }
            // 移除远程平台信息
            newValues.getJSONObject("args").remove("relation");
            helmChartService.upgradeZeusMysql(values, newValues);
        }catch (Exception e){
            log.error("切换失败",e);
            throw new BusinessException(ErrorMessage.REMOTE_SWITCH_FAILED);
        }
    }

    private void masterSwitch(){
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        // 根据切换信息判断是否直接返回， 避免主备平台循环调用
        if (AVAILABLE.containsKey("available") && !AVAILABLE.get("available")){
            return;
        }
        try {
            AVAILABLE.put("available", false);
            // 记录开始切换的操作
            JSONObject newValues = JSONObject.parseObject(values.toJSONString());
            newValues.getJSONObject("args").put("disasterRecoverySwitched",true);
            helmChartService.upgradeZeusMysql(values, newValues);
            // 切换备平台
            JSONObject res = platformClient.switchPlatform(CurrentUserRepository.getUser().getToken(), false);
            log.info("切换结果:{}",res);
            if (res != null && res.getBoolean("success")) {
                log.info("切换成功，res = {}", res);
                newValues.put("type", "slave-slave");
                newValues.getJSONObject("args").put("disasterRecoverySwitched",true);
                helmChartService.upgradeZeusMysql(values, newValues);
            } else {
                log.error("切换失败,res={}",res);
                AVAILABLE.put("available", true);
                newValues.getJSONObject("args").put("disasterRecoverySwitched",false);
                helmChartService.upgradeZeusMysql(values, newValues);
                throw new BusinessException(ErrorMessage.REMOTE_SWITCH_FAILED);
            }
        } catch (Exception e) {
            log.error("切换失败,连接地址异常或不存在", e);
            AVAILABLE.put("available", true);
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
                try {
                    platformClient.setAddress(CurrentUserRepository.getUser().getToken(), info.setIsRelation(false));
                    platformClient.setAddress(CurrentUserRepository.getUser().getToken(),
                        convertParam(values.getJSONObject("args").getJSONObject("local")).setIsRelation(true));
                } catch (Exception e) {
                    log.error("更新备平台信息失败", e);
                }
            }
        } else {
            newValues.getJSONObject("args").put("local", addrInfo);
            helmChartService.upgradeZeusMysql(values, newValues);
            if ("master-slave".equals(values.getString("type"))
                && values.getJSONObject("args").containsKey("relation")) {
                try {
                    platformClient.setAddress(CurrentUserRepository.getUser().getToken(), info.setIsRelation(true));
                    platformClient.setAddress(CurrentUserRepository.getUser().getToken(),
                        convertParam(values.getJSONObject("args").getJSONObject("local")).setIsRelation(false));
                } catch (Exception e) {
                    log.error("更新备平台信息失败", e);
                }
            }
        }
    }

    @Override
    public DisasterRecoveryDto getMysqlReplicateStatus() {

        DisasterRecoveryDto res = new DisasterRecoveryDto();

        log.info("获取同步器状态");
        MysqlReplicateCR mr =
                mysqlReplicateWrapper.getMysqlReplicate(K8sClient.DEFAULT_CLIENT, zeusNamespace, NameConstant.ZEUS_MYSQL_REPLICATE);
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

    @Override
    public String getRelationMysqlPhase() {
        return getZusMysqlPhase();
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
