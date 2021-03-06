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
import com.harmonycloud.caas.common.enums.Protocol;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.enums.middleware.StorageClassProvisionerEnum;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.StorageDto;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.bean.BeanAlertRule;
import com.harmonycloud.zeus.bean.MiddlewareAlertInfo;
import com.harmonycloud.zeus.dao.BeanAlertRuleMapper;
import com.harmonycloud.zeus.dao.MiddlewareAlertInfoMapper;
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
 * ?????????????????????
 */
@Slf4j
public abstract class AbstractBaseOperator {

    /**
     * ???????????????????????????????????????OperatorImpl????????????MysqlOperatorImpl????????????????????????????????????????????????????????????????????????
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
    private MiddlewareBackupServiceImpl middlewareBackupService;
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
    private MiddlewareAlertInfoMapper middlewareAlertInfoMapper;
    @Autowired
    private MiddlewareAlertsServiceImpl middlewareAlertsService;
    @Autowired
    private ServiceWrapper serviceWrapper;
    @Autowired
    protected StorageService storageService;
    @Autowired
    protected NamespaceService namespaceService;

    /**
     * ????????????????????????
     */
    protected abstract boolean support(Middleware middleware);

    /**
     * ?????????????????????
     */
    public List<Middleware> list(Middleware middleware, String keyword) {
        return null;
    }

    /**
     * ?????????????????????
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

        // 4. ??????????????????
        ThreadPoolExecutorFactory.executor.execute(() -> {
            if (middleware.getHostNetwork() && !CollectionUtils.isEmpty(middleware.getIngresses())) {
                try {
                    // ??????svc???????????????
                    checkSvcCreated(middleware);
                    middleware.getIngresses().forEach(ingress -> ingressService.create(middleware.getClusterId(),
                        middleware.getNamespace(), middleware.getName(), ingress));
                } catch (Exception e) {
                    log.error("?????????{}??????????????????{}???????????????{}???????????????????????????", middleware.getClusterId(), middleware.getNamespace(),
                        middleware.getName(), e);
                }
            }
        });
        //5. ??????prometheusRules????????????
        updateAlerts(middleware);
        add2sql(middleware);
    }


    public void delete(Middleware middleware) {
        // ????????????
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        //check exist
        List<HelmListInfo> list = helmChartService.listHelm(middleware.getNamespace(), middleware.getName(), cluster);
        if (CollectionUtils.isEmpty(list)){
            throw new BusinessException(ErrorMessage.MIDDLEWARE_NOT_EXIST);
        }
        ingressService.delete(middleware.getClusterId(), middleware.getNamespace(),
                middleware.getType(), middleware.getName());
        // ??????values.yaml ??????????????????
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
        // ??????pvc
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
            log.error("???????????????pvc??????", e);
        }
        
        beanCacheMiddleware.setValuesYaml(JSONObject.toJSONString(values));
        cacheMiddlewareService.insert(beanCacheMiddleware);
        // helm????????????????????????????????????????????????????????????404
        helmChartService.uninstall(middleware, cluster);
    }

    public void deleteStorage(Middleware middleware){
        BeanCacheMiddleware beanCacheMiddleware = cacheMiddlewareService.get(middleware);
        deletePvc(beanCacheMiddleware);
        deleteCustomConfigHistory(middleware);
        middlewareBackupService.deleteMiddlewareBackupInfo(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
        removeSql(middleware);
        //?????????????????????
        cacheMiddlewareService.delete(middleware);
    }

    /**
     * ????????????????????????
     */
    public void update(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (cluster == null) {
            cluster = clusterService.findById(middleware.getClusterId());
        }
        StringBuilder sb = new StringBuilder();

        // ????????????
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
        // ??????????????????
        updateCommonValues(sb, middleware);

        // ???????????????????????????
        if (sb.length() == 0) {
            return;
        }
        // ?????????????????????
        sb.deleteCharAt(sb.length() - 1);
        // ??????helm
        helmChartService.upgrade(middleware, sb.toString(), cluster);
    }

    /**
     * ??????????????????
     * @param sb
     * @param middleware
     */
    protected void updateCommonValues(StringBuilder sb, Middleware middleware){
        // ??????
        if (middleware.getDescription() != null) {
            sb.append("middleware-desc=").append(middleware.getDescription()).append(",");
        }

        // ????????????
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
     * ??????pvc??????
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
        // ??????pvc
        List<String> pvcNameList = middlewareCRService.getPvc(middleware.getClusterId(), middleware.getNamespace(),
                middleware.getType(), middleware.getName());
        // ??????pvc??????
        List<PersistentVolumeClaim> pvcList = new ArrayList<>();
        for (String pvcName : pvcNameList) {
            pvcList.add(pvcWrapper.get(middleware.getClusterId(), middleware.getNamespace(), pvcName));
        }
        updatePvc(middleware, pvcList);

        // ??????values.yaml
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
            log.error("??????{} ??????{} ?????????{} ???????????????????????????????????????: {}", mw.getClusterId(), mw.getNamespace(), mw.getName(), e);
        }
    }

    public void deleteMiddlewareBackupInfo(Middleware mw){

    }

    public void switchMiddleware(Middleware middleware) {

    }

    /**
     * ???helm chart??????middleware
     */
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (StringUtils.isEmpty(middleware.getClusterId())){
            middleware.setClusterId(cluster.getId());
        }
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        return middleware;
    }

    /**
     * ??????helm chart???values??????
     */
    protected void convertCommonByHelmChart(Middleware middleware, JSONObject values) {
        if (values != null) {
            middleware.setAliasName(values.getString("aliasName"))
                    .setDescription(values.getString("middleware-desc"))
                    .setLabels(values.getString("middleware-label"))
                    .setVersion(values.getString("version"))
                    .setMode(values.getString(MODE));
            // ??????chart-version
            if (StringUtils.isNotEmpty(values.getString("chart-version"))) {
                middleware.setChartVersion(values.getString("chart-version"));
            } else {
                BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoService.list(true).stream()
                    .filter(info -> info.getChartName().equals(middleware.getType())).collect(Collectors.toList())
                    .get(0);
                middleware.setChartVersion(beanMiddlewareInfo.getChartVersion());
            }
            // ??????annotations
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

            //????????????
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

            // ????????????????????????
            middleware.setHasConfigBackup(middlewareBackupService.checkIfAlreadyBackup(middleware.getClusterId(),middleware.getNamespace(),middleware.getType(),middleware.getName()));
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

        // ?????????????????????
        try {
            StorageDto storageDto = storageService.get(middleware.getClusterId(), values.getString("storageClassName"), false);
            quota.setStorageClassAliasName(storageDto.getAliasName());
        } catch (Exception e){
            log.error("?????????{}, ???????????????????????????", middleware.getName());
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
        // ?????????????????????
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
        //??????question.yaml
        convertQuestions(questionYaml.getQuestions(), dynamicValues, values);
        middleware.setDynamicValues(dynamicValues);
        //????????????tab
        middleware.setCapabilities(questionYaml.getCapabilities());
        //??????cpu???memory
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
     * ?????????????????????????????????????????????
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
     * ??????variable???values?????????????????????
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
     * ?????????values????????????????????????????????????
     */
    protected void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        replaceSimplyCommonValues(middleware, cluster, values);
        replaceDynamicValuesContent(middleware, cluster);
        replaceDynamicValues(middleware, values);
        //???????????????????????????
        values.put("custom", true);
        //???????????????tab???
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

        // label
        if (StringUtils.isNotBlank(middleware.getLabels())) {
            String[] labelAry = middleware.getLabels().split(CommonConstant.COMMA);
            JSONObject labelJson = new JSONObject();
            for (String label : labelAry) {
                String[] pair = label.split(CommonConstant.EQUAL);
                labelJson.put(pair[0], pair[1]);
            }
            values.put("labels", labelJson);
        }

        // node affinity
        if (!CollectionUtils.isEmpty(middleware.getNodeAffinity())) {
            // convert to k8s model
            JSONObject nodeAffinity = K8sConvert.convertNodeAffinity2Json(middleware.getNodeAffinity());
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

        //toleration
        if (!CollectionUtils.isEmpty(middleware.getTolerations())) {
            JSONArray jsonArray = K8sConvert.convertToleration2Json(middleware.getTolerations());
            values.put("tolerations", jsonArray);
            StringBuffer sbf = new StringBuffer();
            for (String toleration : middleware.getTolerations()) {
                sbf.append(toleration).append(",");
            }
            values.put("tolerationAry", sbf.substring(0, sbf.length()));
        }

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
     * ???????????????
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
    }

    /**
     * ??????????????????
     */
    protected void replaceDynamicValues(Middleware middleware, JSONObject values) {
        Map<String, String> dynamicValues = middleware.getDynamicValues();
        for (String key : dynamicValues.keySet()) {
            if ("labels".equals(key)) {
                continue;
            }
            // ??????????????????
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
     * ???????????????????????????
     */
    protected void replaceCommonResources(MiddlewareQuota quota, JSONObject resources) {

        // ??????limit???resources
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
     * ?????????????????????
     */
    protected void replaceCommonStorages(MiddlewareQuota quota, JSONObject values) {
        values.put("storageClassName", quota.getStorageClassName());
        values.put("storageSize", quota.getStorageClassQuota() + "Gi");
    }

    /**
     * ??????chart.yaml
     */
    protected void replaceChart(HelmChartFile helmChart, JSONObject values) {
        JSONObject chart = JSONObject.parseObject(helmChart.getYamlFileMap().get(CHART_YAML_NAME));
        
        // 1. ????????????????????????Chart.yaml??????????????????????????????????????????????????????????????????????????????????????????????????????
        // ?????????dependencies?????????alias???????????????operator????????????????????????operator?????????false
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
                    // ???????????????
                    for (int j = 1; j < condKeys.length; j++) {
                        sb.append("}");
                    }
                    values.put(condKeys[0], JSONObject.parseObject(sb.toString()));
                }
            }
        }
    }

    /**
     * ??????limit???cpu???memory
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
     * ??????pod/jvm?????????
     *
     * @param limitMemory limit???memory
     * @param rate        ??????
     * @param unitM       m?????????
     * @return
     */
    protected String calculateMem(String limitMemory, String rate, String unitM) {
        // ?????????mb
        double memory = ResourceCalculationUtil.getResourceValue(limitMemory, MEMORY, ResourceUnitEnum.MI.getUnit());
        // ?????????????????????????????????
        double mem = ResourceCalculationUtil.roundNumber((BigDecimal.valueOf(memory).multiply(new BigDecimal(rate))), 0,
            RoundingMode.CEILING);
        return (long)mem + unitM;
    }

    /**
     * ??????prometheusRules
     */
    public void updateAlerts(Middleware middleware) {
        // ??????cr
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
            log.error("??????{} ??????{} ?????????{}??? ????????????????????????????????????", middleware.getClusterId(), middleware.getNamespace(),
                middleware.getName());
        }
        
    }

    /**
     * ?????????????????????????????????????????????Running????????????????????????
     * @param middleware ???????????????
     * @param middlewareServiceNameIndex ????????????
     */
    public void tryCreateOpenService(Middleware middleware, MiddlewareServiceNameIndex middlewareServiceNameIndex, Boolean needRunningMiddleware) {
        boolean success = false;
        for (int i = 0; i < (60 * 10 * 60) && !success; i++) {
            Middleware detail = middlewareService.detail(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType());
            log.info("????????????{}??????????????????????????????{},????????????{}s", detail.getName(), detail.getStatus(), i);
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
     * ??????NodePort??????
     * @param middleware ???????????????
     * @param middlewareServiceNameIndex ?????????????????????
     */
    public void createOpenService(Middleware middleware, MiddlewareServiceNameIndex middlewareServiceNameIndex) {
        //1.??????????????????????????????????????????????????????????????????????????????????????????????????????
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
            //2.???????????????NodePort?????????????????????
            log.info("????????????????????????,clusterId={},namespace={},middlewareName={}",
                    middleware.getClusterId(), middleware.getNamespace(), middleware.getName());
            try {
                IngressDTO ingressDTO = new IngressDTO();
                ingressDTO.setName(middleware.getName() + "-nodeport-" +UUIDUtils.get8UUID().substring(0, 4));
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
                log.info("????????????????????????");
            } catch (Exception e) {
                log.error("????????????????????????", e);
            }
        }
    }

    /**
     * ??????????????????????????????
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
     *???????????????,??????????????????
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
                        MiddlewareAlertInfo middlewareAlertInfo = new MiddlewareAlertInfo();
                        middlewareAlertInfo.setAlert(rule.getAlert());
                        middlewareAlertInfo.setExpr(rule.getExpr());
                        middlewareAlertInfo.setSymbol(middlewareAlertsService.getSymbol(rule.getExpr()));
                        middlewareAlertInfo.setThreshold(middlewareAlertsService.getThreshold(rule.getExpr()));
                        middlewareAlertInfo.setTime(rule.getTime());
                        rule.getLabels().put("middleware",middleware.getType());
                        middlewareAlertInfo.setLabels(JSONUtil.toJsonStr(rule.getLabels()));
                        middlewareAlertInfo.setAnnotations(JSONUtil.toJsonStr(rule.getAnnotations()));
                        middlewareAlertInfo.setEnable("1");
                        middlewareAlertInfo.setLay("service");
                        middlewareAlertInfo.setClusterId(middleware.getClusterId());
                        middlewareAlertInfo.setNamespace(middleware.getNamespace());
                        middlewareAlertInfo.setMiddlewareName(middleware.getName());
                        middlewareAlertInfo.setName(middleware.getClusterId());
                        middlewareAlertInfo.setCreateTime(new Date());
                        middlewareAlertInfo.setType(middleware.getType());
                        middlewareAlertInfo.setDescription(rule.getAlert());
                        String expr = rule.getAlert() + middlewareAlertsService.getSymbol(rule.getExpr())
                                + middlewareAlertsService.getThreshold(rule.getExpr()) + "%"  + "???" + middlewareAlertInfo.getAlertTime()
                                + "???????????????" + middlewareAlertInfo.getAlertTimes() + "???";
                        middlewareAlertInfo.setAlertExpr(expr);
                        middlewareAlertInfoMapper.insert(middlewareAlertInfo);
                    }
                });
            }
        }
    }

    /**
     * ????????????????????????????????????????????????
     * @param middleware
     */
    public void removeSql(Middleware middleware) {
        QueryWrapper<MiddlewareAlertInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",middleware.getClusterId()).eq("namespace",middleware.getNamespace()).eq("middleware_name",middleware.getName());
        middlewareAlertInfoMapper.delete(wrapper);
    }

    /**
     * ?????????????????????????????????????????????
     */
    protected void convertRegistry(Middleware middleware, MiddlewareClusterDTO middlewareClusterDTO) {
        if (!ObjectUtils.isEmpty(middlewareClusterDTO.getRegistry())) {
            Registry registry = middlewareClusterDTO.getRegistry();
            String path = registry.getAddress() + ":" + registry.getPort() + "/" + registry.getChartRepo();
            middleware.setMirrorImage(path);
        }
    }

    public void convertExternal(JSONObject values, Middleware middleware, MiddlewareClusterDTO cluster){
        // ??????????????????
        JSONObject external = values.getJSONObject(EXTERNAL);
        external.put(ENABLE, TRUE);
        if (external.containsKey(USE_NODE_PORT)) {
            external.put(USE_NODE_PORT, FALSE);
        }
        for (IngressDTO ingressDTO : middleware.getIngresses()){
            // ????????????ip??????
            String exposeIp = getExposeIp(cluster, ingressDTO);
            // ??????????????????
            String splitTag = ingressDTO.getMiddlewareType().equals(MiddlewareTypeEnum.ROCKET_MQ.getType()) ? ";" : ",";
            // ?????????
            StringBuilder ipSb = new StringBuilder();
            StringBuilder svcSb = new StringBuilder();
            for (ServiceDTO serviceDTO : ingressDTO.getServiceList()) {
                if (serviceDTO.getServiceName().contains("-nameserver-proxy-svc")){
                    continue;
                }
                ipSb.append(exposeIp).append(":").append(serviceDTO.getExposePort()).append(splitTag);
                svcSb.append(serviceDTO.getServiceName()).append(splitTag);
            }
            // ???????????????????????????
            ipSb.deleteCharAt(ipSb.length() - 1);
            svcSb.deleteCharAt(svcSb.length() - 1);
            external.put(EXTERNAL_IP_ADDRESS, ipSb.toString());
            external.put(SVC_NAME_TAG, svcSb.toString());
        }
    }

    public String getExposeIp(MiddlewareClusterDTO cluster, IngressDTO ingressDTO){
        if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_NODEPORT)){
            return cluster.getHost();
        } else if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_INGRESS)
                && ingressDTO.getProtocol().equals(Protocol.TCP.getValue())){
            List<MiddlewareClusterIngress> middlewareClusterIngressList = cluster.getIngressList().stream()
                    .filter(ingress -> ingress.getIngressClassName().equals(ingressDTO.getIngressClassName()))
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(middlewareClusterIngressList)){
                return middlewareClusterIngressList.get(0).getAddress();
            }
        }
        return cluster.getHost();
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
}
