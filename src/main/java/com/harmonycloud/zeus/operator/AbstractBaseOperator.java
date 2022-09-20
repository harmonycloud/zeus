package com.harmonycloud.zeus.operator;

import static com.harmonycloud.caas.common.constants.CommonConstant.FALSE;
import static com.harmonycloud.caas.common.constants.CommonConstant.TRUE;
import static com.harmonycloud.caas.common.constants.NameConstant.*;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.USE_NODE_PORT;
import static com.harmonycloud.caas.common.constants.registry.HelmChartConstant.CHART_YAML_NAME;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.enums.middleware.StorageClassProvisionerEnum;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.StorageDto;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.bean.BeanAlertRule;
import com.harmonycloud.zeus.bean.AlertRuleId;
import com.harmonycloud.zeus.dao.BeanAlertRuleMapper;
import com.harmonycloud.zeus.dao.AlertRuleIdMapper;
import com.harmonycloud.zeus.dao.BeanMiddlewareInfoMapper;
import com.harmonycloud.zeus.integration.cluster.ServiceWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRuleGroups;
import com.harmonycloud.zeus.bean.BeanCacheMiddleware;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.service.aspect.AspectService;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.integration.cluster.PvcWrapper;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.schedule.MiddlewareManageTask;
import com.harmonycloud.zeus.service.middleware.*;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareAlertsServiceImpl;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareBackupServiceImpl;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.util.K8sConvert;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Service;
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
    protected PvcWrapper pvcWrapper;
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
    private MiddlewareService middlewareService;
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
    private IngressComponentService ingressComponentService;
    @Autowired
    private BeanMiddlewareInfoMapper middlewareInfoMapper;
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
        //5. 修改prometheusRules添加集群
        updateAlerts(middleware);
        add2sql(middleware);
    }


    public void delete(Middleware middleware) {
        // 获取集群
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        //check exist
        List<HelmListInfo> list = helmChartService.listHelm(middleware.getNamespace(), middleware.getName(), cluster);
        if (CollectionUtils.isEmpty(list)){
            throw new BusinessException(ErrorMessage.MIDDLEWARE_NOT_EXIST);
        }
        ingressService.delete(middleware.getClusterId(), middleware.getNamespace(),
                middleware.getType(), middleware.getName());
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
        } catch (Exception e){
            log.error("获取中间件pvc失败", e);
        }
        
        beanCacheMiddleware.setValuesYaml(JSONObject.toJSONString(values));
        cacheMiddlewareService.insertIfNotPresent(beanCacheMiddleware);
        // helm卸载需要放到最后，要不然一些资源的查询会404
        helmChartService.uninstall(middleware, cluster);
    }

    public void deleteStorage(Middleware middleware){
        BeanCacheMiddleware beanCacheMiddleware = cacheMiddlewareService.get(middleware);
        deletePvc(beanCacheMiddleware);
        deleteCustomConfigHistory(middleware);
        middlewareBackupService.deleteMiddlewareBackupInfo(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
        removeSql(middleware);
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
        // 更新通用字段
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
     * 更新通用字段
     * @param sb
     * @param middleware
     */
    protected void updateCommonValues(StringBuilder sb, Middleware middleware){
        // 备注
        if (middleware.getDescription() != null) {
            sb.append("middleware-desc=").append(middleware.getDescription()).append(",");
        }

        // 日志开关
        if (null != middleware.getFilelogEnabled()) {
            sb.append("logging.collection.filelog.enabled=").append(middleware.getFilelogEnabled()).append(",");
        }
        if (null != middleware.getStdoutEnabled()) {
            sb.append("logging.collection.stdout.enabled=").append(middleware.getStdoutEnabled()).append(",");
        }
    }

    protected void deletePvc(BeanCacheMiddleware beanCacheMiddleware) {
        if (StringUtils.isNotEmpty(beanCacheMiddleware.getPvc())) {
            List<String> pvcList = Arrays.asList(beanCacheMiddleware.getPvc().split(","));
            if (!CollectionUtils.isEmpty(pvcList)) {
                pvcList.forEach(pvc -> pvcWrapper.delete(beanCacheMiddleware.getClusterId(),
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
            pvcWrapper.update(middleware.getClusterId(), middleware.getNamespace(), pvc);
        }
    }

    public void updateStorage(Middleware middleware) {
        // 获取pvc
        List<String> pvcNameList = middlewareCRService.getPvc(middleware.getClusterId(), middleware.getNamespace(),
                middleware.getType(), middleware.getName());
        // 更新pvc内容
        List<PersistentVolumeClaim> pvcList = new ArrayList<>();
        for (String pvcName : pvcNameList) {
            pvcList.add(pvcWrapper.get(middleware.getClusterId(), middleware.getNamespace(), pvcName));
        }
        updatePvc(middleware, pvcList);

        // 更新values.yaml
        StringBuilder sb = new StringBuilder();
        if (middleware.getType().equals(MiddlewareTypeEnum.ELASTIC_SEARCH.getType())){
            for (String key : middleware.getQuota().keySet()){
                sb.append("storage.").append(key).append("Size").append("=").append(middleware.getQuota().get(key).getStorageClassQuota()).append(",");
            }
        }else {
            sb.append("storageSize=").append(middleware.getQuota().get(middleware.getType()).getStorageClassQuota()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        helmChartService.upgrade(middleware, sb.toString(), clusterService.findById(middleware.getClusterId()));
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
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        //setImagePath(middleware, values);
        return middleware;
    }

    /**
     * 设置中间件图片
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
            middleware.setAliasName(values.getString("aliasName"))
                    .setDescription(values.getString("middleware-desc"))
                    .setLabels(values.getString("middleware-label"))
                    .setVersion(values.getString("version"))
                    .setMode(values.getString(MODE));
            // 获取chart-version
            if (StringUtils.isNotEmpty(values.getString("chart-version"))) {
                middleware.setChartVersion(values.getString("chart-version"));
            } else {
                BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoService.list(true).stream()
                    .filter(info -> info.getChartName().equals(middleware.getType())).collect(Collectors.toList())
                    .get(0);
                middleware.setChartVersion(beanMiddlewareInfo.getChartVersion());
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
            // toleration
            if (values.getString("tolerationAry") != null) {
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

            // 设置服务备份状态
            middleware.setHasConfigBackup(middlewareBackupService.checkIfAlreadyBackup(middleware.getClusterId(),middleware.getNamespace(),middleware.getType(),middleware.getName()));

            // 设置中间件图片
            //setImagePath(middleware, values);
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
        quota.setIsLvmStorage(storageClassService.checkLVMStorage(middleware.getClusterId(), middleware.getNamespace(),
            values.getString("storageClassName")));

        // 获取存储中文名
        try {
            StorageDto storageDto = storageService.get(middleware.getClusterId(), values.getString("storageClassName"), false);
            quota.setStorageClassAliasName(storageDto.getAliasName());
        } catch (Exception e){
            log.error("中间件{}, 获取存储中文名失败", middleware.getName());
        }
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
        // 数据仍未清清除
        if (!ObjectUtils.isEmpty(cacheMiddlewareService.get(middleware))){
            throw new BusinessException(ErrorMessage.SAME_NAME_MIDDLEWARE_STORAGE_EXIST);
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
            if (question.getDetail() != null && question.getDetail() && !"nodeAffinity".equals(question.getType()) && !"tolerations".equals(question.getType())) {
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

        //labels
        replaceLabels(middleware, values);
        // node affinity
        replaceNodeAffinity(middleware, values);
        // log
        replaceLog(middleware, values);
        //toleration
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

    protected void replaceNodeAffinity(Middleware middleware, JSONObject values){
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
    }

    protected void replaceToleration(Middleware middleware, JSONObject values){
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

    protected void replaceAnnotations(Middleware middleware, JSONObject values){
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
        if (StringUtils.isNotEmpty(middleware.getMode())){
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
            replaceReadWriteProxyValues(middleware.getReadWriteProxy(), values);
        }
    }

    /**
     * 处理读写分离
     */
    protected void replaceReadWriteProxyValues(ReadWriteProxy readWriteProxy, JSONObject values){

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
                middleware.getDynamicValues().put(key,
                    middleware.getDynamicValues().get(key).toString().replace("${address}", cluster.getRegistry().getAddress()));
            }
            if (middleware.getDynamicValues().get(key).toString().contains("${port}")) {
                middleware.getDynamicValues().put(key, middleware.getDynamicValues().get(key).toString().replace("${port}",
                    String.valueOf(cluster.getRegistry().getPort())));
            }
            if (middleware.getDynamicValues().get(key).toString().contains("${repository}")) {
                middleware.getDynamicValues().put(key, middleware.getDynamicValues().get(key).toString().replace("${repository}",
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
                    rule.getLabels().put("namespace",middleware.getNamespace());
                    rule.getLabels().put("service",middleware.getName());
                    rule.getLabels().put("middleware",middleware.getType());
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
        for (int i = 0; i < (60 * 10 * 60) && !success; i++) {
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
            log.info("开始创建对外服务,clusterId={},namespace={},middlewareName={}",
                    middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
            try {
                IngressDTO ingressDTO = new IngressDTO();
                if (middlewareServiceNameIndex != null && middlewareServiceNameIndex.getMiddlewareServiceNameSuffix() != null && middlewareServiceNameIndex.getMiddlewareServiceNameSuffix().contains("readonly")) {
                    ingressDTO.setName(middleware.getName() + "-readonly-nodeport-" + UUIDUtils.get8UUID().substring(0, 4));
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
                ingressService.create(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), ingressDTO);
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
     *发布服务时,告警规则入库
     */
    public void add2sql(Middleware middleware){
        QueryWrapper<BeanAlertRule> wrapper = new QueryWrapper<>();
        wrapper.eq("chart_name",middleware.getType()).eq("chart_version",middleware.getChartVersion());
        List<BeanAlertRule> beanAlertRules = beanAlertRuleMapper.selectList(wrapper);
        if (beanAlertRules.isEmpty()) {
            return;
        }
        JSONObject jsonObject = JSONObject.parseObject(beanAlertRules.get(0).getAlert());
        if (!jsonObject.isEmpty()) {
            PrometheusRule prometheusRule = JSONObject.toJavaObject(jsonObject,PrometheusRule.class);
            for (PrometheusRuleGroups prometheusRuleGroups : prometheusRule.getSpec().getGroups()) {
                if (prometheusRuleGroups.getRules().size() == 0 || prometheusRuleGroups.getRules() == null) {
                    return;
                }
                prometheusRuleGroups.getRules().stream().forEach(rule -> {
                    if (StringUtils.isNotEmpty(rule.getAlert())) {
                        AlertRuleId alertRuleId = new AlertRuleId();
                        alertRuleId.setAlert(rule.getAlert());
                        alertRuleId.setExpr(rule.getExpr());
                        alertRuleId.setSymbol(middlewareAlertsService.getSymbol(rule.getExpr()));
                        alertRuleId.setThreshold(middlewareAlertsService.getThreshold(rule.getExpr()));
                        alertRuleId.setTime(rule.getTime());
                        rule.getLabels().put("middleware",middleware.getType());
                        alertRuleId.setLabels(JSONUtil.toJsonStr(rule.getLabels()));
                        alertRuleId.setAnnotations(JSONUtil.toJsonStr(rule.getAnnotations()));
                        alertRuleId.setEnable("1");
                        alertRuleId.setLay("service");
                        alertRuleId.setClusterId(middleware.getClusterId());
                        alertRuleId.setNamespace(middleware.getNamespace());
                        alertRuleId.setMiddlewareName(middleware.getName());
                        alertRuleId.setName(middleware.getClusterId());
                        alertRuleId.setCreateTime(new Date());
                        alertRuleId.setType(middleware.getType());
                        alertRuleId.setDescription(rule.getAlert());
                        String expr = rule.getAlert() + middlewareAlertsService.getSymbol(rule.getExpr())
                                + middlewareAlertsService.getThreshold(rule.getExpr()) + "%"  + "且" + alertRuleId.getAlertTime()
                                + "分钟内触发" + alertRuleId.getAlertTimes() + "次";
                        alertRuleId.setAlertExpr(expr);
                        alertRuleIdMapper.insert(alertRuleId);
                    }
                });
            }
        }
    }

    /**
     * 删除中间件时把对应的规则也删除掉
     * @param middleware
     */
    public void removeSql(Middleware middleware) {
        QueryWrapper<AlertRuleId> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",middleware.getClusterId()).eq("namespace",middleware.getNamespace()).eq("middleware_name",middleware.getName());
        alertRuleIdMapper.delete(wrapper);
    }

    /**
     * 非自定义中间件镜像仓库信息转换
     */
    protected void convertRegistry(Middleware middleware, MiddlewareClusterDTO middlewareClusterDTO) {
        if (!ObjectUtils.isEmpty(middlewareClusterDTO.getRegistry())) {
            Registry registry = middlewareClusterDTO.getRegistry();
            String path = registry.getAddress() + ":" + registry.getPort() + "/" + registry.getChartRepo();
            middleware.setMirrorImage(path);
        }
    }

    public void convertExternal(JSONObject values, Middleware middleware, MiddlewareClusterDTO cluster){
        // 开启对外访问
        JSONObject external = values.getJSONObject(EXTERNAL);
        external.put(ENABLE, TRUE);
        if (external.containsKey(USE_NODE_PORT)) {
            external.put(USE_NODE_PORT, FALSE);
        }
        for (IngressDTO ingressDTO : middleware.getIngresses()){
            // 获取暴露ip地址
            String exposeIp = ingressService.getExposeIp(cluster, ingressDTO);
            // 指定分隔符号
            String splitTag = ingressDTO.getMiddlewareType().equals(MiddlewareTypeEnum.ROCKET_MQ.getType()) ? ";" : ",";
            // 初始化
            StringBuilder ipSb = new StringBuilder();
            StringBuilder svcSb = new StringBuilder();
            for (ServiceDTO serviceDTO : ingressDTO.getServiceList()) {
                if (serviceDTO.getServiceName().contains("-nameserver-proxy-svc")){
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
     * @param middleware
     * @param values
     */
    public void setActiveActiveToleration(Middleware middleware, JSONObject values){
        middleware.getTolerations();
        String activeActiveToleration = "harm.cn/type=active-active:NoSchedule";
        if (!CollectionUtils.isEmpty(middleware.getTolerations()) && !middleware.getTolerations().contains(activeActiveToleration)) {
            middleware.getTolerations().add(activeActiveToleration);
        } else {
            middleware.setTolerations(new ArrayList<>());
            middleware.getTolerations().add(activeActiveToleration);
        }
        JSONArray jsonArray = K8sConvert.convertToleration2Json(middleware.getTolerations());
        values.put("tolerations", jsonArray);
        if (values.getJSONObject("proxy") != null && MiddlewareTypeEnum.MYSQL.getType().equals(middleware.getType())) {
            JSONObject proxy = values.getJSONObject("proxy");
            proxy.put("tolerations", jsonArray);
        }
        StringBuilder sbf = new StringBuilder();
        for (String toleration : middleware.getTolerations()) {
            sbf.append(toleration).append(",");
        }
        values.put("tolerationAry", sbf.substring(0, sbf.length()));
    }

    /**
     * @description 过滤掉双活主机容忍和主机亲和
     * @author  liyinlong
     * @since 2022/8/31 3:13 下午
     * @param middleware
     */
    public void filterActiveActiveToleration(Middleware middleware) {
        if (!CollectionUtils.isEmpty(middleware.getTolerations())) {
            List<String> tolerations = middleware.getTolerations().stream().filter(item -> !item.contains("active-active")).collect(Collectors.toList());
            middleware.setTolerations(tolerations);
        }
        if (!CollectionUtils.isEmpty(middleware.getNodeAffinity())) {
            List<AffinityDTO> affinityDTOList = middleware.getNodeAffinity().stream().filter(item -> !item.getLabel().contains("zone!=zoneC")).collect(Collectors.toList());
            middleware.setNodeAffinity(affinityDTOList);
        }
    }

    /**
     * 设置values.yaml双活参数
     * @param values
     * @param middleware
     */
    public void checkAndSetActiveActive(JSONObject values, Middleware middleware) {
    }

    /**
     * 设置双活参数
     * @param values
     * @param activeActiveKey
     */
    public void setActiveActiveConfig(String activeActiveKey, JSONObject values) {
        values.put("podAntiAffinityTopologKey", "zone");
        values.put("podAntiAffinity", "soft");
        AffinityDTO affinityDTO = new AffinityDTO();
        affinityDTO.setLabel("zone=zoneC");
        affinityDTO.setRequired(true);
        JSONObject nodeAffinity = K8sConvert.convertNodeAffinity2Json(affinityDTO, "NotIn");
        if (nodeAffinity != null) {
            if (!StringUtils.isEmpty(activeActiveKey)) {
                JSONObject activeKey = values.getJSONObject(activeActiveKey);
                activeKey.put("nodeAffinity", nodeAffinity);
            } else {
                values.put("nodeAffinity", nodeAffinity);
            }
        }
    }

}
