package com.harmonycloud.zeus.operator.impl;

import static com.harmonycloud.caas.common.constants.CmdConstant.*;
import static com.harmonycloud.caas.common.constants.CommonConstant.DOT;
import static com.harmonycloud.caas.common.constants.CommonConstant.NUM_ZERO;
import static com.harmonycloud.caas.common.constants.NameConstant.RESOURCES;
import static com.harmonycloud.caas.common.constants.NameConstant.RUNNING;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.ARGS;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.SYNC_SLAVE;

import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.ActiveAreaAnnotationDto;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.cmd.CmdExecUtil;
import com.harmonycloud.zeus.integration.cluster.ServiceWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.service.k8s.K8sExecService;
import com.harmonycloud.zeus.service.k8s.MiddlewareBackupCRService;
import com.harmonycloud.zeus.service.k8s.PodService;
import io.fabric8.kubernetes.api.model.Service;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.operator.api.PostgresqlOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractPostgresqlOperator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import com.harmonycloud.caas.common.constants.CmdConstant.*;

/**
 * @author xutianhong
 * @Date 2022/6/7 3:19 下午
 */
@Slf4j
@Operator(paramTypes4One = Middleware.class)
public class PostgresqlOperatorImpl extends AbstractPostgresqlOperator implements PostgresqlOperator {

    @Autowired
    public ServiceWrapper serviceWrapper;

    @Autowired
    private MiddlewareBackupCRService middlewareBackupCRService;

    @Autowired
    private K8sExecService k8sExecService;

    @Autowired
    private PodService podService;

    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.POSTGRESQL == MiddlewareTypeEnum.findByType(middleware.getType());
    }

    @Override
    public void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 替换通用values
        replaceCommonValues(middleware, cluster, values);
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        replaceCommonResources(quota, values.getJSONObject(RESOURCES));
        replaceCommonStorages(quota, values);

        // 替换pgSQL专用
        // 替换实例数
        values.put("instances", quota.getNum() + 1);
        if (quota.getNum() == NUM_ZERO){
            values.getJSONObject(ARGS).put("synchronous_commit", "off");
        }
        // 替换密码
        if (StringUtils.isBlank(middleware.getPassword())) {
            middleware.setPassword(PasswordUtils.generateCommonPassword(10));
        }
        JSONObject userPasswords = new JSONObject();
        userPasswords.put("postgres", middleware.getPassword());
        values.put("userPasswords", userPasswords);
        // 替换版本
        values.put("pgsqlVersion", middleware.getVersion());

        // 备份恢复
        if (StringUtils.isNotEmpty(middleware.getBackupFileName())){
            try {
                MiddlewareBackupCR middlewareBackupCR = middlewareBackupCRService.get(cluster.getId(), middleware.getNamespace(), middleware.getBackupFileName());
                Map<String, Object> res = middlewareBackupCR.getStatus().getBackupResults().get(0);
                MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters mp = middlewareBackupCR.getSpec().getBackupDestination().getParameters();

                JSONObject clone = new JSONObject();
                clone.put("cluster", middlewareBackupCR.getSpec().getName());
                clone.put("timestamp", res.get("backupTimestamp"));
                clone.put("s3_wal_path", res.get("repository"));
                clone.put("s3_endpoint", mp.getUrl());
                clone.put("s3_access_key_id", Base64.getDecoder().decode(mp.getUserId()));
                clone.put("s3_secret_access_key", Base64.getDecoder().decode(mp.getUserKey()));
                clone.put("s3_force_path_style", true);

                values.put("clone", clone);
            } catch (Exception e){
                log.info("克隆postgresql实例失败", e);
                throw new BusinessException(ErrorMessage.BACKUP_RESTORE_FAILED);
            }
        }
        // 添加双活配置
        checkAndSetActiveActive(values, middleware);
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertResourcesByHelmChart(middleware, middleware.getType(), values.getJSONObject(RESOURCES));
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        convertRegistry(middleware, cluster);

        middleware.setIsAllLvmStorage(true);
        middleware.setVersion(values.getString("pgsqlVersion"));
        if (checkUserAuthority(MiddlewareTypeEnum.POSTGRESQL.getType())){
            middleware.setPassword(values.getJSONObject("userPasswords").getString("postgres"));
        }
        return middleware;
    }

    @Override
    public void update(Middleware middleware, MiddlewareClusterDTO cluster) {
        StringBuilder sb = new StringBuilder();

        // 实例扩容
        if (middleware.getQuota() != null && middleware.getQuota().get(middleware.getType()) != null) {
            MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
            String cpu = quota.getCpu();
            if (!cpu.contains(DOT)){
                cpu += ".0";
                quota.setCpu(cpu);
            }
            // 设置limit的resources
            setLimitResources(quota);
            if (StringUtils.isNotBlank(quota.getCpu())) {
                sb.append("resources.requests.cpu=").append(quota.getCpu()).append(",resources.limits.cpu=")
                        .append(quota.getLimitCpu()).append(",");
            }
            if (StringUtils.isNotBlank(quota.getMemory())) {
                sb.append("resources.requests.memory=").append(quota.getMemory()).append(",resources.limits.memory=")
                        .append(quota.getLimitMemory()).append(",");
            }
        }
        updateCommonValues(sb, middleware);
        // 没有修改，直接返回
        if (sb.length() == 0) {
            return;
        }
        // 去掉末尾的逗号
        sb.deleteCharAt(sb.length() - 1);
        // 更新helm
        helmChartService.upgrade(middleware, sb.toString(), cluster);
    }

    @Override
    public SwitchInfo getAutoSwitch(Middleware middleware) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        return new SwitchInfo().setIsAuto(getAutoSwitch(middleware, cluster));
    }

    public Boolean getAutoSwitch(Middleware middleware, MiddlewareClusterDTO cluster) {
        // 获取pod列表
        List<PodInfo> podInfos = podService.listPods(cluster.getId(), middleware.getNamespace(), MiddlewareTypeEnum.POSTGRESQL.getType(), middleware.getName());
        List<PodInfo> runningPods = podInfos.stream().filter(podInfo -> RUNNING.equalsIgnoreCase(podInfo.getStatus())).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(runningPods)){
            throw new BusinessException(ErrorMessage.MIDDLEWARE_CLUSTER_IS_NOT_RUNNING);
        }
        // 获取patroniService
        String patroniName = middleware.getName() + "-patroni";
        Service patroniService = serviceWrapper.get(middleware.getClusterId(), middleware.getNamespace(), patroniName);

        if (patroniService == null) {
            log.error("无法找到patroni服务");
            throw new BusinessException(DictEnum.SERVICE,patroniName,ErrorMessage.NOT_FOUND);
        }
        // pod执行命令
        String execCommand = MessageFormat.format(POSTGRESQL_AUTO_SWITCH_STATUS,
                runningPods.get(0).getPodName(), middleware.getNamespace(), cluster.getAddress(), cluster.getAccessToken(), patroniName);
        List<String> resList;
        try {
            resList = CmdExecUtil.runCmd(execCommand);
        } catch (Exception e) {
            log.error("查询自动切换失败", e);
            throw new BusinessException(ErrorMessage.GET_AUTOSWITCH_FAILED);
        }
        // 查看pause
        StringBuilder sb = new StringBuilder();
        resList.forEach(sb::append);
        JSONObject resJSON = JSONObject.parseObject(sb.toString());
        if (resJSON == null) {
            throw new BusinessException(ErrorMessage.GET_AUTOSWITCH_FAILED);
        }
        return resJSON.getBoolean("pause") == null || !resJSON.getBoolean("pause");
    }


    /**
     * 检查是否是双活分区并设置双活配置字段
     */
    @Override
    public void checkAndSetActiveActive(JSONObject values, Middleware middleware) {
        if (namespaceService.isOpenAvailableDomain(middleware.getClusterId(), middleware.getNamespace())) {
            super.setActiveActiveConfig(null, values);
            super.setActiveActiveToleration(middleware, values);
        }
    }

    @Override
    public void switchMiddleware(Middleware middleware) {
        MiddlewareCR cr = middlewareCRService.getCR(middleware.getClusterId(), middleware.getNamespace(),
                MiddlewareTypeEnum.POSTGRESQL.getType(), middleware.getName());
        if (cr==null){
            throw new BusinessException(DictEnum.MIDDLEWARE,middleware.getName(),ErrorMessage.NOT_EXIST);
        }
        // null手动切换， true/false更改自动切换状态
        if (middleware.getAutoSwitch()!=null){
            autoSwitch(middleware,cr);
        }else{
            handSwitch(middleware,cr);
        }

    }

    private void autoSwitch(Middleware middleware, MiddlewareCR cr) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        // 获取patroniService
        String patroniName = middleware.getName() + "-patroni";
        Service patroniService = serviceWrapper.get(middleware.getClusterId(), middleware.getNamespace(), patroniName);
        if (patroniService == null) {
            throw new BusinessException(DictEnum.SERVICE, patroniName, ErrorMessage.NOT_EXIST);
        }
        // 获取执行pod
        JSONArray conditions = JSONObject.parseObject(cr.getMetadata().getAnnotations().get("status")).getJSONArray("conditions");
        if (CollectionUtil.isEmpty(conditions)){
            throw new BusinessException(DictEnum.POD,ErrorMessage.NOT_FOUND);
        }
        JSONObject pod = (JSONObject) conditions.get(0);

        String execCommand = MessageFormat.format(POSTGRESQL_AUTO_SWITCH,
                pod.getString("name"), middleware.getNamespace(), cluster.getAddress(), cluster.getAccessToken(),
                !middleware.getAutoSwitch(), patroniName);
        k8sExecService.exec(execCommand);
    }




    private void handSwitch(Middleware middleware, MiddlewareCR cr) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        // 获取patroniService
        String patroniName = middleware.getName() + "-patroni";
        Service patroniService = serviceWrapper.get(middleware.getClusterId(), middleware.getNamespace(), patroniName);
        if (patroniService == null) {
            throw new BusinessException(DictEnum.SERVICE, patroniName, ErrorMessage.NOT_EXIST);
        }
        // 获取执行pod
        JSONArray conditions = JSONObject.parseObject(cr.getMetadata().getAnnotations().get("status")).getJSONArray("conditions");
        if (CollectionUtil.isEmpty(conditions)){
            throw new BusinessException(DictEnum.ROLE,SYNC_SLAVE,ErrorMessage.NOT_FOUND);
        }
        List<Object> syncSlavePods = conditions.stream().filter(condition -> {
            JSONObject con = (JSONObject) condition;
            return "sync_slave".equals(con.getString("type"));
        }).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(syncSlavePods)){
            throw new BusinessException(DictEnum.ROLE,SYNC_SLAVE,ErrorMessage.NOT_FOUND);
        }
        JSONObject syncSlavePod = (JSONObject) syncSlavePods.get(0);
        String execCommand = MessageFormat.format(POSTGRESQL_HAND_SWITCH,
                syncSlavePod.getString("name"), middleware.getNamespace(), cluster.getAddress(), cluster.getAccessToken(),
                patroniName,syncSlavePod.getString("name"));
        List<String> results = CmdExecUtil.runCmd(execCommand);
        // 判断结果
        parseHandSwitchResult(results);
    }

    @Override
    public List<String> getConfigmapDataList(ConfigMap configMap) {
        return null;
    }

    @Override
    public Map<String, String> configMap2Data(ConfigMap configMap) {
        return null;
    }

    @Override
    public void editConfigMapData(CustomConfig customConfig, List<String> data) {

    }

    @Override
    public void updateConfigData(ConfigMap configMap, List<String> data) {

    }

    @Override
    public ActiveAreaAnnotationDto getActiveAreaAnnotation(String clusterId, String namespace, String type, String middlewareName) {
        return super.getActiveAreaAnnotation(clusterId, namespace, type, middlewareName);
    }

    public void buildClone(Middleware middleware, JSONObject values){
        middlewareBackupCRService.get(middleware.getClusterId(), middleware.getNamespace(), middleware.getBackupFileName());
    }

}

