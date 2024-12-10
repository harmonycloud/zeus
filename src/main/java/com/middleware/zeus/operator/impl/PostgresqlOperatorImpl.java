package com.middleware.zeus.operator.impl;

import static com.middleware.zeus.common.constants.CmdConstant.*;
import static com.middleware.zeus.common.constants.CommonConstant.*;
import static com.middleware.zeus.common.constants.NameConstant.*;
import static com.middleware.zeus.common.constants.NameConstant.CPU;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.ACTIVE_ACTIVE;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.ActiveAreaAnnotationDto;
import com.middleware.zeus.util.K8sConvert;
import com.middleware.zeus.util.cmd.CmdExecUtil;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.bean.BeanSystemConfig;
import com.middleware.zeus.integration.cluster.bean.Postgresql;
import com.middleware.zeus.integration.cluster.PostgresqlWrapper;
import com.middleware.zeus.integration.cluster.ServiceWrapper;
import com.middleware.zeus.integration.dashboard.PostgresqlClientWrapper;
import com.middleware.zeus.service.k8s.K8sExecService;
import com.middleware.zeus.service.k8s.MiddlewareBackupCRService;
import com.middleware.zeus.service.k8s.PodService;
import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackup;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupSpec;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.operator.api.PostgresqlOperator;
import com.middleware.zeus.operator.miiddleware.AbstractPostgresqlOperator;
import com.middleware.zeus.util.middleware.ChartVersionUtil;
import com.middleware.zeus.util.numeric.ResourceCalculationUtil;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.util.encrypt.PasswordUtils;

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
    @Autowired
    private PostgresqlClientWrapper postgresqlClientWrapper;

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
            if (middleware.getPostgresqlParam().getHostNetwork()){
                values.put("podAntiAffinity", "hard");
            }
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

        // 审计日志配置
        if (middleware.getAudit() != null && middleware.getAudit()){
            JSONObject args = values.getJSONObject(ARGS);
            if (args != null){
                args.put("pgaudit.log", "WRITE,DDL");
            }
        }

        //连接池配置
        if(middleware.getPostgresqlParam()!=null){
            PostgresqlParam pgParam = middleware.getPostgresqlParam();
            values.put("enableConnectionPooler", pgParam.getEnableConnectionPooler());
            JSONObject connectionPooler = values.getJSONObject("connectionPooler");
            if(pgParam.getEnableConnectionPooler()&&connectionPooler!=null){
                connectionPooler.put("podAntiAffinity","hard");
                connectionPooler.put("podAntiAffinityTopologKey","topology.kubernetes.io/zone");
                connectionPooler.put("numberOfInstances",pgParam.getPoolerInstanceNum());
                JSONObject resources = connectionPooler.getJSONObject("resources");
                JSONObject limits = resources.getJSONObject("limits");
                JSONObject requests = resources.getJSONObject("requests");
                limits.put("cpu",pgParam.getConnectionPoolerCpu());
                limits.put("memory",pgParam.getConnectionPoolerMemory());
                requests.put("cpu",pgParam.getConnectionPoolerCpu());
                requests.put("memory",pgParam.getConnectionPoolerMemory());
            }
        }

        //字符集
        JSONObject patroni = values.getJSONObject("patroni");
        if(patroni != null){
            JSONObject initdb = patroni.getJSONObject("initdb");
            if(StringUtils.isNotBlank(middleware.getCharSet())){
                initdb.put("encoding", middleware.getCharSet());
            }
            if(StringUtils.isNotBlank(middleware.getLanguage())){
                initdb.put("locale",middleware.getLanguage());
            }
        }

        // 备份恢复
        if (StringUtils.isNotEmpty(middleware.getBackupFileName())){
            try {
                MiddlewareBackup middlewareBackup = middlewareBackupCRService.get(cluster.getId(), middleware.getNamespace(), middleware.getBackupFileName());
                Map<String, Object> res = middlewareBackup.getStatus().getBackupResults().get(0);
                MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters mp = middlewareBackup.getSpec().getBackupDestination().getParameters();

                JSONObject clone = new JSONObject();
                clone.put("cluster", middlewareBackup.getSpec().getName());
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
        // 设置uid
        super.setSecurityContext(middleware, values);
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        super.convertCommonByHelmChart(middleware, values);
        super.convertResourcesByHelmChart(middleware, middleware.getType(), values.getJSONObject(RESOURCES));
        super.convertStoragesByHelmChart(middleware, middleware.getType(), values);
        super.convertRegistry(middleware, values);
        super.convertCustomVolumesByHelmChart(middleware, values);
        convertPostgresqlParamByHelmChart(middleware, values);
        super.convertSecurityContext(middleware, values);

        middleware.setIsAllLvmStorage(true);
        middleware.setVersion(values.getString("pgsqlVersion"));
        if (checkUserAuthority(MiddlewareTypeEnum.POSTGRESQL.getType())) {
            middleware.setPassword(values.getJSONObject("userPasswords").getString("postgres"));
        }
        middleware.setPassword(values.getJSONObject("userPasswords").getString("postgres"));

        if (middleware.getQuota() != null && middleware.getQuota().containsKey(middleware.getType())){
            middleware.getQuota().get(middleware.getType()).setNum(getReplicas(values) - 1);
        }

        List<MiddlewareQuota> storageClasses = getMiddlewareStorageClasses(cluster.getId(), middleware, values);
        middleware.setStorageResource(storageClasses);

        JSONObject patroni = values.getJSONObject("patroni");
        JSONObject initdb = patroni.getJSONObject("initdb");
        middleware.setCharSet(initdb.getString("encoding"));
        middleware.setLanguage(initdb.getString("locale"));
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

        //pool
        pgParam.setEnableConnectionPooler(values.getBoolean("enableConnectionPooler"));
        if(pgParam.getEnableConnectionPooler().equals(true)) {
            JSONObject connectionPooler = values.getJSONObject("connectionPooler");
            pgParam.setPoolerInstanceNum(connectionPooler.getInteger("numberOfInstances"));
            JSONObject resources = connectionPooler.getJSONObject("resources");
            JSONObject limits = resources.getJSONObject("limits");
            pgParam.setConnectionPoolerCpu(limits.getString("cpu"));
            pgParam.setConnectionPoolerMemory(limits.getString("memory"));
        }
        middleware.setPostgresqlParam(pgParam);
    }

    @Override
    public SwitchInfo getAutoSwitch(Middleware middleware) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        // 获取pod列表
        List<PodInfo> podInfos = podService.listMiddlewarePods(cluster.getId(), middleware.getNamespace(),
                middleware.getName(), MiddlewareTypeEnum.POSTGRESQL.getType());
        List<PodInfo> runningPods = podInfos.stream().filter(podInfo -> RUNNING.equalsIgnoreCase(podInfo.getStatus()))
                .collect(Collectors.toList());
        // pod状态全异常，直接返回缓存数据
        if (CollectionUtil.isEmpty(runningPods)) {
            BeanSystemConfig config = systemConfigService.getConfig(middleware.toStringKey());
            return new SwitchInfo().setStatus(false)
                .setIsAuto(config == null || Boolean.parseBoolean(config.getConfigValue()));
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
        List<String> resList = new ArrayList<>();
        try {
            resList = CmdExecUtil.execCmd(execCommand, null);
        } catch (Exception e) {
            log.error("查询自动切换失败", e);
        }

        SwitchInfo switchInfo = new SwitchInfo();
        // 查看pause
        if (!CollectionUtils.isEmpty(resList)){
            StringBuilder sb = new StringBuilder();
            resList.forEach(sb::append);
            JSONObject res = JSONObject.parseObject(sb.toString());
            if (res != null) {
                switchInfo.setStatus(true);
                switchInfo.setIsAuto(!res.containsKey(PAUSE) || !res.getBoolean("pause"));
                systemConfigService.saveConfig(middleware.toStringKey(), String.valueOf(switchInfo.getIsAuto()));
            }
        }
        // 处理无法获取到自动切换状态的异常状态
        if (switchInfo.getStatus() == null && switchInfo.getIsAuto() == null) {
            BeanSystemConfig config = systemConfigService.getConfig(middleware.toStringKey());
            switchInfo.setStatus(false);
            switchInfo.setIsAuto(config == null || Boolean.parseBoolean(config.getConfigValue()));
        }
        return switchInfo;
    }

    @Override
    public SwitchInfo getManualSwitch(Middleware middleware) {
        String patroniName = middleware.getName() + "-patroni";
        Integer patroniPort = getPatroniPort(middleware.getClusterId(), middleware.getNamespace(), patroniName);
        String podName = null;
        SwitchInfo switchInfo = new SwitchInfo().setStatus(true);
        try {
            JSONArray dataArray = postgresqlClientWrapper.cluster(middleware.getNamespace(), patroniName, patroniPort);
            for (int i = 0; i < dataArray.size(); ++i) {
                String role = dataArray.getJSONObject(i).getString("role");
                if (StringUtils.isNotEmpty(role) && "sync_standby".equals(role)) {
                    podName = dataArray.getJSONObject(i).getString("name");
                }
            }
            if (StringUtils.isEmpty(podName)) {
                switchInfo.setStatus(false);
            }
        } catch (Exception e) {
            log.error("查询postgresql: {} 是否可以手动切换失败", middleware.getName());
            log.debug("查询手动切换状态失败", e);
            switchInfo.setStatus(false);
        }
        return switchInfo;
    }


    /**
     * 检查是否是双活分区并设置双活配置字段
     */
    @Override
    public void checkAndSetActiveActive(JSONObject values, Middleware middleware) {
        if (namespaceService.isOpenAvailableDomain(middleware.getClusterId(), middleware.getNamespace())) {
            super.setActiveActiveConfig(null, values);
            super.setActiveActiveToleration(middleware, values);
            values.put(ACTIVE_ACTIVE, true);
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
        Integer patroniPort = getPatroniPort(middleware.getClusterId(), middleware.getNamespace(), patroniName);
        // 确认cluster信息
        String podName = null;
        JSONArray dataArray = postgresqlClientWrapper.cluster(middleware.getNamespace(), patroniName, patroniPort);
        for (int i = 0; i < dataArray.size(); ++i){
            String role = dataArray.getJSONObject(i).getString("role");
            if(StringUtils.isNotEmpty(role) && "sync_standby".equals(role)){
                podName = dataArray.getJSONObject(i).getString("name");
            }
        }
        if (StringUtils.isEmpty(podName)){
            throw new BusinessException(ErrorMessage.POSTGRESQL_SYNC_POD_NOT_EXIST);
        }
        String execCommand = MessageFormat.format(POSTGRESQL_HAND_SWITCH, podName, middleware.getNamespace(),
            cluster.getAddress(), cluster.getAccessToken(), patroniName, String.valueOf(patroniPort), podName);
        List<String> results = CmdExecUtil.runCmd(execCommand);
        // 判断结果
        parseHandSwitchResult(results);
        return new SwitchInfo().setNewMasterName(podName);
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
    public Double calculateCpuRequest(JSONObject values) {
        JSONObject resources = values.getJSONObject(RESOURCES);
        if (resources == null) {
            return 0.0;
        }
        String cpu = resources.getJSONObject(REQUESTS).getString(CPU);
        return ResourceCalculationUtil.getResourceValue(cpu, CPU, "") * getReplicas(values);
    }

    @Override
    public Integer getReplicas(JSONObject values) {
        return values.getIntValue("instances");
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

        //连接池参数
        if (middleware.getPostgresqlParam()!=null&&middleware.getPostgresqlParam().getPoolerInstanceNum()!=null){
            sb.append("connectionPooler.numberOfInstances=").append(middleware.getPostgresqlParam().getPoolerInstanceNum()).append(",");
        }
        if (middleware.getPostgresqlParam()!=null&&middleware.getPostgresqlParam().getConnectionPoolerCpu()!=null){
            Pattern pattern = Pattern.compile("^[0-9]+$");
            if (StringUtils.isNotEmpty(middleware.getPostgresqlParam().getConnectionPoolerCpu()) &&
                    pattern.matcher(middleware.getPostgresqlParam().getConnectionPoolerCpu()).matches()) {
                middleware.getPostgresqlParam().setConnectionPoolerCpu(middleware.getPostgresqlParam().getConnectionPoolerCpu() + DOT + "0");
            }
            sb.append("connectionPooler.resources.limits.cpu=").append(middleware.getPostgresqlParam().getConnectionPoolerCpu()).append(",");
            sb.append("connectionPooler.resources.requests.cpu=").append(middleware.getPostgresqlParam().getConnectionPoolerCpu()).append(",");
            sb.append("connectionPooler.resources.limits.memory=").append(middleware.getPostgresqlParam().getConnectionPoolerMemory()).append(",");
            sb.append("connectionPooler.resources.requests.memory=").append(middleware.getPostgresqlParam().getConnectionPoolerMemory()).append(",");
        }

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

        // 处理审计日志开启关闭时的参数修改
        if (middleware.getAudit() != null) {
            if (middleware.getAudit()) {
                sb.append("args.pgaudit\\\\.log=WRITE\\\\,DDL");
            } else if (!middleware.getAudit()) {
                sb.append("args.pgaudit\\\\.log=NONE");
            }
        }

        if (sb.length() == 0) {
            return;
        }
        // 去掉末尾的逗号
        if (sb.toString().endsWith(",")) {
            sb.deleteCharAt(sb.length() - 1);
        }
        // 更新helm
        helmChartService.upgrade(middleware, sb.toString(), null, cluster);

    }

    @Override
    public void reboot(String clusterId, String namespace, String name, String type, String podType) {
        Postgresql postgresql = postgresqlWrapper.get(clusterId, namespace, name);
        Integer numberOfInstances = postgresql.getSpec().getNumberOfInstances();
        if (numberOfInstances == 1 || (StringUtils.isNotEmpty(podType) && !"Master".equalsIgnoreCase(podType))) {
            super.reboot(clusterId, namespace, name, type, podType);
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

    public Integer getPatroniPort(String clusterId, String namespace, String name){
        Service patroniService = serviceWrapper.get(clusterId, namespace,  name);
        if (patroniService == null) {
            throw new BusinessException(DictEnum.SERVICE, name, ErrorMessage.NOT_EXIST);
        }
        // 获取获取patroniService端口
        Integer patroniPort = null;
        if (patroniService.getSpec() == null || CollectionUtils.isEmpty(patroniService.getSpec().getPorts())) {
            throw new BusinessException(DictEnum.SERVICE, name, ErrorMessage.INVALID_PARAMETER);
        }
        for (ServicePort port : patroniService.getSpec().getPorts()) {
            if ("patroni".equals(port.getName())) {
                patroniPort = port.getPort();
            }
        }
        return patroniPort;
    }

    @Override
    public Set<String> getCustomConfigRole(JSONObject values) {
        HashSet<String> result = new HashSet<>();
        result.add("major");
        if(values.getBoolean("enableConnectionPooler")){
            result.add("Pgbouncer");
        }
        return result;
    }

    @Override
    public Boolean withPgbouncer(String clusterId, Middleware middleware) {
        JSONObject values = helmChartService.getInstalledValues(middleware, clusterService.findById(clusterId));
        return values.getBoolean("enableConnectionPooler");
    }

    @Override
    public void replaceToleration(Middleware middleware, JSONObject values) {
        super.replaceToleration(middleware, values);
        if (!CollectionUtils.isEmpty(middleware.getTolerations())) {
            JSONArray jsonArray = K8sConvert.convertToleration2Json(middleware.getTolerations());
            JSONObject connectionPooler = values.getJSONObject("connectionPooler");
            connectionPooler.put("tolerations", jsonArray);
            StringBuffer sbf = new StringBuffer();
            for (String toleration : middleware.getTolerations()) {
                sbf.append(toleration).append(",");
            }
            connectionPooler.put("tolerationAry", sbf.substring(0, sbf.length()));
            values.put("connectionPooler", connectionPooler);
        }
    }

    @Override
    public void replaceNodeAffinity(Middleware middleware, JSONObject values) {
        super.replaceNodeAffinity(middleware,values);
        JSONObject connectionPooler = values.getJSONObject("connectionPooler");
        if (!CollectionUtils.isEmpty(middleware.getNodeAffinity())) {
            JSONObject nodeAffinity = K8sConvert.convertNodeAffinity2Json(middleware.getNodeAffinity());
            if (nodeAffinity != null) {
                connectionPooler.put("nodeAffinity", nodeAffinity);
            }
        } else {
            connectionPooler.put("nodeAffinity", new JSONObject());
        }
        values.put("connectionPooler", connectionPooler);
    }

}

