package com.harmonycloud.zeus.operator.impl;

import static com.middleware.caas.common.constants.CommonConstant.*;
import static com.middleware.caas.common.constants.NameConstant.RESOURCES;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.ARGS;

import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.middleware.caas.common.model.middleware.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.middleware.*;
import com.middleware.tool.cmd.CmdExecUtil;
import com.middleware.tool.encrypt.PasswordUtils;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.integration.cluster.ServiceWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupSpec;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.integration.cluster.bean.Status;
import com.harmonycloud.zeus.operator.api.PostgresqlOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractPostgresqlOperator;
import com.harmonycloud.zeus.service.k8s.K8sExecService;
import com.harmonycloud.zeus.service.k8s.MiddlewareBackupCRService;
import com.harmonycloud.zeus.service.k8s.PodService;

import cn.hutool.core.collection.CollectionUtil;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2022/6/7 3:19 下午
 */
@Slf4j
@Operator(paramTypes4One = Middleware.class)
public class PostgresqlOperatorImpl extends AbstractPostgresqlOperator implements PostgresqlOperator {

    @Autowired
    private MiddlewareBackupCRService middlewareBackupCRService;

    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.POSTGRESQL == MiddlewareTypeEnum.findByType(middleware.getType());
    }

    @Autowired
    public ServiceWrapper serviceWrapper;
    @Autowired
    public K8sExecService k8sExecService;
    @Autowired
    private PodService podService;

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
        values.put("pgsqlVersion", middleware.getVersion() + MIDDLEWARE_VERSION_PLACEHOLDER_STRING);
        // 主机网络配置
        if (middleware.getPostgresqlParam() != null && middleware.getPostgresqlParam().getHostNetwork() != null) {
            values.put("hostNetwork", middleware.getPostgresqlParam().getHostNetwork());
        }

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
        convertCustomVolumesByHelmChart(middleware, values);

        middleware.setIsAllLvmStorage(true);
        middleware.setVersion(values.getString("pgsqlVersion"));
        if (checkUserAuthority(MiddlewareTypeEnum.POSTGRESQL.getType())){
            middleware.setPassword(values.getJSONObject("userPasswords").getString("postgres"));
        }
        middleware.setPassword(values.getJSONObject("userPasswords").getString("postgres"));

        // 是否自动切换
        middleware.setAutoSwitch(getAutoSwitch(middleware, cluster));

        middleware.setPostgresqlParam(new PostgresqlParam().setHostNetwork(values.getBoolean("hostNetwork")));
        return middleware;
    }

    public Boolean getAutoSwitch(Middleware middleware, MiddlewareClusterDTO cluster) {
        // 获取patroniService
        String patroniName = middleware.getName() + "-patroni";
        Service patroniService = serviceWrapper.get(middleware.getClusterId(), middleware.getNamespace(), patroniName);

        if (patroniService == null) {
            log.error("无法找到patroni服务");
            return null;
        }
        // 获取pod列表
        Status status = middlewareCRService.getStatus(middleware.getClusterId()
                , middleware.getNamespace(), MiddlewareTypeEnum.POSTGRESQL.getType(), middleware.getName());
        List<Status.Condition> conditions = status.getConditions();
        if (CollectionUtil.isEmpty(conditions)) {
            return null;
        }
        // pod执行命令
        String execCommand = MessageFormat.format(
                "kubectl exec {0} -n {1} -c postgres --server={2} --token={3} --insecure-skip-tls-verify=true " +
                        "-- bash  -c \"curl -s http://{4}:8008/patroni | jq .\"",
                conditions.get(0).getName(), middleware.getNamespace(), cluster.getAddress(), cluster.getAccessToken(), patroniName);
        List<String> resList;
        try {
            resList = CmdExecUtil.runCmd(execCommand);
        } catch (Exception e) {
            log.error("查询自动切换失败", e);
            return null;
        }
        // 查看pause
        StringBuilder sb = new StringBuilder();
        resList.forEach(sb::append);
        JSONObject resJSON = JSONObject.parseObject(sb.toString());
        return resJSON.getBoolean("pause") == null || !resJSON.getBoolean("pause");
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

    /**
     * 检查是否是双活分区并设置双活配置字段
     */
    @Override
    public void checkAndSetActiveActive(JSONObject values, Middleware middleware) {
        if (namespaceService.checkAvailableDomain(middleware.getClusterId(), middleware.getNamespace())) {
            super.setActiveActiveConfig(null, values);
            super.setActiveActiveToleration(middleware, values);
        }
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

    public void buildClone(Middleware middleware, JSONObject values){
        middlewareBackupCRService.get(middleware.getClusterId(), middleware.getNamespace(), middleware.getBackupFileName());
    }

    @Override
    public Integer getReplicas(JSONObject values) {
        return values.getInteger("instances");
    }

    @Override
    public void switchMiddleware(Middleware middleware) {
        MiddlewareCR cr = middlewareCRService.getCR(middleware.getClusterId(), middleware.getNamespace(),
                MiddlewareTypeEnum.POSTGRESQL.getType(), middleware.getName());
        if (cr==null){
            throw new BusinessException(DictEnum.MIDDLEWARE,middleware.getName(),ErrorMessage.NOT_EXIST);
        }
        if (!"Running".equals(cr.getStatus().getPhase())){
            throw new BusinessException(ErrorMessage.MIDDLEWARE_CLUSTER_IS_NOT_RUNNING);
        }
        // null手动切换， true/false更改自动切换状态
        if (middleware.getAutoSwitch()!=null){
            autoSwitch(middleware,cr);
        }else{
            handSwitch(middleware,cr);
        }

    }

    @Override
    public List<IngressDTO> listHostNetworkAddress(String clusterId, String namespace, String middlewareName, String type) {
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        if (values == null) {
            return Collections.emptyList();
        }
        if (values.containsKey("hostNetwork") && values.getBoolean("hostNetwork")) {
            List<PodInfo> podInfoList = podService.listMiddlewarePods(clusterId, namespace, middlewareName, MiddlewareTypeEnum.POSTGRESQL.getType());
            podInfoList = podInfoList.stream().filter(podInfo -> "master".equals(podInfo.getRole())).collect(Collectors.toList());
            return podInfoList.stream().map(podInfo -> {
                IngressDTO ingressDTO = new IngressDTO();
                ingressDTO.setServicePurpose(podInfo.getPodName());
                ingressDTO.setExposeIP(podInfo.getHostIp());
                ingressDTO.setExposePort("5432");
                return ingressDTO;
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
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
            throw new BusinessException(DictEnum.POD,ErrorMessage.NOT_FOUND);
        }
        List<Object> syncSlavePods = conditions.stream().filter(condition -> {
            JSONObject con = (JSONObject) condition;
            return "sync_slave".equals(con.getString("type"));
        }).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(syncSlavePods)){
            throw new BusinessException(DictEnum.POD,ErrorMessage.NOT_FOUND);
        }
        JSONObject syncSlavePod = (JSONObject) syncSlavePods.get(0);
        String execCommand = MessageFormat.format(
                "kubectl exec {0} -n {1} -c postgres --server={2} --token={3} --insecure-skip-tls-verify=true " +
                        "-- bash -c \"curl -s -X POST http://{4}:8008/failover -d '''{\\\"candidate\\\": \\\"'{5}'\\\"}'''\"",
                syncSlavePod.getString("name"), middleware.getNamespace(), cluster.getAddress(), cluster.getAccessToken(),
                patroniName,syncSlavePod.getString("name"));
            k8sExecService.exec(execCommand);
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

        String execCommand = MessageFormat.format(
                "kubectl exec {0} -n {1} -c postgres --server={2} --token={3} --insecure-skip-tls-verify=true " +
                        "-- bash -c \"curl -s -X PATCH -d '''{\\\"pause\\\": '{4}' }''' http://{5}:8008/config | jq .\"",
                pod.getString("name"), middleware.getNamespace(), cluster.getAddress(), cluster.getAccessToken(),
                !middleware.getAutoSwitch(), patroniName);
            k8sExecService.exec(execCommand);
    }

    @Override
    public String replaceSingleQuotes(String valueYaml){
        return valueYaml.replace(MIDDLEWARE_VERSION_PLACEHOLDER_STRING, "");
    }
}

