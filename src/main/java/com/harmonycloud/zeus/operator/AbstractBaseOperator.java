package com.harmonycloud.zeus.operator;

import static com.harmonycloud.caas.common.constants.NameConstant.*;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_EXPOSE_NODEPORT;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PERSISTENT_VOLUME_CLAIMS;
import static com.harmonycloud.caas.common.constants.registry.HelmChartConstant.CHART_YAML_NAME;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.zeus.bean.BeanCacheMiddleware;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.dao.BeanCacheMiddlewareMapper;
import com.harmonycloud.zeus.service.aspect.AspectService;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.integration.cluster.PvcWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.schedule.MiddlewareManageTask;
import com.harmonycloud.zeus.service.middleware.*;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareBackupServiceImpl;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.util.K8sConvert;
import io.fabric8.kubernetes.api.model.ConfigMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.ResourceUnitEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.AffinityDTO;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.harmonycloud.tool.collection.JsonUtils;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;

import io.fabric8.kubernetes.api.model.NodeAffinity;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 中间件通用处理
 */
@Slf4j
public abstract class AbstractBaseOperator {

    /**
     * 此处的注入，实际上是由各个OperatorImpl子类（如MysqlOperatorImpl）进行注入了，如果直接初始化当前类，会发现值为空
     */
    @Autowired
    protected ClusterService clusterService;
    @Autowired
    protected HelmChartService helmChartService;
    @Autowired
    private PvcWrapper pvcWrapper;
    @Autowired
    protected MiddlewareCRDService middlewareCRDService;
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
    private ConfigMapService configMapService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private ServiceService serviceService;
    @Autowired
    private MiddlewareBackupServiceImpl middlewareBackupService;
    @Autowired
    private AspectService aspectService;
    @Autowired
    private GrafanaService grafanaService;
    @Autowired
    protected CacheMiddlewareService cacheMiddlewareService;
    @Autowired
    protected ClusterMiddlewareInfoService clusterMiddlewareInfoService;

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
        Middleware mw = middlewareCRDService.simpleDetail(middleware.getClusterId(), middleware.getNamespace(),
                middleware.getType(), middleware.getName());
        if (mw == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middleware.getName(), ErrorMessage.NOT_EXIST);
        }
        return convertByHelmChart(mw, cluster);
    }

    public void create(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (cluster == null) {
            cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
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
        // deal with Charts.yaml file
        replaceChart(helmChart, values);
        // deal with dynamic
        aspectService.operation(cluster, middleware, middleware.getDynamicValues(), values);
        // map to yaml
        String newValuesYaml = yaml.dumpAsMap(values);
        helmChart.setValueYaml(newValuesYaml);
        // write to local file
        helmChartService.coverYamlFile(helmChart);
        // 3. helm package & install
        String tgzFilePath = helmChartService.packageChart(helmChart.getTarFileName(), middleware.getChartName(),
            middleware.getChartVersion());
        helmChartService.install(middleware, tgzFilePath, cluster);

        // 4. 创建对外访问
        if (!CollectionUtils.isEmpty(middleware.getIngresses())) {
            try {
                middleware.getIngresses().forEach(ingress -> ingressService.create(middleware.getClusterId(),
                        middleware.getNamespace(), middleware.getName(), ingress));
            } catch (Exception e) {
                log.error("集群：{}，命名空间：{}，中间件：{}，创建对外访问异常", middleware.getClusterId(), middleware.getNamespace(),
                        middleware.getName(), e);
                throw new BusinessException(ErrorMessage.MIDDLEWARE_SUCCESS_INGRESS_FAIL);
            }
        }
        //5. 修改prometheusRules添加集群
        updateAlerts(middleware);
    }

    public void recovery(Middleware middleware, MiddlewareClusterDTO cluster){
        BeanCacheMiddleware beanCacheMiddleware = cacheMiddlewareService.get(middleware);
        // 1. download and read helm chart from registry
        HelmChartFile helmChart =
                helmChartService.getHelmChartFromMysql(middleware.getChartName(), middleware.getChartVersion());
        // 2. deal with values.yaml andChart.yaml
        JSONObject values = JSONObject.parseObject(beanCacheMiddleware.getValues());
        Yaml yaml = new Yaml();
        helmChart.setValueYaml(yaml.dumpAsMap(values));
        helmChartService.coverYamlFile(helmChart);
        // 3. helm package & install
        String tgzFilePath = helmChartService.packageChart(helmChart.getTarFileName(), middleware.getChartName(),
                middleware.getChartVersion());
        helmChartService.install(middleware, tgzFilePath, cluster);
    }


    public void delete(Middleware middleware) {
        /*deletePvc(middleware);
        deleteIngress(middleware);
        deleteCustomConfigHistory(middleware);
        middlewareBackupService.deleteMiddlewareBackupInfo(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());*/
        // 获取集群
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
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
        beanCacheMiddleware.setValues(JSONObject.toJSONString(values));
        cacheMiddlewareService.insert(beanCacheMiddleware);
        // helm卸载需要放到最后，要不然一些资源的查询会404
        helmChartService.uninstall(middleware, cluster);
    }

    public void deleteStorage(Middleware middleware){
        deleteIngress(middleware);
        deleteCustomConfigHistory(middleware);
        middlewareBackupService.deleteMiddlewareBackupInfo(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
        //删除数据库记录
        cacheMiddlewareService.delete(middleware);
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
        // 没有修改，直接返回
        if (sb.length() == 0) {
            return;
        }
        // 去掉末尾的逗号
        sb.deleteCharAt(sb.length() - 1);
        // 更新helm
        helmChartService.upgrade(middleware, sb.toString(), cluster);
    }


    public MonitorDto monitor(Middleware middleware) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        List<BeanMiddlewareInfo> middlewareInfoList = middlewareInfoService.list(true);
        BeanMiddlewareInfo mwInfo = middlewareInfoList.stream()
            .collect(Collectors.toMap(
                beanMiddlewareInfo -> beanMiddlewareInfo.getChartName() + ":" + beanMiddlewareInfo.getChartVersion(),
                middlewareInfo -> middlewareInfo))
            .get(middleware.getType() + ":" + middleware.getChartVersion());
        if (mwInfo == null) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_NOT_EXIST);
        }
        if (StringUtils.isEmpty(mwInfo.getGrafanaId())){
            updateGrafanaId(mwInfo, middleware);
        }
        if (StringUtils.isEmpty(mwInfo.getGrafanaId())){
            throw new BusinessException(ErrorMessage.GRAFANA_ID_NOT_FOUND);
        }

        MiddlewareClusterMonitorInfo monitorInfo = cluster.getMonitor().getGrafana();
        if (monitorInfo == null
            || StringUtils.isAnyEmpty(monitorInfo.getProtocol(), monitorInfo.getHost(), monitorInfo.getPort())) {
            throw new BusinessException(ErrorMessage.CLUSTER_MONITOR_INFO_NOT_FOUND);
        }
        // 生成token
        if (StringUtils.isEmpty(monitorInfo.getToken()) && StringUtils.isNotEmpty(monitorInfo.getUsername())
            && StringUtils.isNotEmpty(monitorInfo.getPassword())) {
            grafanaService.setToken(monitorInfo);
            cluster.getMonitor().setGrafana(monitorInfo);
            clusterService.update(cluster);
        }

        MonitorDto monitorDto = new MonitorDto();
        monitorDto.setUrl(monitorInfo.getAddress() + "/d/" + mwInfo.getGrafanaId() + "/" + middleware.getType()
            + "?var-namespace=" + middleware.getNamespace() + "&var-service=" + middleware.getName());
        monitorDto.setAuthorization("Bearer " + monitorInfo.getToken());
        return monitorDto;
    }

    public void updateGrafanaId(BeanMiddlewareInfo mwInfo, Middleware middleware) {
        HelmChartFile helm = helmChartService.getHelmChart(middleware.getClusterId(),
            middleware.getNamespace(), middleware.getName(), middleware.getType());
        String alias;
        if (!CollectionUtils.isEmpty(helm.getDependency())){
            alias = helm.getDependency().get("alias");
        }else {
            alias = mwInfo.getChartName() + "-operator";
        }
        List<HelmListInfo> helmInfos =
            helmChartService.listHelm("", alias, clusterService.findById(middleware.getClusterId()));
        if (!CollectionUtils.isEmpty(helmInfos)) {
            // 获取configmap
            HelmListInfo helmInfo = helmInfos.get(0);
            ConfigMap configMap =
                configMapService.get(middleware.getClusterId(), helmInfo.getNamespace(), alias + "-dashboard");
            if (!ObjectUtils.isEmpty(configMap)) {
                for (String key : configMap.getData().keySet()) {
                    JSONObject object = JSONObject.parseObject(configMap.getData().get(key));
                    mwInfo.setGrafanaId(object.get("uid").toString());
                    middlewareInfoService.update(mwInfo);
                }
            }
        }
    }

    protected void deletePvc(Middleware middleware) {
        // query middleware cr
        MiddlewareCRD mw = middlewareCRDService.getCR(middleware.getClusterId(), middleware.getNamespace(),
            middleware.getType(), middleware.getName());
        if (mw == null || mw.getStatus() == null || mw.getStatus().getInclude() == null) {
            return;
        }
        List<MiddlewareInfo> pvcs = mw.getStatus().getInclude().get(PERSISTENT_VOLUME_CLAIMS);
        if (!CollectionUtils.isEmpty(pvcs)) {
            pvcs.forEach(pvc -> pvcWrapper.delete(middleware.getClusterId(), middleware.getNamespace(), pvc.getName()));
        }
    }

    protected void deleteIngress(Middleware mw) {
        List<IngressDTO> ingressList;
        try {
            ingressList = ingressService.get(mw.getClusterId(), mw.getNamespace(), mw.getType(), mw.getName());
        } catch (Exception e) {
            log.error("集群：{}，命名空间：{}，中间件：{}/{}，删除对外访问时查询列表异常", mw.getClusterId(), mw.getNamespace(), mw.getType(),
                mw.getName(), e);
            return;
        }
        ingressList.forEach(ing -> {
            try {
                ingressService.delete(mw.getClusterId(), mw.getNamespace(), mw.getName(), ing.getName(), ing);
            } catch (Exception e) {
                log.error("集群：{}，命名空间：{}，中间件：{}/{}，对外服务{}/{}，删除对外访问异常", mw.getClusterId(), mw.getNamespace(),
                    mw.getType(), mw.getName(), ing.getExposeType(), ing.getName(), e);
            }
        });
    }

    public void deleteCustomConfigHistory(Middleware mw) {
        try {
            middlewareCustomConfigService.deleteHistory(mw.getClusterId(), mw.getNamespace(), mw.getName());
        } catch (Exception e) {
            log.error("集群{} 分区{} 中间件{} 自定义参数修改历史删除失败: {}", mw.getClusterId(), mw.getNamespace(), mw.getName(), e);
        }
    }

    public void deleteMiddlewareBackupInfo(Middleware mw){

    }

    public void switchMiddleware(Middleware middleware) {

    }

    /**
     * 从helm chart转回middleware
     */
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (StringUtils.isEmpty(middleware.getClusterId())){
            middleware.setClusterId(cluster.getId());
        }
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        return middleware;
    }

    /**
     * 根据helm chart的values转换
     */
    protected void convertCommonByHelmChart(Middleware middleware, JSONObject values) {
        if (values != null) {
            middleware.setAliasName(values.getString("aliasName"))
                    .setDescription(values.getString("middleware-desc"))
                    .setLabels(values.getString("middleware-label"))
                    .setVersion(values.getString("version"))
                    .setMode(values.getString(MODE));

            if (StringUtils.isNotEmpty(values.getString("chart-version"))){
                middleware.setChartVersion(values.getString("chart-version"));
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

            //动态参数
            if (values.containsKey("custom")){
                convertDynamicValues(middleware, values);
            }

            // node affinity
            if (JsonUtils.isJsonObject(values.getString("nodeAffinity"))) {
                JSONObject nodeAffinity = values.getJSONObject("nodeAffinity");
                if (!CollectionUtils.isEmpty(nodeAffinity)) {
                    List<AffinityDTO> dto = K8sConvert.convertNodeAffinity(
                        JSONObject.parseObject(nodeAffinity.toJSONString(), NodeAffinity.class), AffinityDTO.class);
                    middleware.setNodeAffinity(dto);
                }
            }
        } else {
            middleware.setAliasName(middleware.getName());
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
        quota.setCpu(requests.getString(CPU)).setMemory(requests.getString(MEMORY))
            .setLimitCpu(limits.getString(CPU)).setLimitMemory(limits.getString(MEMORY));
    }

    protected void convertStoragesByHelmChart(Middleware middleware, String quotaKey, JSONObject values) {
        if (StringUtils.isBlank(quotaKey) || values == null) {
            return;
        }
        MiddlewareQuota quota = checkMiddlewareQuota(middleware, quotaKey);
        quota.setStorageClassName(values.getString("storageClassName"))
            .setStorageClassQuota(values.getString("storageSize"));
    }


    public MiddlewareQuota checkMiddlewareQuota(Middleware middleware, String quotaKey){
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
    }

    public void updatePreCheck(Middleware middleware, MiddlewareClusterDTO cluster) {
        // throws if it exists
        List<HelmListInfo> helms = helmChartService.listHelm(middleware.getNamespace(), null, cluster);
        if (helms.stream().noneMatch(h -> middleware.getName().equals(h.getName()))) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middleware.getName(), ErrorMessage.NOT_EXIST);
        }
    }

    public void convertDynamicValues(Middleware middleware, JSONObject values) {
        HelmChartFile helm = helmChartService.getHelmChartFromMysql(middleware.getType(), middleware.getChartVersion());
        QuestionYaml questionYaml = helmChartService.getQuestionYaml(helm);
        Map<String, String> dynamicValues = new HashMap<>();
        //解析question.yaml
        convertQuestions(questionYaml.getQuestions(), dynamicValues, values);
        middleware.setDynamicValues(dynamicValues);
        //设置动态tab
        middleware.setCapabilities(questionYaml.getCapabilities());
        //获取cpu和memory
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
            if (question.getDetail() != null && question.getDetail() && !"nodeAffinity".equals(question.getType())) {
                String value = getValuesByVariable(question.getVariable(), values);
                dynamicValues.put(question.getLabel(), value);
                if (StringUtils.isNotEmpty(question.getShowSubQuestionIf())
                    && value.equals(question.getShowSubQuestionIf())) {
                    convertQuestions(question.getSubQuestions(), dynamicValues, values);
                }
            }
        });
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
        //标记为自定义中间件
        values.put("custom", true);
        //记录自定义tab页
        if (CollectionUtils.isEmpty(middleware.getCapabilities())) {
            values.put("dynamicTabs", new ArrayList<>(Collections.singletonList("basic")));
        } else {
            values.put("dynamicTabs", middleware.getCapabilities());
        }
    }

    protected void replaceSimplyCommonValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values){
        values.put("version", middleware.getVersion());
        values.put("nameOverride", middleware.getName());
        values.put("fullnameOverride", middleware.getName());
        values.put("aliasName",
                StringUtils.isBlank(middleware.getAliasName()) ? middleware.getName() : middleware.getAliasName());
        values.put("middleware-desc", middleware.getDescription());
        values.put("middleware-label", middleware.getLabels());
        values.put("chart-version", middleware.getChartVersion());

        // node affinity
        if (!CollectionUtils.isEmpty(middleware.getNodeAffinity())) {
            // convert to k8s model
            JSONObject nodeAffinity = K8sConvert.convertNodeAffinity2Json(
                    middleware.getNodeAffinity().get(0).getLabel(), middleware.getNodeAffinity().get(0).isRequired());
            if (nodeAffinity != null) {
                values.put("nodeAffinity", nodeAffinity);
            }
        } else {
            values.put("nodeAffinity", new JSONObject());
        }

        // log
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

        // annotations
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
        values.put(MODE, middleware.getMode());
        // image
        JSONObject image = values.getJSONObject("image");
        Registry registry = cluster.getRegistry();
        image.put("repository", registry.getRegistryAddress() + "/"
                + (StringUtils.isBlank(registry.getImageRepo()) ? registry.getChartRepo() : registry.getImageRepo()));
    }

    /**
     * 处理动态表单
     */
    protected void replaceDynamicValues(Middleware middleware, JSONObject values) {
        Map<String, String> dynamicValues = middleware.getDynamicValues();
        for (String key : dynamicValues.keySet()) {
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
                values.put(key, dynamicValues.get(key));
            }
        }
    }

    private void replaceDynamicValuesContent(Middleware middleware, MiddlewareClusterDTO cluster) {
        for (String key : middleware.getDynamicValues().keySet()) {
            if (middleware.getDynamicValues().get(key).contains("${address}")) {
                middleware.getDynamicValues().put(key,
                    middleware.getDynamicValues().get(key).replace("${address}", cluster.getRegistry().getAddress()));
            }
            if (middleware.getDynamicValues().get(key).contains("${port}")) {
                middleware.getDynamicValues().put(key, middleware.getDynamicValues().get(key).replace("${port}",
                    String.valueOf(cluster.getRegistry().getPort())));
            }
            if (middleware.getDynamicValues().get(key).contains("${repository}")) {
                middleware.getDynamicValues().put(key, middleware.getDynamicValues().get(key).replace("${repository}",
                    cluster.getRegistry().getImageRepo()));
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
        values.put("storageClassName", quota.getStorageClassName());
        values.put("storageSize", quota.getStorageClassQuota() + "Gi");
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
                if (dependence.getString("alias").contains("operator")) {
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
     * @param rate        比例
     * @param unitM       m的单位
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
                if (!CollectionUtils.isEmpty(rule.getLabels())){
                    rule.getLabels().put("clusterId", middleware.getClusterId());
                }
            }));
            prometheusRuleService.update(middleware.getClusterId(), prometheusRule);
        } catch (Exception e){
            log.error("集群{} 分区{} 中间件{}， 告警规则标签添加集群失败", middleware.getClusterId(), middleware.getNamespace(),
                middleware.getName());
        }
        
    }

    /**
     * 尝试创建对外服务，当实例状态为Running时才创建对外服务
     * @param middleware 中间件信息
     * @param middlewareServiceNameIndex 服务名称
     */
    public void tryCreateOpenService(Middleware middleware, MiddlewareServiceNameIndex middlewareServiceNameIndex, Boolean needRunningMiddleware) {
        boolean success = false;
        for (int i = 0; i < 600 && !success; i++) {
            Middleware detail = middlewareService.detail(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType());
            log.info("为实例：{}创建对外服务：状态：{},已用时：{}s", detail.getName(), detail.getStatus(), i);
            if (detail != null) {
                if (needRunningMiddleware) {
                    if (detail.getStatus() != null && "Running".equals(detail.getStatus())) {
                        createOpenService(middleware, middlewareServiceNameIndex);
                        success = true;
                    }
                } else {
                    createOpenService(middleware, middlewareServiceNameIndex);
                    success = true;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建NodePort服务
     * @param middleware 中间件信息
     * @param middlewareServiceNameIndex 中间件服务名称
     */
    public void createOpenService(Middleware middleware, MiddlewareServiceNameIndex middlewareServiceNameIndex) {
        //1.获取所有对外服务，判断指定类型的服务是否已创建，如果已创建则直接返回
        List<IngressDTO> ingressDTOS = ingressService.get(middleware.getClusterId(), middleware.getNamespace(),
                middleware.getType(), middleware.getName());
        if (!CollectionUtils.isEmpty(ingressDTOS)) {
            String finalServiceName = middlewareServiceNameIndex.getNodePortServiceName();
            List<IngressDTO> ingressDTOList = ingressDTOS.stream().filter(ingressDTO -> (
                    ingressDTO.getName().contains(finalServiceName) && ingressDTO.getExposeType().equals(MIDDLEWARE_EXPOSE_NODEPORT))
            ).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(ingressDTOList)) {
                return;
            }
        }

        List<ServicePortDTO> servicePortDTOS = serviceService.list(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType());
        String finalMiddlewareServiceNameSuffix = middlewareServiceNameIndex.getMiddlewareServiceNameSuffix();
        List<ServicePortDTO> serviceList = servicePortDTOS.stream().filter(servicePortDTO -> servicePortDTO.getServiceName().endsWith(finalMiddlewareServiceNameSuffix)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(serviceList)) {
            ServicePortDTO servicePortDTO = serviceList.get(0);
            PortDetailDTO portDetailDTO = servicePortDTO.getPortDetailDtoList().get(0);
            //2.将服务通过NodePort暴露为对外服务
            boolean successCreateService = false;
            int servicePort = 31000;
            while (!successCreateService) {
                log.info("开始创建对外服务,clusterId={},namespace={},middlewareName={},port={}",
                        middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), servicePort);
                try {
                    IngressDTO ingressDTO = new IngressDTO();
                    List<ServiceDTO> serviceDTOList = new ArrayList<>();
                    ServiceDTO serviceDTO = new ServiceDTO();
                    serviceDTO.setExposePort(String.valueOf(servicePort));
                    serviceDTO.setTargetPort(portDetailDTO.getTargetPort());
                    serviceDTO.setServicePort(portDetailDTO.getPort());
                    serviceDTO.setServiceName(servicePortDTO.getServiceName());
                    serviceDTOList.add(serviceDTO);

                    ingressDTO.setMiddlewareType(middleware.getType());
                    ingressDTO.setServiceList(serviceDTOList);
                    ingressDTO.setExposeType(MIDDLEWARE_EXPOSE_NODEPORT);
                    ingressDTO.setProtocol("TCP");
                    ingressService.create(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), ingressDTO);
                    successCreateService = true;
                    log.info("对外服务创建成功");
                } catch (Exception e) {
                    servicePort++;
                    log.error("对外服务创建失败，尝试端口：{}", servicePort);
                    successCreateService = false;
                }
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
}
