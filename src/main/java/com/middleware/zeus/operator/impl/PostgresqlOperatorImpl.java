package com.middleware.zeus.operator.impl;

import static com.middleware.caas.common.constants.CmdConstant.*;
import static com.middleware.caas.common.constants.CommonConstant.NUM_ZERO;
import static com.middleware.caas.common.constants.NameConstant.RESOURCES;
import static com.middleware.caas.common.constants.NameConstant.RUNNING;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.ARGS;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.SYNC_SLAVE;
import static com.middleware.caas.common.enums.DictEnum.ROLE;

import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollectionUtil;
import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ActiveAreaAnnotationDto;
import com.middleware.tool.cmd.CmdExecUtil;
import com.middleware.caas.common.model.middleware.*;
import com.middleware.zeus.integration.cluster.Postgresql;
import com.middleware.zeus.integration.cluster.PostgresqlWrapper;
import com.middleware.zeus.integration.cluster.ServiceWrapper;
import com.middleware.zeus.service.k8s.K8sExecService;
import com.middleware.zeus.service.k8s.MiddlewareBackupCRService;
import com.middleware.zeus.service.k8s.PodService;
import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupSpec;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.operator.api.PostgresqlOperator;
import com.middleware.zeus.operator.miiddleware.AbstractPostgresqlOperator;
import com.middleware.zeus.util.ChartVersionUtil;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.tool.encrypt.PasswordUtils;

import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

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

    @Autowired
    private PostgresqlWrapper postgresqlWrapper;

    @Value("${system.gracefulRestartParam:middleware.maintenance.lock:graceful-restart,middleware.maintenance.step:0}")
    private String gracefulRestartParam;

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
        if (ChartVersionUtil.compare(middleware.getChartVersion(), "2.0.12") > 0) {
            values.put("pgsqlVersion", middleware.getVersion().split("\\.")[0]);
        } else {
            values.put("pgsqlVersion", middleware.getVersion());
        }
        // 主机网络配置
        if (middleware.getPostgresqlParam() != null && middleware.getPostgresqlParam().getHostNetwork() != null) {
            values.put("hostNetwork", middleware.getPostgresqlParam().getHostNetwork());
            values.put("podAntiAffinity", "hard");
        }

        // 端口配置
        JSONObject customEnvs = values.getJSONObject("customEnvs");
        if (middleware.getPostgresqlParam() != null && customEnvs != null) {
            PostgresqlParam pgParam = middleware.getPostgresqlParam();
            if (pgParam.getPgPort() != null) {
                customEnvs.put("PGPORT", pgParam.getPgPort().toString());
            }
            if (pgParam.getApiPort() != null) {
                customEnvs.put("APIPORT", pgParam.getApiPort().toString());
            }
            if (pgParam.getExporterPort() != null) {
                customEnvs.put("EXPORTERPORT", pgParam.getExporterPort().toString());
            }
            if (pgParam.getBgMonPort() != null) {
                customEnvs.put("BGMONPORT", pgParam.getBgMonPort().toString());
            }

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
        convertRegistry(middleware, values);
        convertCustomVolumesByHelmChart(middleware, values);
        convertPostgresqlParamByHelmChart(middleware, values);

        middleware.setIsAllLvmStorage(true);
        middleware.setVersion(values.getString("pgsqlVersion"));
        if (checkUserAuthority(MiddlewareTypeEnum.POSTGRESQL.getType())) {
            middleware.setPassword(values.getJSONObject("userPasswords").getString("postgres"));
        }
        middleware.setPassword(values.getJSONObject("userPasswords").getString("postgres"));

        if (middleware.getQuota() != null && middleware.getQuota().containsKey(middleware.getType())){
            middleware.getQuota().get(middleware.getType()).setNum(values.getInteger("instances") - 1);
        }

        List<MiddlewareQuota> storageClasses = getMiddlewareStorageClasses(cluster.getId(), middleware, values);
        middleware.setStorageResource(storageClasses);
        return middleware;
    }

    private void convertPostgresqlParamByHelmChart(Middleware middleware, JSONObject values) {
        // 主机网络配置
        PostgresqlParam pgParam = middleware.getPostgresqlParam();
        if (pgParam == null){
            pgParam = new PostgresqlParam();
        }
        pgParam.setHostNetwork(values.getBoolean("hostNetwork"));

        // 端口
        JSONObject customEnvs = values.getJSONObject("customEnvs");
        String pgPort = customEnvs == null ? "5432" : customEnvs.getString("PGPORT");
        String apiPort = customEnvs == null ? "8008" : customEnvs.getString("APIPORT");
        String exporterPort = customEnvs == null ? "9187" : customEnvs.getString("EXPORTERPORT");
        String bgMonPort = customEnvs == null ? "8080" : customEnvs.getString("BGMONPORT");

        pgParam.setApiPort(apiPort == null ? 8008 : Integer.parseInt(apiPort))
            .setBgMonPort(bgMonPort == null ? 8080 : Integer.parseInt(bgMonPort))
            .setPgPort(pgPort == null ? 5432 : Integer.parseInt(pgPort))
            .setExporterPort(exporterPort == null ? 9187 : Integer.parseInt(exporterPort));
        middleware.setPostgresqlParam(pgParam);
    }

    @Override
    public SwitchInfo getAutoSwitch(Middleware middleware) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        return new SwitchInfo().setIsAuto(getAutoSwitch(middleware, cluster));
    }

    public Boolean getAutoSwitch(Middleware middleware, MiddlewareClusterDTO cluster) {
        // 获取pod列表
        List<PodInfo> podInfos = podService.listMiddlewarePods(cluster.getId(), middleware.getNamespace(),
            middleware.getName(), MiddlewareTypeEnum.POSTGRESQL.getType());
        List<PodInfo> runningPods = podInfos.stream().filter(podInfo -> RUNNING.equalsIgnoreCase(podInfo.getStatus()))
            .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(runningPods)) {
            throw new BusinessException(ErrorMessage.GET_AUTOSWITCH_FAILED);
        }
        // 获取patroniService
        String patroniName = middleware.getName() + "-patroni";
        Service patroniService = serviceWrapper.get(middleware.getClusterId(), middleware.getNamespace(), patroniName);

        if (patroniService == null) {
            log.error("无法找到patroni服务");
            throw new BusinessException(DictEnum.SERVICE, patroniName, ErrorMessage.NOT_FOUND);
        }
        // 获取获取patroniService端口
        String patroniPort = null;
        if (patroniService.getSpec() == null || CollectionUtils.isEmpty(patroniService.getSpec().getPorts())) {
            throw new BusinessException(DictEnum.SERVICE, patroniName, ErrorMessage.INVALID_PARAMETER);
        }
        for (ServicePort port : patroniService.getSpec().getPorts()) {
            if ("patroni".equals(port.getName())) {
                patroniPort = Integer.toString(port.getPort());
            }
        }
        // pod执行命令
        String execCommand = MessageFormat.format(POSTGRESQL_AUTO_SWITCH_STATUS, runningPods.get(0).getPodName(),
            middleware.getNamespace(), cluster.getAddress(), cluster.getAccessToken(), patroniName, patroniPort);
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
    public SwitchInfo switchMiddleware(Middleware middleware) {
        MiddlewareCR cr = middlewareCRService.getCR(middleware.getClusterId(), middleware.getNamespace(),
                MiddlewareTypeEnum.POSTGRESQL.getType(), middleware.getName());
        if (cr==null){
            throw new BusinessException(DictEnum.MIDDLEWARE,middleware.getName(),ErrorMessage.NOT_EXIST);
        }
        // null手动切换， true/false更改自动切换状态
        return middleware.getAutoSwitch() == null ? handSwitch(middleware,cr): autoSwitch(middleware,cr);
    }

    private SwitchInfo autoSwitch(Middleware middleware, MiddlewareCR cr) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        // 获取patroniService
        String patroniName = middleware.getName() + "-patroni";
        Service patroniService = serviceWrapper.get(middleware.getClusterId(), middleware.getNamespace(), patroniName);
        if (patroniService == null) {
            throw new BusinessException(DictEnum.SERVICE, patroniName, ErrorMessage.NOT_EXIST);
        }
        // 获取pod列表
        List<PodInfo> podInfos = podService.listMiddlewarePods(cluster.getId(), middleware.getNamespace(),
            middleware.getName(), MiddlewareTypeEnum.POSTGRESQL.getType());
        List<PodInfo> runningPods = podInfos.stream().filter(podInfo -> RUNNING.equalsIgnoreCase(podInfo.getStatus()))
            .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(runningPods)) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_CLUSTER_IS_NOT_RUNNING);
        }
        // 获取获取patroniService端口
        String patroniPort = null;
        if (patroniService.getSpec() == null || CollectionUtils.isEmpty(patroniService.getSpec().getPorts())) {
            throw new BusinessException(DictEnum.SERVICE, patroniName, ErrorMessage.INVALID_PARAMETER);
        }
        for (ServicePort port : patroniService.getSpec().getPorts()) {
            if ("patroni".equals(port.getName())) {
                patroniPort = Integer.toString(port.getPort());
            }
        }
        String execCommand =
            MessageFormat.format(POSTGRESQL_AUTO_SWITCH, runningPods.get(0).getPodName(), middleware.getNamespace(),
                cluster.getAddress(), cluster.getAccessToken(), !middleware.getAutoSwitch(), patroniName, patroniPort);
        k8sExecService.exec(execCommand);
        return null;
    }




    private SwitchInfo handSwitch(Middleware middleware, MiddlewareCR cr) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        // 获取patroniService
        String patroniName = middleware.getName() + "-patroni";
        Service patroniService = serviceWrapper.get(middleware.getClusterId(), middleware.getNamespace(), patroniName);
        if (patroniService == null) {
            throw new BusinessException(DictEnum.SERVICE, patroniName, ErrorMessage.NOT_EXIST);
        }
        // 获取获取patroniService端口
        String patroniPort = null;
        if (patroniService.getSpec() == null || CollectionUtils.isEmpty(patroniService.getSpec().getPorts())) {
            throw new BusinessException(DictEnum.SERVICE, patroniName, ErrorMessage.INVALID_PARAMETER);
        }
        for (ServicePort port : patroniService.getSpec().getPorts()) {
            if ("patroni".equals(port.getName())) {
                patroniPort = Integer.toString(port.getPort());
            }
        }
        // 获取执行pod
        List<PodInfo> podInfos = podService.listMiddlewarePods(cluster.getId(), middleware.getNamespace(),
            middleware.getName(), MiddlewareTypeEnum.POSTGRESQL.getType());
        List<PodInfo> runningPods = podInfos.stream().filter(
            podInfo -> RUNNING.equalsIgnoreCase(podInfo.getStatus()) && SYNC_SLAVE.equalsIgnoreCase(podInfo.getRole()))
            .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(runningPods)) {
            throw new BusinessException(ROLE, SYNC_SLAVE, ErrorMessage.NOT_EXIST_OR_NOT_RUNNING);
        }
        String newMasterName = runningPods.get(0).getPodName();
        String execCommand = MessageFormat.format(POSTGRESQL_HAND_SWITCH, newMasterName, middleware.getNamespace(),
            cluster.getAddress(), cluster.getAccessToken(), patroniName, patroniPort, newMasterName);
        List<String> results = CmdExecUtil.runCmd(execCommand);
        // 判断结果
        parseHandSwitchResult(results);
        return new SwitchInfo().setNewMasterName(newMasterName);
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

    @Override
    public List<IngressDTO> listHostNetworkAddress(String clusterId, String namespace, String middlewareName, String type) {
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        if (values == null) {
            return Collections.emptyList();
        }
        if (values.containsKey("hostNetwork") && values.getBoolean("hostNetwork")) {
            List<PodInfo> podInfoList = podService.listMiddlewarePods(clusterId, namespace, middlewareName, MiddlewareTypeEnum.POSTGRESQL.getType());
            return podInfoList.stream().map(podInfo -> {
                IngressDTO ingressDTO = new IngressDTO();
                ingressDTO.setServicePurpose(podInfo.getPodName());
                ingressDTO.setExposeIP(podInfo.getHostIp());
                ingressDTO.setExposePort("5432");
                if (values.containsKey("customEnvs") && values.getJSONObject("customEnvs").containsKey("PGPORT")) {
                    ingressDTO.setExposePort(values.getJSONObject("customEnvs").getString("PGPORT"));
                }
                return ingressDTO;
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void update(Middleware middleware, MiddlewareClusterDTO cluster) {
        StringBuilder sb = new StringBuilder();

        if (middleware.getQuota() != null && middleware.getQuota().get(middleware.getType()) != null) {
            MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());

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
            // 设置实例数量
            if (quota.getNum() != null) {
                checkInstanceNum(quota.getNum());
                int instance = quota.getNum() + 1;
                String mod = String.format("1m-%ds", quota.getNum());
                sb.append("instances=").append(instance).append(",");
                sb.append("mode=").append(mod);
            }
        }
        updateCommonValues(sb, middleware);

        if (sb.length() == 0) {
            return;
        }
        // 去掉末尾的逗号
        if (sb.toString().endsWith(",")) {
            sb.deleteCharAt(sb.length() - 1);
        }
        // 更新helm
        helmChartService.upgrade(middleware, sb.toString(), cluster);

    }

    @Override
    public void reboot(String clusterId, String namespace, String name, String type) {
        Postgresql postgresql = postgresqlWrapper.get(clusterId, namespace, name);
        Integer numberOfInstances = postgresql.getSpec().getNumberOfInstances();
        if (numberOfInstances == 1) {
            super.reboot(clusterId, namespace, name, type);
            return;
        }
        Map<String, String> annotations = postgresql.getMetadata().getAnnotations();
        String[] params = gracefulRestartParam.split(",");
        for (String param : params) {
            String[] kv = param.split(":");
            annotations.put(kv[0], kv[1]);
        }
        postgresqlWrapper.update(clusterId, namespace, postgresql);
    }

    public void buildClone(Middleware middleware, JSONObject values){
        middlewareBackupCRService.get(middleware.getClusterId(), middleware.getNamespace(), middleware.getBackupFileName());
    }

    /**
     * 检查从节点数量是否合法,pg的从节点数量范围为：1-3
     * @param instanceNum
     */
    private void checkInstanceNum(int instanceNum){
        if(instanceNum < 0 || instanceNum > 3){
            throw new BusinessException(ErrorMessage.ERROR_PG_POD_NUm);
        }
    }

}

