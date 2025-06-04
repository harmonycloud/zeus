package com.middleware.zeus.operator;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.constants.ActiveAreaConstant;
import com.middleware.zeus.common.constants.CommonConstant;
import com.middleware.zeus.common.constants.ContainerConstant;
import com.middleware.zeus.common.constants.MysqlConstant;
import com.middleware.zeus.common.enums.ComponentsEnum;
import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.common.enums.middleware.ResourceUnitEnum;
import com.middleware.zeus.common.enums.middleware.StorageClassProvisionerEnum;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.*;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.common.model.registry.HelmChartFile;
import com.middleware.zeus.service.k8s.IngressService;
import com.middleware.zeus.service.middleware.impl.MiddlewareServiceImpl;
import com.middleware.zeus.service.system.AlertUserService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.util.RequestUtil;
import com.middleware.zeus.util.ThreadPoolExecutorFactory;
import com.middleware.zeus.util.collection.JsonUtils;
import com.middleware.zeus.util.numeric.ResourceCalculationUtil;
import com.middleware.zeus.util.uuid.UUIDUtils;
import com.middleware.zeus.bean.*;
import com.middleware.zeus.dao.AlertRuleIdMapper;
import com.middleware.zeus.dao.BeanAlertRecordMapper;
import com.middleware.zeus.dao.BeanAlertRuleMapper;
import com.middleware.zeus.dao.BeanMiddlewareInfoMapper;
import com.middleware.zeus.integration.cluster.ServiceWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRuleGroups;
import com.middleware.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.middleware.zeus.schedule.MiddlewareManageTask;
import com.middleware.zeus.service.aspect.AspectService;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.*;
import com.middleware.zeus.service.middleware.impl.MiddlewareAlertsServiceImpl;
import com.middleware.zeus.service.middleware.impl.MiddlewareBackupServiceImpl;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.LicenseService;
import com.middleware.zeus.service.system.SystemConfigService;
import com.middleware.zeus.service.user.RoleAuthorityService;
import com.middleware.zeus.util.K8sConvert;
import com.middleware.zeus.util.numeric.MathUtil;
import io.fabric8.kubernetes.api.model.NodeAffinity;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.AlertConstant.SERVICE;
import static com.middleware.zeus.common.constants.CommonConstant.*;
import static com.middleware.zeus.common.constants.NameConstant.*;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;
import static com.middleware.zeus.common.constants.registry.HelmChartConstant.CHART_YAML_NAME;

/**
 * @author dengyulong
 * @date 2021/03/23 中间件通用处理
 */
@Slf4j
public abstract class AbstractBaseOperator {

    @Value("${active-active.label.key:topology.kubernetes.io/zone}")
    private String zoneKey;

    /**
     * 此处的注入，实际上是由各个OperatorImpl子类（如MysqlOperatorImpl）进行注入了，如果直接初始化当前类，会发现值为空
     */
    @Autowired
    protected ClusterService clusterService;
    @Autowired
    protected HelmChartService helmChartService;
    @Autowired
    protected PvcService pvcService;
    @Autowired
    protected MiddlewareCRService middlewareCRService;
    @Autowired
    protected MiddlewareInfoService middlewareInfoService;
    @Autowired
    protected IngressService ingressService;
    @Autowired
    protected MiddlewareManageTask middlewareManageTask;
    @Autowired
    private PrometheusRuleService prometheusRuleService;
    @Autowired
    private MiddlewareCustomConfigService middlewareCustomConfigService;
    @Autowired
    private MiddlewareServiceImpl middlewareService;
    @Autowired
    private ServiceService serviceService;
    @Autowired
    protected MiddlewareBackupServiceImpl middlewareBackupService;
    @Autowired
    private AspectService aspectService;
    @Autowired
    protected CacheMiddlewareService cacheMiddlewareService;
    @Autowired
    protected ClusterMiddlewareInfoService clusterMiddlewareInfoService;
    @Autowired
    protected StorageClassService storageClassService;
    @Autowired
    private BeanAlertRuleMapper beanAlertRuleMapper;
    @Autowired
    private AlertRuleIdMapper alertRuleIdMapper;
    @Autowired
    private MiddlewareAlertsServiceImpl middlewareAlertsService;
    @Autowired
    private ServiceWrapper serviceWrapper;
    @Autowired
    protected StorageService storageService;
    @Autowired
    protected NamespaceService namespaceService;
    @Autowired
    protected LicenseService licenseService;
    @Autowired
    private BeanMiddlewareInfoMapper middlewareInfoMapper;
    @Autowired
    private RoleAuthorityService roleAuthorityService;
    @Autowired
    private PodService podService;
    @Autowired
    private MaintenanceService maintenanceService;
    @Autowired
    private BeanAlertRecordMapper alertRecordMapper;
    @Autowired
    private ImageRepositoryService imageRepositoryService;
    @Autowired
    protected SystemConfigService systemConfigService;
    @Autowired
    private CustomConfigHistoryService customConfigHistoryService;
    @Autowired
    protected AlertUserService alertUserService;
    @Autowired
    protected SecretService secretService;
    @Autowired
    protected ProjectService projectService;

    /**
     * 是否支持该中间件
     */
    protected abstract boolean support(Middleware middleware);

    /**
     * 查询中间件列表
     */
    public List<Middleware> list(Middleware middleware, String keyword) {
        return null;
    }

    /**
     * 查询中间件列表
     */
    public Middleware detail(Middleware middleware) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        Middleware mw = middlewareCRService.simpleDetail(middleware.getClusterId(), middleware.getNamespace(),
            middleware.getType(), middleware.getName());
        if (mw == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middleware.getName(), ErrorMessage.NOT_EXIST);
        }
        mw.setNamespaceAliasName(namespaceService.get(mw.getClusterId(), mw.getNamespace()).getAliasName());
        return convertByHelmChart(mw, cluster);
    }

    public SwitchInfo getAutoSwitch(Middleware middleware) {
        return null;
    }

    public SwitchInfo getManualSwitch(Middleware middleware) {
        return null;
    }

    public void create(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (cluster == null) {
            cluster = clusterService.findById(middleware.getClusterId());
        }
        // 1. download and read helm chart from registry
        HelmChartFile helmChart =
            helmChartService.getHelmChartFromMysql(middleware.getChartName(), middleware.getChartVersion());

        // 2. deal with values.yaml andChart.yaml
        // load values.yaml to map
        Yaml yaml = new Yaml();
        JSONObject values = yaml.loadAs(helmChart.getValueYaml(), JSONObject.class);
        // deal with values.yaml file
        replaceValues(middleware, cluster, values);
        // synchronize the backup source service custom config to the backup service
        syncBackupSourceConfig(middleware, values);
        // deal with Charts.yaml file
        replaceChart(helmChart, values);
        // deal with dynamic
        aspectService.operation(cluster, middleware, middleware.getDynamicValues(), values);
        // map to yaml
        String newValuesYaml = yaml.dumpAsMap(values);
        // deal with 'version'
        helmChart.setValueYaml(newValuesYaml);
        // write to local file
        helmChartService.coverYamlFile(helmChart);
        // 3. helm package & install
        String tgzFilePath = helmChartService.packageChart(helmChart.getTarFileName(), middleware.getChartName(),
            middleware.getChartVersion());
        helmChartService.install(middleware, tgzFilePath, cluster, null);

        // 4. 创建对外访问
        ThreadPoolExecutorFactory.executor.execute(() -> {
            if (!CollectionUtils.isEmpty(middleware.getIngresses())) {
                try {
                    // 校验svc是否已创建
                    checkSvcCreated(middleware);
                    middleware.getIngresses().forEach(ingress -> ingressService.create(middleware.getClusterId(),
                        middleware.getNamespace(), middleware.getName(), ingress));
                } catch (Exception e) {
                    log.error("集群：{}，命名空间：{}，中间件：{}，创建对外访问异常", middleware.getClusterId(), middleware.getNamespace(),
                        middleware.getName(), e);
                }
            }
        });
        // 5. 修改prometheusRules添加集群
        updateAlerts(middleware);
        // add2sql(middleware);
        //6. 删除告警记录
        deleteRecord(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
        // license资源计算
        licenseService.addMiddlewareResource(cluster.getType(), calculateCpuRequest(values));
    }

    protected void syncBackupSourceConfig(Middleware middleware, JSONObject values) {
        if (middleware.getIsBackup() != null && middleware.getIsBackup() && middleware.getRelationMiddleware() != null) {
            BaseOperator operator = middlewareService.getOperator(BaseOperator.class, BaseOperator.class, middleware.getRelationMiddleware());
            customConfigHistoryService.listLatestConfig(middleware.getRelationMiddleware()).forEach(ccDo -> {
                String valuesType = operator.changeConfigRoleToValueArg(ccDo.getRole(), false);
                JSONObject args;
                if ("master".equalsIgnoreCase(valuesType)) {
                    args = values.getJSONObject("args");
                    if (args == null) {
                        args = new JSONObject();
                        values.put("args", args);
                    }
                } else {
                    JSONObject valuesTypeJSON = values.getJSONObject(valuesType);
                    if (valuesTypeJSON == null) {
                        valuesTypeJSON = new JSONObject();
                        values.put(valuesType, valuesTypeJSON);
                    }
                    args = valuesTypeJSON.getJSONObject("args");
                    if (args == null) {
                        args = new JSONObject();
                        valuesTypeJSON.put("args", args);
                    }
                }
                args.put(ccDo.getItem(), ccDo.getAfter());
            });

        }
    }

    public void delete(Middleware middleware) {
        // 获取集群
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        // check exist
        List<HelmListInfo> list = helmChartService.listHelm(middleware.getNamespace(), middleware.getName(), cluster);
        if (CollectionUtils.isEmpty(list)) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middleware.getName(), ErrorMessage.DOES_NOT_EXIST);
        }
        ingressService.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(),
            middleware.getName());
        // 获取values.yaml 并写入数据库
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        BeanCacheMiddleware beanCacheMiddleware = new BeanCacheMiddleware();
        BeanUtils.copyProperties(middleware, beanCacheMiddleware);
        if (values.containsKey("chart-version")) {
            beanCacheMiddleware.setChartVersion(values.getString("chart-version"));
        } else {
            BeanClusterMiddlewareInfo beanClusterMiddlewareInfo =
                clusterMiddlewareInfoService.get(cluster.getId(), middleware.getType());
            beanCacheMiddleware.setChartVersion(beanClusterMiddlewareInfo.getChartVersion());
        }
        // 获取pvc
        try {
            List<String> pvcNameList = middlewareCRService.getPvc(middleware.getClusterId(), middleware.getNamespace(),
                middleware.getType(), middleware.getName());
            StringBuilder sb = new StringBuilder();
            for (String name : pvcNameList) {
                sb.append(name).append(",");
            }
            if (sb.length() == 0) {
                beanCacheMiddleware.setPvc(null);
            } else {
                sb.deleteCharAt(sb.length() - 1);
                beanCacheMiddleware.setPvc(sb.toString());
            }
        } catch (Exception e) {
            log.error("获取中间件pvc失败", e);
        }

        beanCacheMiddleware.setValuesYaml(JSONObject.toJSONString(values));
        cacheMiddlewareService.insertIfNotPresent(beanCacheMiddleware);
        // helm卸载需要放到最后，要不然一些资源的查询会404
        helmChartService.uninstall(middleware, cluster);
    }

    public void deleteStorage(Middleware middleware) {
        BeanCacheMiddleware beanCacheMiddleware = cacheMiddlewareService.get(middleware);
        deletePvc(beanCacheMiddleware);
        deleteCustomConfigHistory(middleware);
        // 删除告警联系人绑定
        alertUserService.delete(null, middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), SERVICE);
        // 删除Maintenance
        maintenanceService.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
        // 删除备份相关
        middlewareBackupService.deleteMiddlewareBackupInfo(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
        // 删除额外prometheus rules文件
        prometheusRuleService.delete(middleware.getClusterId(), middleware.getNamespace(), ZEUS + LINE + middleware.getName());
        // 删除缓存数据
        systemConfigService.delete(middleware.toStringKey());
        removeSql(middleware);
        // 设置values.yaml为null
        cacheMiddlewareService.updateValuesToNull(middleware);
    }

    public List<PodInfoGroup> podInfoGroup(Middleware middleware){
        List<PodInfo> podInfoList = podService.listMiddlewarePods(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType());
        PodInfoGroup podInfoGroup = new PodInfoGroup();
        podInfoGroup.setRole(middleware.getType());
        if (podInfoList.stream().allMatch(podInfo -> podInfo.getStatus().equalsIgnoreCase(RUNNING))){
            podInfoGroup.setStatus(RUNNING);
        }else {
            podInfoGroup.setStatus("NotReady");
        }
        return Collections.singletonList(podInfoGroup);
    }


    /**
     * 更新自定义中间件
     */
    public void update(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (cluster == null) {
            cluster = clusterService.findById(middleware.getClusterId());
        }
        StringBuilder sb = new StringBuilder();

        // 实例扩容
        if (middleware.getQuota() != null && middleware.getQuota().get(middleware.getType()) != null
            && middleware.getQuota().get(VARIABLE) != null) {
            MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
            MiddlewareQuota variable = middleware.getQuota().get(VARIABLE);

            if (StringUtils.isNotBlank(quota.getCpu())) {
                sb.append(variable.getCpu()).append("=").append(quota.getCpu()).append(",")
                    .append(variable.getLimitCpu()).append("=").append(quota.getCpu()).append(",");
            }
            if (StringUtils.isNotBlank(quota.getMemory())) {
                sb.append(variable.getMemory()).append("=").append(quota.getMemory()).append(",")
                    .append(variable.getLimitMemory()).append("=").append(quota.getMemory()).append(",");
            }
        }
        // 更新通用字段
        updateCommonValues(sb, middleware);

        // 没有修改，直接返回
        if (sb.length() == 0) {
            return;
        }
        // 去掉末尾的逗号
        sb.deleteCharAt(sb.length() - 1);
        // 更新helm
        helmChartService.upgrade(middleware, sb.toString(), null, cluster);
    }
    /**
     * 更新通用字段
     *
     * @param sb
     * @param middleware
     */
    protected void updateCommonValues(StringBuilder sb, Middleware middleware) {
        // 备注
        if (middleware.getDescription() != null) {
            // 处理特殊字符 \ ,
            String desc = middleware.getDescription();
            if (desc.contains("\\")) {
                desc = desc.replace("\\", "\\\\");
                middleware.setDescription(middleware.getDescription().replace("\\", "\\\\"));
            }
            if (desc.contains(",")) {
                desc = desc.replace(",", "\\,");
            }
            sb.append("middleware-desc=\'").append(desc).append("\',");
        }

        // 日志开关
        if (null != middleware.getFilelogEnabled()) {
            sb.append("logging.collection.filelog.enabled=").append(middleware.getFilelogEnabled()).append(",");
        }
        if (null != middleware.getStdoutEnabled()) {
            sb.append("logging.collection.stdout.enabled=").append(middleware.getStdoutEnabled()).append(",");
        }
        // 添加sql审计开关
        if (middleware.getAudit() != null){
            sb.append("features.auditLog.enabled=").append(middleware.getAudit()).append(",");
        }
    }

    protected void deletePvc(BeanCacheMiddleware beanCacheMiddleware) {
        if (StringUtils.isNotEmpty(beanCacheMiddleware.getPvc())) {
            List<String> pvcList = Arrays.asList(beanCacheMiddleware.getPvc().split(","));
            if (!CollectionUtils.isEmpty(pvcList)) {
                pvcList.forEach(pvc -> pvcService.delete(beanCacheMiddleware.getClusterId(),
                    beanCacheMiddleware.getNamespace(), pvc));
            }
        }
    }

    /**
     * 更新pvc存储
     */
    protected void updatePvc(Middleware middleware, List<PersistentVolumeClaim> pvcList) {
        for (PersistentVolumeClaim pvc : pvcList) {
            if (CollectionUtils.isEmpty(pvc.getMetadata().getAnnotations())
                || !pvc.getMetadata().getAnnotations().containsKey("volume.beta.kubernetes.io/storage-provisioner")
                || !StorageClassProvisionerEnum.CSI_LVM.getProvisioner()
                    .equals(pvc.getMetadata().getAnnotations().get("volume.beta.kubernetes.io/storage-provisioner"))) {
                throw new BusinessException(ErrorMessage.NOT_LVM);
            }
        }
        String storage = middleware.getQuota().get(middleware.getType()).getStorageClassQuota();
        for (PersistentVolumeClaim pvc : pvcList) {
            pvc.getSpec().getResources().getRequests().put(STORAGE, new Quantity(storage));
            pvcService.update(middleware.getClusterId(), middleware.getNamespace(), pvc);
        }
    }

    /**
     * 获取hostnetwork访问地址
     * @return
     */
    public List<IngressDTO> getHostNetworkAddress(String clusterId, String namespace, String middlewareName) {
        return null;
    }

    public void updateStorage(Middleware middleware) {
        // 获取pvc
        List<String> pvcNameList = middlewareCRService.getPvc(middleware.getClusterId(), middleware.getNamespace(),
            middleware.getType(), middleware.getName());
        // 更新pvc内容
        List<PersistentVolumeClaim> pvcList = new ArrayList<>();
        for (String pvcName : pvcNameList) {
            pvcList.add(pvcService.get(middleware.getClusterId(), middleware.getNamespace(), pvcName));
        }
        updatePvc(middleware, pvcList);

        // 更新values.yaml
        StringBuilder sb = new StringBuilder();
        if (middleware.getType().equals(MiddlewareTypeEnum.ELASTIC_SEARCH.getType())) {
            for (String key : middleware.getQuota().keySet()) {
                sb.append("storage.").append(key).append("Size").append("=")
                    .append(middleware.getQuota().get(key).getStorageClassQuota()).append(",");
            }
        } else {
            sb.append("storageSize=").append(middleware.getQuota().get(middleware.getType()).getStorageClassQuota())
                .append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        helmChartService.upgrade(middleware, sb.toString(), null, clusterService.findById(middleware.getClusterId()));
    }

    public void deleteCustomConfigHistory(Middleware mw) {
        try {
            middlewareCustomConfigService.deleteHistory(mw.getClusterId(), mw.getNamespace(), mw.getName());
        } catch (Exception e) {
            log.error("集群{} 分区{} 中间件{} 自定义参数修改历史删除失败: {}", mw.getClusterId(), mw.getNamespace(), mw.getName(), e);
        }
    }

    /**
     * 删除告警记录
     * @param clusterId
     * @param namespace
     * @param type
     * @param middlewareName
     */
    private void deleteRecord(String clusterId, String namespace, String type, String middlewareName) {
        QueryWrapper<BeanAlertRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId);
        wrapper.eq("namespace", namespace);
        wrapper.eq("type", type);
        wrapper.eq("name", middlewareName);
        alertRecordMapper.delete(wrapper);
    }

    public void deleteMiddlewareBackupInfo(Middleware mw){

    }

    public SwitchInfo switchMiddleware(Middleware middleware) {
        return null;
    }

    public SwitchInfo switchMiddleware(Middleware middleware, String slaveName) {
        return null;
    }

    public void parseHandSwitchResult(List<String> results){
        if (!"200".equals(results.get(1)) && !"202".equals(results.get(1))) {
            String errorMessage = results.get(0);
            if (errorMessage.startsWith("Not failed over, because this instance is delay")) {
                throw new BusinessException(ErrorMessage.SWITCH_FAILD_BECAUSE_DELAY);
            } else if ("411".equals(results.get(1)) && results.get(0).endsWith("please apply your changes to the latest version and try again")) {
                throw new BusinessException(ErrorMessage.SWITCH_FAILED, "," + results.get(0));
            } else {
                log.error("切换结果: {}", results.get(0));
                log.error("切换状态: {}", results.get(1));
                throw new BusinessException(ErrorMessage.SWITCH_FAILED);
            }
        }
    }
    /**
     * 从helm chart转回middleware
     */
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (StringUtils.isEmpty(middleware.getClusterId())) {
            middleware.setClusterId(cluster.getId());
        }
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        return convertByHelmChart(middleware, cluster, values);
    }

    /**
     * 从helm chart转回middleware
     */
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        convertCommonByHelmChart(middleware, values);
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        return middleware;
    }

    /**
     * 设置中间件图片
     *
     * @param middleware
     * @param values
     */
    private void setImagePath(Middleware middleware, JSONObject values) {
        String chartVersion = values.getString("chart-version");
        String middlewareType = middleware.getType();
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("chart_name", middlewareType);
        wrapper.eq("chart_version", chartVersion);
        BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoMapper.selectOne(wrapper);
        middleware.setImagePath(beanMiddlewareInfo.getImagePath());
    }

    /**
     * 根据helm chart的values转换
     */
    protected void convertCommonByHelmChart(Middleware middleware, JSONObject values) {
        if (values != null) {
            middleware.setAliasName(values.getString("aliasName")).setDescription(values.getString("middleware-desc"))
                .setVersion(values.getString("version"))
                .setMode(values.getString(MODE));
            // 获取chart-version
            middleware.setChartVersion(helmChartService.getChartVersion(values, middleware.getType()));
            // 获取labels
            if (values.containsKey("labels")) {
                JSONObject labels = values.getJSONObject("labels");
                StringBuilder builder = new StringBuilder();
                for (String key : labels.keySet()) {
                    builder.append(key).append("=").append(labels.getString(key)).append(",");
                }
                if (builder.length() != 0) {
                    builder.deleteCharAt(builder.length() - 1);
                    middleware.setLabels(builder.toString());
                }
            }
            // 获取annotations
            if (values.containsKey("annotations")) {
                JSONObject ann = values.getJSONObject("annotations");
                StringBuilder builder = new StringBuilder();
                for (String key : ann.keySet()) {
                    builder.append(key).append("=").append(ann.getString(key)).append(",");
                }
                if (builder.length() != 0) {
                    builder.deleteCharAt(builder.length() - 1);
                    middleware.setAnnotations(builder.toString());
                }
            }

            // 动态参数
            if (values.containsKey("custom")) {
                convertDynamicValues(middleware, values);
            }

            // node affinity
            convertNodeAffinity(middleware, values);

            // toleration
            if (values.containsKey("tolerationAry")) {
                String tolerationAry = values.getString("tolerationAry");
                middleware.setTolerations(new ArrayList<>(Arrays.asList(tolerationAry.split(","))));
            }
            // description
            if (values.getString("middleware-desc") != null) {
                middleware.setDescription(values.getString("middleware-desc"));
            }
            // log
            if (JsonUtils.isJsonObject(values.getString("logging"))) {
                JSONObject logging = values.getJSONObject("logging");
                JSONObject collection = logging.getJSONObject("collection");
                Boolean filelogEnabled = collection.getJSONObject("filelog").getBoolean("enabled");
                Boolean stdoutEnabled = collection.getJSONObject("stdout").getBoolean("enabled");
                middleware.setFilelogEnabled(filelogEnabled);
                middleware.setStdoutEnabled(stdoutEnabled);
            }

            // audit
            if (values.containsKey("features")){
                JSONObject features = values.getJSONObject("features");
                if (features.getJSONObject(MysqlConstant.KEY_FEATURES_AUDITLOG) != null) {
                    middleware.setAudit(features.getJSONObject(MysqlConstant.KEY_FEATURES_AUDITLOG).getBoolean("enabled"));
                }
            }
            // 扩展调度器
            if (values.containsKey("statefulSetConfiguration")
                && values.getJSONObject("statefulSetConfiguration").containsKey("schedulerName")) {
                if (values.getJSONObject("statefulSetConfiguration").getString("schedulerName")
                    .equals(ComponentsEnum.MIDDLEWARE_SCHEDULER.getName())) {
                    middleware.setScheduler(true);
                }
            }
        } else {
            middleware.setAliasName(middleware.getName());
        }
    }

    public void convertNodeAffinity(Middleware middleware, JSONObject values) {
        if (JsonUtils.isJsonObject(values.getString("nodeAffinity"))) {
            JSONObject nodeAffinity = values.getJSONObject("nodeAffinity");
            if (!CollectionUtils.isEmpty(nodeAffinity)) {
                List<AffinityDTO> dto = K8sConvert.convertNodeAffinity(
                    JSONObject.parseObject(nodeAffinity.toJSONString(), NodeAffinity.class), AffinityDTO.class);
                middleware.setNodeAffinity(dto);
            }
        }
    }

    protected void convertResourcesByHelmChart(Middleware middleware, String quotaKey, JSONObject resources) {
        if (middleware == null || StringUtils.isBlank(quotaKey) || resources == null) {
            return;
        }

        // quota
        MiddlewareQuota quota = checkMiddlewareQuota(middleware, quotaKey);
        JSONObject requests = resources.getJSONObject("requests");
        JSONObject limits = resources.getJSONObject("limits");

        // 因为cpu涉及单位转换，所以根据单位是否为m做转换处理
        if (requests.getString(CPU).endsWith(ResourceUnitEnum.M.getUnit())) {
            quota.setCpu(String.valueOf(ResourceCalculationUtil.getResourceValue(requests.getString(CPU), CPU, "")));
        } else {
            quota.setCpu(requests.getString(CPU));
        }
        if (limits.getString(CPU).endsWith(ResourceUnitEnum.M.getUnit())) {
            quota.setLimitCpu(String.valueOf(ResourceCalculationUtil.getResourceValue(limits.getString(CPU), CPU, "")));
        } else {
            quota.setLimitCpu(limits.getString(CPU));
        }
        // 设置内存
        quota.setMemory(requests.getString(MEMORY)).setLimitMemory(limits.getString(MEMORY));
    }

    protected void convertStoragesByHelmChart(Middleware middleware, String quotaKey, JSONObject values) {
        if (StringUtils.isBlank(quotaKey) || values == null) {
            return;
        }
        String storageClass = values.getString("storageClassName");
        MiddlewareQuota quota = checkMiddlewareQuota(middleware, quotaKey);
        quota.setStorageClassName(storageClass)
            .setStorageClassQuota(values.getString("storageSize"));
        // 获取存储信息
        try {
            // 查询sc
            if(storageClass.contains(CommonConstant.COMMA)){
                storageClass = storageClass.split(CommonConstant.COMMA)[0];
            }
            StorageDto storageDto = storageService.get(middleware.getClusterId(), storageClass);
            // 设置是否lvm
            quota.setIsLvmStorage(storageClassService.checkLVMStorage(storageDto));
            // 设置中文名称
            if (StringUtils.isNotEmpty(storageDto.getAliasName())){
                quota.setStorageClassAliasName(storageDto.getAliasName());
            }
            // 设置provisioner
            if (!CollectionUtils.isEmpty(storageDto.getStorageClassList())){
                quota.setProvisioner(storageDto.getStorageClassList().get(0).getProvisioner());
            }
        } catch (Exception e) {
            log.debug("中间件{}, 设置存储信息失败", middleware.getName());
        }
    }

    public MiddlewareQuota checkMiddlewareQuota(Middleware middleware, String quotaKey) {
        if (middleware.getQuota() == null) {
            middleware.setQuota(new HashMap<>());
        }
        MiddlewareQuota quota = middleware.getQuota().get(quotaKey);
        if (quota == null) {
            quota = new MiddlewareQuota();
            middleware.getQuota().put(quotaKey, quota);
        }
        return quota;
    }

    public void createPreCheck(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (StringUtils.isEmpty(middleware.getName())) {
            return;
        }
        // throws if it exists
        List<HelmListInfo> helms = helmChartService.listHelm(middleware.getNamespace(), null, cluster);
        if (helms.stream().anyMatch(h -> middleware.getName().equals(h.getName()))) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middleware.getName(), ErrorMessage.EXIST);
        }
        // 数据仍未清清除
        if (!ObjectUtils.isEmpty(cacheMiddlewareService.get(middleware))) {
            throw new BusinessException(ErrorMessage.SAME_NAME_MIDDLEWARE_STORAGE_EXIST);
        }
        // 分区配额校验
        if (!middlewareService.middlewareResourceCheck(middleware)){
            throw new BusinessException(ErrorMessage.NAMESPACE_QUOTA_NOT_ENOUGH);
        }
    }

    public void updatePreCheck(Middleware middleware, MiddlewareClusterDTO cluster) {
        // throws if it exists
        List<HelmListInfo> helms = helmChartService.listHelm(middleware.getNamespace(), null, cluster);
        if (helms.stream().noneMatch(h -> middleware.getName().equals(h.getName()))) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middleware.getName(), ErrorMessage.NOT_EXIST);
        }
        // 分区配额校验
        if (!middlewareService.middlewareResourceCheck(middleware)){
            throw new BusinessException(ErrorMessage.NAMESPACE_QUOTA_NOT_ENOUGH);
        }
    }

    public void convertDynamicValues(Middleware middleware, JSONObject values) {
        HelmChartFile helm = helmChartService.getHelmChartFromMysql(middleware.getType(), middleware.getChartVersion());
        QuestionYaml questionYaml = helmChartService.getQuestionYaml(helm);
        Map<String, String> dynamicValues = new HashMap<>();
        // 解析question.yaml
        convertQuestions(questionYaml.getQuestions(), dynamicValues, values);
        middleware.setDynamicValues(dynamicValues);
        // 设置动态tab
        middleware.setCapabilities(questionYaml.getCapabilities());
        // 获取cpu和memory
        String type = middleware.getType();
        checkMiddlewareQuota(middleware, type);
        middleware.getQuota().put(VARIABLE, new MiddlewareQuota());
        questionYaml.getQuestions().forEach(question -> {
            String[] var = question.getVariable().split("\\.");
            if (var.length > 2) {
                if (CPU.equals(var[var.length - 1])) {
                    if (REQUESTS.equals(var[var.length - 2])) {
                        middleware.getQuota().get(type).setCpu(getValuesByVariable(question.getVariable(), values));
                        middleware.getQuota().get(VARIABLE).setCpu(question.getVariable());
                    }
                    if (LIMITS.equals(var[var.length - 2])) {
                        middleware.getQuota().get(type)
                            .setLimitCpu(getValuesByVariable(question.getVariable(), values));
                        middleware.getQuota().get(VARIABLE).setLimitCpu(question.getVariable());
                    }
                } else if (MEMORY.equals(var[var.length - 1])) {
                    if (REQUESTS.equals(var[var.length - 2])) {
                        middleware.getQuota().get(type).setMemory(getValuesByVariable(question.getVariable(), values));
                        middleware.getQuota().get(VARIABLE).setMemory(question.getVariable());
                    }
                    if (LIMITS.equals(var[var.length - 2])) {
                        middleware.getQuota().get(type)
                            .setLimitMemory(getValuesByVariable(question.getVariable(), values));
                        middleware.getQuota().get(VARIABLE).setLimitMemory(question.getVariable());
                    }
                }
            }
        });
    }

    /**
     * 获取包含需要在详情页展示的字段
     */
    public void convertQuestions(List<Question> questions, Map<String, String> dynamicValues, JSONObject values) {
        questions.forEach(question -> {
            if (question.getDetail() != null && question.getDetail() && !"nodeAffinity".equals(question.getType())
                && !"tolerations".equals(question.getType())) {
                String value = getValuesByVariable(question.getVariable(), values);
                dynamicValues.put(question.getLabel(), value);
                if (StringUtils.isNotEmpty(question.getShowSubQuestionIf())
                    && value.equals(question.getShowSubQuestionIf())) {
                    convertQuestions(question.getSubQuestions(), dynamicValues, values);
                }
            }
        });
    }

    public void convertCustomVolumesByHelmChart(Middleware middleware, JSONObject values) {
        Object volumeObject = values.get("customVolumes");
        if (!(volumeObject instanceof List)) {
            return;
        }
        JSONArray array = values.getJSONArray("customVolumes");
        if (!CollectionUtils.isEmpty(array)) {
            Map<String, CustomVolume> customVolumeMap = new HashMap<>();
            array.forEach(item -> {
                CustomVolume customVolume = null;
                try {
                    JSONObject obj = (JSONObject) item;
                    customVolume = new CustomVolume();
                    customVolume.setName(obj.get("name").toString());
                    customVolume.setMountPath(obj.get("mountPath").toString());
                    ArrayList<String> targetContainers = (ArrayList<String>) obj.get("targetContainers");
                    customVolume.setTargetContainers(targetContainers);
                    customVolume.setStorageClass(obj.get("storageClass").toString());
                    customVolume.setVolumeSize(String.valueOf(MathUtil.extractDigital(obj.get("volumeSize").toString())));
                    customVolume.setHostPath(obj.get("hostPath").toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                customVolumeMap.put(customVolume.getName(), customVolume);
            });
            middleware.setCustomVolumes(customVolumeMap);
        }
    }

    /**
     * 通过variable在values中获取对应的值
     */
    public String getValuesByVariable(String variable, JSONObject values) {
        String[] var = variable.split("\\.");
        String value;
        if (var.length == 1) {
            value = values.getString(var[0]);
        } else {
            JSONObject object = values;
            for (int i = 0; i < var.length - 1; ++i) {
                object = object.getJSONObject(var[i]);
            }
            value = object.getString(var[var.length - 1]);
        }
        return value;
    }

    /**
     * 替换掉values，子类应该各自覆盖该方法
     */
    protected void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        replaceSimplyCommonValues(middleware, cluster, values);
        replaceDynamicValuesContent(middleware, cluster);
        replaceDynamicValues(middleware, values);
        // 标记为自定义中间件
        values.put("custom", true);
        // 记录自定义tab页
        if (CollectionUtils.isEmpty(middleware.getCapabilities())) {
            values.put("dynamicTabs", new ArrayList<>(Collections.singletonList("basic")));
        } else {
            values.put("dynamicTabs", middleware.getCapabilities());
        }
    }

    protected void replaceSimplyCommonValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        values.put("version", middleware.getVersion());
        values.put("nameOverride", middleware.getName());
        values.put("fullnameOverride", middleware.getName());
        values.put("aliasName",
            StringUtils.isBlank(middleware.getAliasName()) ? middleware.getName() : middleware.getAliasName());
        values.put("middleware-desc", middleware.getDescription());
        values.put("chart-version", middleware.getChartVersion());
        values.put("clusterId", cluster.getId());

        // labels
        replaceLabels(middleware, values);
        // node affinity
        replaceNodeAffinity(middleware, values);
        // log
        replaceLog(middleware, values);
        // toleration
        replaceToleration(middleware, values);
        // annotations
        replaceAnnotations(middleware, values);
    }

    protected void replaceLabels(Middleware middleware, JSONObject values) {
        if (StringUtils.isNotBlank(middleware.getLabels())) {
            String[] labelAry = middleware.getLabels().split(CommonConstant.COMMA);
            JSONObject labelJson = new JSONObject();
            for (String label : labelAry) {
                String[] pair = label.split(CommonConstant.EQUAL);
                if (pair.length == 1) {
                    labelJson.put(pair[0], "");
                } else {
                    labelJson.put(pair[0], pair[1]);
                }
            }
            values.put("labels", labelJson);
        }
    }

    protected void replaceNodeAffinity(Middleware middleware, JSONObject values) {
        if (!CollectionUtils.isEmpty(middleware.getNodeAffinity())) {
            // convert to k8s model
            JSONObject nodeAffinity = K8sConvert.convertNodeAffinity2Json(middleware.getNodeAffinity());
            if (nodeAffinity != null) {
                values.put("nodeAffinity", nodeAffinity);
            }
        } else {
            values.put("nodeAffinity", new JSONObject());
        }
    }

    protected void replaceLog(Middleware middleware, JSONObject values){
        // 标准日志和文件日志
        JSONObject logging = new JSONObject();
        JSONObject collection = new JSONObject();

        JSONObject filelog = new JSONObject();
        filelog.put("enabled", middleware.getFilelogEnabled());
        JSONObject stdout = new JSONObject();
        stdout.put("enabled", middleware.getStdoutEnabled());

        collection.put("filelog", filelog);
        collection.put("stdout", stdout);
        logging.put("collection", collection);
        values.put("logging", logging);

        // 审计日志
        if (middleware.getAudit() != null){
            JSONObject features = values.getJSONObject("features");
            checkAndSetAuditSqlStatus(features, middleware);
        }
    }

    /**
     * 检查SQL审计采集开关，若支持SQL审计，则默认设置为开
     */
    private void checkAndSetAuditSqlStatus(JSONObject features, Middleware middleware) {
        if (features == null) {
            return;
        }
        if (features.getJSONObject(MysqlConstant.KEY_FEATURES_AUDITLOG) != null) {
            if (middleware.getAudit() != null) {
                features.getJSONObject(MysqlConstant.KEY_FEATURES_AUDITLOG).put("enabled", middleware.getAudit());
            } else {
                features.getJSONObject(MysqlConstant.KEY_FEATURES_AUDITLOG).put("enabled", false);
            }
        }
    }

    protected void replaceToleration(Middleware middleware, JSONObject values) {
        if (!CollectionUtils.isEmpty(middleware.getTolerations())) {
            JSONArray jsonArray = K8sConvert.convertToleration2Json(middleware.getTolerations());
            values.put("tolerations", jsonArray);
            StringBuffer sbf = new StringBuffer();
            for (String toleration : middleware.getTolerations()) {
                sbf.append(toleration).append(",");
            }
            values.put("tolerationAry", sbf.substring(0, sbf.length()));
        }
    }

    protected void replaceAnnotations(Middleware middleware, JSONObject values) {
        if (StringUtils.isNotEmpty(middleware.getAnnotations())) {
            JSONObject ann = new JSONObject();
            String[] annotations = middleware.getAnnotations().split(",");
            for (String annotation : annotations) {
                String[] temp = annotation.split("=");
                ann.put(temp[0], temp[1]);
            }
            values.put("annotations", ann);
        }
    }

    /**
     * 替换通用值
     */
    protected void replaceCommonValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        replaceSimplyCommonValues(middleware, cluster, values);
        if (StringUtils.isNotEmpty(middleware.getMode())) {
            values.put(MODE, middleware.getMode());
        }
        // image
        JSONObject image = values.getJSONObject("image");
        if (image != null) {
            Registry registry = cluster.getRegistry();
            image.put("repository", registry.getRegistryAddress() + "/"
                + (StringUtils.isBlank(registry.getImageRepo()) ? registry.getChartRepo() : registry.getImageRepo()));
        }
        // 读写分离
        if (middleware.getReadWriteProxy() != null && middleware.getReadWriteProxy().getEnabled()){
            replaceReadWriteProxyValues(middleware, values);
        }
        // 自定义目录
        replaceCustomVolumeValues(middleware, values);

        // 扩展调度器
        if(middleware.getScheduler() != null && middleware.getScheduler()){
            JSONObject statefulSetConfiguration = values.getJSONObject("statefulSetConfiguration");
            if (statefulSetConfiguration == null){
                statefulSetConfiguration = new JSONObject();
            }
            statefulSetConfiguration.put("schedulerName", "middleware-scheduler");
        }
    }

    // 处理自定义目录
    private void replaceCustomVolumeValues(Middleware middleware, JSONObject values){
        if(!CollectionUtils.isEmpty(middleware.getCustomVolumes())){
            values.put("customVolumes", convertCustomVolumes(middleware.getCustomVolumes()));
        }
    }

    /**
     * 处理读写分离
     */
    protected void replaceReadWriteProxyValues(Middleware middleware, JSONObject values){

    }

    /**
     * 设置容器安全上下文信息，例如uid、gid
     * @param middleware
     * @param target
     * @return
     */
    public void setSecurityContext(Middleware middleware, JSONObject target) {
        JSONObject securityContext = target.containsKey(ContainerConstant.SECURITY_CONTEXT)
            ? target.getJSONObject(ContainerConstant.SECURITY_CONTEXT) : new JSONObject();
        if (securityContext == null){
            securityContext = new JSONObject();
        }
        // 设置uid
        if (middleware.getContainerUID() != null) {
            securityContext.put(ContainerConstant.UID, middleware.getContainerUID());
        }
        // 设置gid
        if (middleware.getContainerGID() != null) {
            securityContext.put(ContainerConstant.GID, middleware.getContainerGID());
        }
        target.put(ContainerConstant.SECURITY_CONTEXT, securityContext);
    }

    public void convertSecurityContext(Middleware middleware, JSONObject values) {
        if (!values.containsKey(ContainerConstant.SECURITY_CONTEXT)
            || values.getJSONObject(ContainerConstant.SECURITY_CONTEXT) == null) {
            return;
        }
        JSONObject securityContext = values.getJSONObject(ContainerConstant.SECURITY_CONTEXT);
        if (securityContext.containsKey(ContainerConstant.UID)) {
            middleware.setContainerUID(securityContext.getLong(ContainerConstant.UID));
        }
        if (securityContext.containsKey(ContainerConstant.GID)) {
            middleware.setContainerGID(securityContext.getLong(ContainerConstant.GID));
        }
    }

    /**
     * 处理动态表单
     */
    protected void replaceDynamicValues(Middleware middleware, JSONObject values) {
        Map<String, String> dynamicValues = middleware.getDynamicValues();
        for (String key : dynamicValues.keySet()) {
            if ("labels".equals(key)) {
                continue;
            }
            // 是否存在多级
            if (key.contains(".")) {
                String[] nested = key.split("\\.");
                int length = nested.length;
                JSONObject object = values;
                for (int i = 0; i < length - 1; ++i) {
                    if (!object.containsKey(nested[i])) {
                        object.put(nested[i], new JSONObject());
                    }
                    object = object.getJSONObject(nested[i]);
                }
                object.put(nested[length - 1], dynamicValues.get(key));
            } else {
                if ("description".equals(key)) {
                    values.put("middleware-desc", dynamicValues.get(key));
                } else {
                    values.put(key, dynamicValues.get(key));
                }
            }
        }
    }

    private void replaceDynamicValuesContent(Middleware middleware, MiddlewareClusterDTO cluster) {
        for (String key : middleware.getDynamicValues().keySet()) {
            if (middleware.getDynamicValues().get(key).toString().contains("${address}")) {
                middleware.getDynamicValues().put(key, middleware.getDynamicValues().get(key).toString()
                    .replace("${address}", cluster.getRegistry().getAddress()));
            }
            if (middleware.getDynamicValues().get(key).toString().contains("${port}")) {
                middleware.getDynamicValues().put(key, middleware.getDynamicValues().get(key).toString()
                    .replace("${port}", String.valueOf(cluster.getRegistry().getPort())));
            }
            if (middleware.getDynamicValues().get(key).toString().contains("${repository}")) {
                middleware.getDynamicValues().put(key, middleware.getDynamicValues().get(key).toString()
                    .replace("${repository}", cluster.getRegistry().getImageRepo()));
            }
        }
    }

    /**
     * 处理通用的资源配额
     */
    protected void replaceCommonResources(MiddlewareQuota quota, JSONObject resources) {
        // 设置limit的resources
        setLimitResources(quota);

        JSONObject requests = resources.getJSONObject("requests");
        JSONObject limits = resources.getJSONObject("limits");
        if (StringUtils.isNotBlank(quota.getCpu())) {
            requests.put(CPU, quota.getCpu());
            limits.put(CPU, quota.getLimitCpu());
        }
        if (StringUtils.isNotBlank(quota.getMemory())) {
            requests.put(MEMORY, quota.getMemory());
            limits.put(MEMORY, quota.getLimitMemory());
        }
    }

    /**
     * 处理通用的存储
     */
    protected void replaceCommonStorages(MiddlewareQuota quota, JSONObject values) {
        if (!StringUtils.isEmpty(quota.getStorageClassName())) {
            values.put("storageClassName", quota.getStorageClassName());
        }
        if (!StringUtils.isEmpty(quota.getStorageClassQuota())) {
            values.put("storageSize", quota.getStorageClassQuota() + "Gi");
        }
    }

    /**
     * 替换chart.yaml
     */
    protected void replaceChart(HelmChartFile helmChart, JSONObject values) {
        JSONObject chart = JSONObject.parseObject(helmChart.getYamlFileMap().get(CHART_YAML_NAME));

        // 1. 发布时需检查组件Chart.yaml中的依赖关系，如果有依赖控制面组件的话，则需发布时禁止控制面组件发布
        // 即如果dependencies里，有alias名称包含了operator的，需要把对应的operator设置成false
        JSONArray dependencies = chart.getJSONArray("dependencies");
        if (!CollectionUtils.isEmpty(dependencies)) {
            for (int i = 0; i < dependencies.size(); i++) {
                JSONObject dependence = dependencies.getJSONObject(i);
                if (dependence.getString("alias") != null && dependence.getString("alias").contains("operator")) {
                    StringBuilder sb = new StringBuilder();
                    String[] condKeys = dependence.getString("condition").split("\\.");
                    for (int j = 1; j < condKeys.length; j++) {
                        sb.append("{").append("\"").append(condKeys[j]).append("\":");
                    }
                    sb.append(false);
                    // 补上右括号
                    for (int j = 1; j < condKeys.length; j++) {
                        sb.append("}");
                    }
                    values.put(condKeys[0], JSONObject.parseObject(sb.toString()));
                }
            }
        }
    }

    /**
     * 设置limit的cpu和memory
     */
    protected void setLimitResources(MiddlewareQuota quota) {
        if (StringUtils.isNotBlank(quota.getCpu()) && StringUtils.isBlank(quota.getLimitCpu())) {
            quota.setLimitCpu(quota.getCpu());
        }
        if (StringUtils.isNotBlank(quota.getMemory())) {
            if (NumberUtils.isNumber(quota.getMemory())) {
                quota.setMemory(quota.getMemory() + "Gi");
            }

            if (StringUtils.isBlank(quota.getLimitMemory())) {
                quota.setLimitMemory(quota.getMemory());
            } else if (NumberUtils.isNumber(quota.getMemory())) {
                quota.setLimitMemory(quota.getLimitMemory() + "Gi");
            }
        }
    }

    /**
     * 计算pod/jvm内存等
     *
     * @param limitMemory limit的memory
     * @param rate 比例
     * @param unitM m的单位
     * @return
     */
    protected String calculateMem(String limitMemory, String rate, String unitM) {
        // 先转为mb
        double memory = ResourceCalculationUtil.getResourceValue(limitMemory, MEMORY, ResourceUnitEnum.MI.getUnit());
        // 乘以倍率，并保存为整数
        double mem = ResourceCalculationUtil.roundNumber((BigDecimal.valueOf(memory).multiply(new BigDecimal(rate))), 0,
            RoundingMode.CEILING);
        return (long)mem + unitM;
    }

    /**
     * 更新prometheusRules
     */
    public void updateAlerts(Middleware middleware) {
        // 获取cr
        try {
            PrometheusRule prometheusRule =
                prometheusRuleService.get(middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
            prometheusRule.getSpec().getGroups().forEach(group -> group.getRules().forEach(rule -> {
                if (!CollectionUtils.isEmpty(rule.getLabels())) {
                    rule.getLabels().put("clusterId", middleware.getClusterId());
                    rule.getLabels().put("namespace", middleware.getNamespace());
                    rule.getLabels().put("service", middleware.getName());
                    rule.getLabels().put("middleware", middleware.getType());
                }
            }));
            prometheusRuleService.update(middleware.getClusterId(), prometheusRule);
        } catch (Exception e) {
            log.error("集群{} 分区{} 中间件{}， 告警规则标签添加集群失败", middleware.getClusterId(), middleware.getNamespace(),
                middleware.getName());
        }
        try {
            // 开启备份通知
            middlewareAlertsService.editBackupAlert(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType(), true);
        } catch (Exception e) {
            log.error("集群{} 分区{} 中间件{}， 开启备份通知失败", middleware.getClusterId(), middleware.getNamespace(),
                    middleware.getName());
        }

    }

    /**
     * 创建NodePort服务
     *
     * @param middleware 中间件信息
     * @param middlewareServiceNameIndex 中间件服务名称
     */
    public void createOpenService(Middleware middleware, MiddlewareServiceNameIndex middlewareServiceNameIndex) {
        // 1.获取所有对外服务，判断指定类型的服务是否已创建，如果已创建则直接返回
        List<IngressDTO> ingressDTOS = ingressService.get(middleware.getClusterId(), middleware.getNamespace(),
            middleware.getType(), middleware.getName());
        if (!CollectionUtils.isEmpty(ingressDTOS)) {
            String finalServiceName = middlewareServiceNameIndex.getNodePortServiceName();
            List<
                IngressDTO> ingressDTOList =
                    ingressDTOS.stream()
                        .filter(ingressDTO -> (ingressDTO.getName().contains(finalServiceName)
                            && ingressDTO.getExposeType().equals(MIDDLEWARE_EXPOSE_NODEPORT)))
                        .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(ingressDTOList)) {
                return;
            }
        }

        List<ServicePortDTO> servicePortDTOS = serviceService.list(middleware.getClusterId(), middleware.getNamespace());
        String finalMiddlewareServiceNameSuffix = middlewareServiceNameIndex.getMiddlewareServiceNameSuffix();
        List<ServicePortDTO> serviceList = servicePortDTOS.stream()
            .filter(servicePortDTO -> servicePortDTO.getServiceName().endsWith(finalMiddlewareServiceNameSuffix))
            .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(serviceList)) {
            ServicePortDTO servicePortDTO = serviceList.get(0);
            PortDetailDTO portDetailDTO = servicePortDTO.getPortDetailDtoList().get(0);
            // 2.将服务通过NodePort暴露为对外服务
            log.info("开始创建对外服务,clusterId={},namespace={},middlewareName={}", middleware.getClusterId(),
                middleware.getNamespace(), middleware.getName());
            try {
                IngressDTO ingressDTO = new IngressDTO();
                if (middlewareServiceNameIndex != null
                    && middlewareServiceNameIndex.getMiddlewareServiceNameSuffix() != null
                    && middlewareServiceNameIndex.getMiddlewareServiceNameSuffix().contains("readonly")) {
                    ingressDTO
                        .setName(middleware.getName() + "-readonly-nodeport-" + UUIDUtils.get8UUID().substring(0, 4));
                } else {
                    ingressDTO.setName(middleware.getName() + "-nodeport-" + UUIDUtils.get8UUID().substring(0, 4));
                }
                List<ServiceDTO> serviceDTOList = new ArrayList<>();
                ServiceDTO serviceDTO = new ServiceDTO();
                serviceDTO.setTargetPort(portDetailDTO.getTargetPort());
                serviceDTO.setServicePort(portDetailDTO.getPort());
                serviceDTO.setServiceName(servicePortDTO.getServiceName());
                serviceDTOList.add(serviceDTO);

                ingressDTO.setMiddlewareType(middleware.getType());
                ingressDTO.setServiceList(serviceDTOList);
                ingressDTO.setExposeType(MIDDLEWARE_EXPOSE_NODEPORT);
                ingressDTO.setProtocol("TCP");
                ingressService.create(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(),
                    ingressDTO);
                log.info("对外服务创建成功");
            } catch (Exception e) {
                log.error("对外服务创建失败", e);
            }
        }
    }

    /**
     * 检查中间件是否已存在
     *
     * @param namespace
     * @param middlewareName
     * @param cluster
     * @return
     */
    public boolean checkIfExist(String namespace, String middlewareName, MiddlewareClusterDTO cluster) {
        if (StringUtils.isEmpty(middlewareName)) {
            return false;
        }
        List<HelmListInfo> helms = helmChartService.listHelm(namespace, null, cluster);
        if (helms.stream().anyMatch(h -> middlewareName.equals(h.getName()))) {
            return true;
        }
        return false;
    }

    /**
     * 发布服务时,告警规则入库
     */
//    public void add2sql(Middleware middleware) {
//        QueryWrapper<BeanAlertRule> wrapper = new QueryWrapper<>();
//        wrapper.eq("chart_name", middleware.getType()).eq("chart_version", middleware.getChartVersion());
//        List<BeanAlertRule> beanAlertRules = beanAlertRuleMapper.selectList(wrapper);
//        if (beanAlertRules.isEmpty()) {
//            return;
//        }
//        JSONObject jsonObject = JSONObject.parseObject(beanAlertRules.get(0).getAlert());
//        if (!jsonObject.isEmpty()) {
//            PrometheusRule prometheusRule = JSONObject.toJavaObject(jsonObject, PrometheusRule.class);
//            for (PrometheusRuleGroups prometheusRuleGroups : prometheusRule.getSpec().getGroups()) {
//                if (prometheusRuleGroups.getRules().size() == 0 || prometheusRuleGroups.getRules() == null) {
//                    return;
//                }
//                prometheusRuleGroups.getRules().stream().forEach(rule -> {
//                    if (StringUtils.isNotEmpty(rule.getAlert())) {
//                        AlertRuleId alertRuleId = new AlertRuleId();
//                        alertRuleId.setAlert(rule.getAlert());
//                        alertRuleId.setExpr(rule.getExpr());
//                        alertRuleId.setSymbol(middlewareAlertsService.getSymbol(rule.getExpr()));
//                        alertRuleId.setThreshold(middlewareAlertsService.getThreshold(rule.getExpr()));
//                        alertRuleId.setTime(rule.getTime());
//                        rule.getLabels().put("middleware", middleware.getType());
//                        alertRuleId.setLabels(JSONUtil.toJsonStr(rule.getLabels()));
//                        alertRuleId.setAnnotations(JSONUtil.toJsonStr(rule.getAnnotations()));
//                        alertRuleId.setEnable("1");
//                        alertRuleId.setLay("service");
//                        alertRuleId.setClusterId(middleware.getClusterId());
//                        alertRuleId.setNamespace(middleware.getNamespace());
//                        alertRuleId.setMiddlewareName(middleware.getName());
//                        alertRuleId.setName(middleware.getClusterId());
//                        alertRuleId.setCreateTime(new Date());
//                        alertRuleId.setType(middleware.getType());
//                        alertRuleId.setDescription(rule.getAlert());
//                        String expr = rule.getAlert() + middlewareAlertsService.getSymbol(rule.getExpr())
//                            + middlewareAlertsService.getThreshold(rule.getExpr()) + "%" + "且"
//                            + alertRuleId.getAlertTime() + "分钟内触发" + alertRuleId.getAlertTimes() + "次";
//                        alertRuleId.setAlertExpr(expr);
//                        alertRuleIdMapper.insert(alertRuleId);
//                    }
//                });
//            }
//        }
//    }

    /**
     * 删除中间件时把对应的规则也删除掉
     *
     * @param middleware
     */
    public void removeSql(Middleware middleware) {
        QueryWrapper<AlertRuleId> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", middleware.getClusterId()).eq("namespace", middleware.getNamespace())
            .eq("middleware_name", middleware.getName());
        alertRuleIdMapper.delete(wrapper);
    }

    /**
     * 非自定义中间件镜像仓库信息转换
     */
    protected void convertRegistry(Middleware middleware, JSONObject values) {
        if (values != null && values.getJSONObject("image") != null) {
            JSONObject image = values.getJSONObject("image");
            String repository = image.getString("repository");
            middleware.setMirrorImage(repository);
            BeanImageRepository beanImageRepository = imageRepositoryService.findByAddress(repository);
            if (beanImageRepository != null) {
                middleware.setMirrorImageId(beanImageRepository.getId().toString());
            }
        }
    }

    public void convertExternal(JSONObject values, Middleware middleware, MiddlewareClusterDTO cluster) {
        // 开启对外访问
        JSONObject external = values.getJSONObject(EXTERNAL);
        external.put(ENABLE, TRUE);
        if (external.containsKey(USE_NODE_PORT)) {
            external.put(USE_NODE_PORT, FALSE);
        }
        for (IngressDTO ingressDTO : middleware.getIngresses()) {
            // 获取暴露ip地址
            String exposeIp = ingressService.getExposeIp(cluster, ingressDTO);
            // 指定分隔符号
            String splitTag = ingressDTO.getMiddlewareType().equals(MiddlewareTypeEnum.ROCKET_MQ.getType()) ? ";" : ",";
            // 初始化
            StringBuilder ipSb = new StringBuilder();
            StringBuilder svcSb = new StringBuilder();
            for (ServiceDTO serviceDTO : ingressDTO.getServiceList()) {
                if (serviceDTO.getServiceName().contains("-nameserver-proxy-svc")) {
                    continue;
                }
                ipSb.append(exposeIp).append(":").append(serviceDTO.getExposePort()).append(splitTag);
                svcSb.append(serviceDTO.getServiceName()).append(splitTag);
            }
            // 去除最后一位分隔符
            ipSb.deleteCharAt(ipSb.length() - 1);
            svcSb.deleteCharAt(svcSb.length() - 1);
            external.put(EXTERNAL_IP_ADDRESS, ipSb.toString());
            external.put(SVC_NAME_TAG, svcSb.toString());
        }
    }

    public void checkSvcCreated(Middleware middleware) {
        middleware.getIngresses().forEach(ingressDTO -> ingressDTO.getServiceList().forEach(serviceDTO -> {
            boolean again = true;
            while (again) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Service service = serviceWrapper.get(middleware.getClusterId(), middleware.getNamespace(),
                    serviceDTO.getServiceName());
                if (service != null) {
                    again = false;
                    String port = service.getSpec().getPorts().get(0).getPort().toString();
                    serviceDTO.setServicePort(port);
                    serviceDTO.setTargetPort(port);
                }
            }
        }));
    }

    /**
     * 设置容忍双活污点
     *
     * @param middleware
     * @param values
     */
    public void setActiveActiveToleration(Middleware middleware, JSONObject values) {
        if (!CollectionUtils.isEmpty(middleware.getTolerations())) {
            JSONArray jsonArray = K8sConvert.convertToleration2Json(middleware.getTolerations());
            if (values.getJSONObject("proxy") != null
                && MiddlewareTypeEnum.MYSQL.getType().equals(middleware.getType())) {
                JSONObject proxy = values.getJSONObject("proxy");
                proxy.put("tolerations", jsonArray);
            }
        }
    }

    /**
     * 设置values.yaml双活参数
     *
     * @param values
     * @param middleware
     */
    public void checkAndSetActiveActive(JSONObject values, Middleware middleware) {}

    /**
     * 获取双活注解
     * @param clusterId
     * @param namespace
     * @param type 中间件类型
     * @param middlewareName
     * @return
     */
    public ActiveAreaAnnotationDto getActiveAreaAnnotation(String clusterId, String namespace, String type, String middlewareName) {
        List<PodInfo> podInfoList = podService.listMiddlewarePodsWithArea(clusterId, namespace, middlewareName, type);
        List<String> zoneAPodList = new ArrayList<>();
        List<String> zoneBPodList = new ArrayList<>();
        for (PodInfo podInfo : podInfoList) {
            String podName = podInfo.getPodName();
            switch (podInfo.getZone()){
                case "zoneA":
                    zoneAPodList.add(podName);
                    break;
                case "zoneB":
                    zoneBPodList.add(podName);
                    break;
                default:
            }
        }
        Arrays.sort(zoneAPodList.toArray());
        Arrays.sort(zoneBPodList.toArray());
        String zoneAPod = zoneAPodList.get(0);
        String zoneBPod = zoneBPodList.get(0);
        Map<String,String> zoneAAnnotation = new HashMap<>();
        Map<String,String> zoneBAnnotation = new HashMap<>();
        zoneAAnnotation.put(ActiveAreaConstant.KEY_POD_SELECTOR, "[.status.conditions[]|select(.name==\"" + zoneAPod + "\")|.name]");
        zoneBAnnotation.put(ActiveAreaConstant.KEY_POD_SELECTOR, "[.status.conditions[]|select(.name==\"" + zoneBPod + "\")|.name]");
        return new ActiveAreaAnnotationDto(zoneAAnnotation, zoneBAnnotation);
    }

    /**
     * 设置双活参数
     *
     * @param values
     * @param activeActiveKey
     */
    public void setActiveActiveConfig(String activeActiveKey, JSONObject values) {
        values.put("podAntiAffinityTopologKey", zoneKey);
        values.put("podAntiAffinity", "hard");
    }


    public Double calculateCpuRequest(JSONObject values) {
        JSONObject resources = values.getJSONObject(RESOURCES);
        if (resources == null) {
            return 0.0;
        }
        String cpu = resources.getJSONObject("requests").getString(CPU);
        return ResourceCalculationUtil.getResourceValue(cpu, CPU, "") * getReplicas(values);
    }

    public Integer getReplicas(JSONObject values) {
        return values.getIntValue("replicas");
    }

    /**
     * 转换CustomVolume
     * @param customVolumes
     * @return
     */
    private JSONArray convertCustomVolumes(Map<String, CustomVolume> customVolumes) {
        JSONArray array = new JSONArray();
        customVolumes.forEach((name, customVolume) -> {
            JSONObject item = new JSONObject();
            item.put("name", name);
            item.put("mountPath", customVolume.getMountPath());
            item.put("storageClass", customVolume.getStorageClass());
            item.put("volumeSize", customVolume.getVolumeSize() + "Gi");
            item.put("hostPath", customVolume.getHostPath());
            JSONArray containers = new JSONArray();
            containers.addAll(customVolume.getTargetContainers());
            item.put("targetContainers", containers);
            array.add(item);
        });
        return array;
    }

    /**
     * 校验用户权限
     */
    public Boolean checkUserAuthority(String type){
        return roleAuthorityService.checkOps(null, type);
    }

    public List<MiddlewareQuota> getMiddlewareStorageClasses(String clusterId, Middleware middleware, JSONObject values) {
        Map<String, String> scAliasNameMap = storageService.listStorageMap(clusterId, true);
        Map<String, CustomVolume> customVolumes = middleware.getCustomVolumes();
        List<MiddlewareQuota> storageList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(customVolumes)) {
            Set<String> scSet = new HashSet<>();
            customVolumes.forEach((k, v) -> {
                scSet.add(v.getStorageClass());
            });
            scSet.forEach(scName -> {
                MiddlewareQuota middlewareQuota = new MiddlewareQuota();
                middlewareQuota.setStorageClassName(scName);
                middlewareQuota.setStorageClassAliasName(scAliasNameMap.get(scName));
                storageList.add(middlewareQuota);
            });
        } else {
            String scName = values.getString("storageClassName");
            MiddlewareQuota middlewareQuota = new MiddlewareQuota();
            middlewareQuota.setStorageClassName(scName);
            middlewareQuota.setStorageClassAliasName(scAliasNameMap.get(scName));
            storageList.add(middlewareQuota);
        }
        return storageList;
    }

    public void reboot(String clusterId, String namespace, String name, String type, String podType) {
        try {
            MiddlewareCR mw = middlewareCRService.getCR(clusterId, namespace, type, name);
            List<MiddlewareInfo> pods = mw.getStatus().getInclude().get(PODS);
            if(!CollectionUtils.isEmpty(pods)){
                pods.forEach(pod -> {
                    if (podType == null || pod.getType() == null || pod.getType().equalsIgnoreCase(podType)){
                        podService.restart(clusterId, namespace, name, type, pod.getName());
                    }
                });
            }
        } catch (Exception e){
            throw new BusinessException(ErrorMessage.MIDDLEWARE_REBOOT_FAILED);
        }
    }

    public String getCustomConfigRole(String podType){
        switch (podType.toLowerCase()) {
            case "master":
                return "major";
            default:
                return podType;
        }
    }

    public String getPodType(String customConfigRole){
        switch (customConfigRole.toLowerCase()) {
            case "major":
                return "master";
            default:
                return customConfigRole;
        }
    }

    public String changeConfigRoleToValueArg(String customConfigRole, boolean reserve) {
        if (!reserve) {
            switch (customConfigRole) {
                case "major" :
                    return "Master";
                default: return customConfigRole;
            }
        } else {
            switch (customConfigRole) {
                case "Master" :
                    return "major";
                default: return customConfigRole;
            }
        }
    }

    public Set<String> getCustomConfigRole(JSONObject values) {
        return null;
    }

    public Boolean withProxy(String clusterId, Middleware middleware){
        return false;
    }

    public Boolean withPgbouncer(String clusterId, Middleware middleware){
        return false;
    }

    public void convertDeployConfiguration(JSONObject configuration, List<AffinityDTO> nodeAffinity, List<String> tolerations){
        if (configuration == null){
            return;
        }

        if (!CollectionUtils.isEmpty(nodeAffinity)){
            JSONObject jsonNodeAffinity = K8sConvert.convertNodeAffinity2Json(nodeAffinity);
            JSONObject affinity = configuration.getJSONObject("affinity");
            if (affinity == null){
                affinity = new JSONObject();
            }
            affinity.put("nodeAffinity", jsonNodeAffinity);
            configuration.put("affinity", affinity);
        }

        if (!CollectionUtils.isEmpty(tolerations)){
            JSONArray jsonTolerations = K8sConvert.convertToleration2Json(tolerations);
            configuration.put("tolerations", jsonTolerations);
            StringBuffer sb = new StringBuffer();
            for (String toleration : tolerations) {
                sb.append(toleration).append(",");
            }
            configuration.put("tolerationAry", sb.substring(0, sb.length()));
        }
    }

    public List<AffinityDTO> convertDeployConfigAffinity(JSONObject configuration) {
        try {
            if (configuration != null && configuration.containsKey("affinity")) {
                JSONObject affinity = configuration.getJSONObject("affinity");
                if (affinity.containsKey("nodeAffinity")) {
                    JSONObject nodeAffinity = affinity.getJSONObject("nodeAffinity");
                    return K8sConvert.convertNodeAffinity(
                            JSONObject.parseObject(nodeAffinity.toJSONString(), NodeAffinity.class), AffinityDTO.class);
                }
            }
        } catch (Exception e){
            log.error("获取deploy 节点亲和配置失败", e);
        }
        return null;
    }

    public List<String> convertDeployConfigTolerations(JSONObject configuration){
        try {
            if (configuration.containsKey("tolerations")){
                JSONArray tolerations = configuration.getJSONArray("tolerations");
                return K8sConvert.convertTolerationToString(tolerations);
            }
        } catch (Exception e){
            log.error("获取deploy容忍配置失败", e);
        }
        return null;
    }

    protected void parseQuotaToString(MiddlewareQuota quota) {
        if (quota == null) {
            return;
        }
        //
        Pattern pattern = Pattern.compile("^[0-9]+$");
        if (StringUtils.isNotEmpty(quota.getCpu()) && pattern.matcher(quota.getCpu()).matches()) {
            quota.setCpu((int) ResourceCalculationUtil.getResourceValue(quota.getCpu(), CPU, "m", 0, RoundingMode.UP) + "m");
        }
        if (StringUtils.isNotEmpty(quota.getLimitCpu()) && pattern.matcher(quota.getLimitCpu()).matches()) {
            quota.setLimitCpu((int) ResourceCalculationUtil.getResourceValue(quota.getLimitCpu(), CPU, "m", 0, RoundingMode.UP) + "m");
        }
    }

}
