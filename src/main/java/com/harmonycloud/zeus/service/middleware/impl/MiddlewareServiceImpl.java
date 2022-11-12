package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MonitorResourceQuota;
import com.harmonycloud.caas.common.model.MonitorResourceQuotaBase;
import com.harmonycloud.caas.common.model.PrometheusResponse;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;
import com.harmonycloud.zeus.bean.BeanCacheMiddleware;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.bean.user.BeanRoleAuthority;
import com.harmonycloud.zeus.dao.BeanMiddlewareInfoMapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.operator.BaseOperator;
import com.harmonycloud.zeus.service.AbstractBaseService;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.*;
import com.harmonycloud.zeus.service.prometheus.PrometheusResourceMonitorService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.service.user.ProjectService;
import com.harmonycloud.zeus.service.user.RoleAuthorityService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import com.harmonycloud.zeus.util.ChartVersionUtil;
import com.harmonycloud.zeus.util.ServiceNameConvertUtil;
import com.harmonycloud.zeus.util.YamlUtil;
import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PODS;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 处理中间件
 */
@Slf4j
@Service
public class MiddlewareServiceImpl extends AbstractBaseService implements MiddlewareService {

    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;
    @Autowired
    private ClusterMiddlewareInfoService clusterMiddlewareInfoService;
    @Autowired
    private CacheMiddlewareService cacheMiddlewareService;
    @Autowired
    private PodService podService;
    @Autowired
    private GrafanaService grafanaService;
    @Autowired
    private ConfigMapService configMapService;
    @Autowired
    private PrometheusResourceMonitorService prometheusResourceMonitorService;
    @Autowired
    private ImageRepositoryService imageRepositoryService;
    @Autowired
    private IngressService ingressService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RoleAuthorityService roleAuthorityService;
    @Autowired
    private MiddlewareCrTypeService middlewareCrTypeService;
    @Autowired
    private BeanMiddlewareInfoMapper middlewareInfoMapper;
    @Autowired
    private PvcService pvcService;

    @Override
    public List<Middleware> simpleList(String clusterId, String namespace, String type, String keyword) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);

        Map<String, String> label = null;
        List<String> nameList = new ArrayList<>();
        boolean nameFilter = false;
        if (StringUtils.isNotEmpty(type)){
            if (MiddlewareTypeEnum.isType(type)){
                label = new HashMap<>(1);
                label.put("type", middlewareCrTypeService.findByType(type));
            }
            else {
                nameList = getNameList(clusterId, namespace, type);
                nameFilter = true;
            }
        }
        List<MiddlewareCR> mwList = middlewareCRService.listCR(clusterId, namespace, label);
        if (CollectionUtils.isEmpty(mwList)) {
            return new ArrayList<>(0);
        }
        //对于自定义中间件 根据名称进行过滤
        if (nameFilter) {
            if (CollectionUtils.isEmpty(nameList)) {
                return new ArrayList<>();
            }
            List<String> finalNameList = nameList;
            mwList = mwList.stream()
                .filter(mw -> finalNameList.stream().anyMatch(name -> mw.getSpec().getName().equals(name)))
                .collect(Collectors.toList());
        }

        // filter and convert
        Middleware middleware = new Middleware().setClusterId(clusterId).setNamespace(namespace).setType(type);
        Map<String, BaseOperator> operatorMap = new HashMap<>();
        boolean filter = StringUtils.isNotBlank(keyword);
        return mwList.stream().filter(mw -> !filter || mw.getMetadata().getName().contains(keyword)).map(mw -> {
            String middlewareType = middlewareCrTypeService.findTypeByCrType(mw.getSpec().getType());
            if (!operatorMap.containsKey(middlewareType)) {
                middleware.setType(middlewareType);
                operatorMap.put(middlewareType, getOperator(BaseOperator.class, BaseOperator.class, middleware));
            }
            try {
                return operatorMap.get(middlewareType)
                    .convertByHelmChart(middlewareCRService.simpleConvert(mw).setClusterId(clusterId), cluster);
            } catch (Exception e) {
                log.error("cluster:{} namespace:{} middleware:{} 获取详情失败", clusterId, namespace,
                    mw.getMetadata().getName(), e);
                return null;
            }
        }).filter(mw -> !ObjectUtils.isEmpty(mw)).collect(Collectors.toList());
    }

    @Override
    public Middleware detail(String clusterId, String namespace, String name, String type) {
        checkBaseParam(clusterId, namespace, name, type);
        Middleware middleware =
                new Middleware().setClusterId(clusterId).setNamespace(namespace).setType(type).setName(name);
        return getOperator(BaseOperator.class, BaseOperator.class, middleware).detail(middleware);
    }

    @Override
    public Middleware create(Middleware middleware) {
        checkBaseParam(middleware);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        // pre check
        operator.createPreCheck(middleware, cluster);
        updateRegistry(middleware,cluster);
        // create
        operator.create(middleware, cluster);
        // 查看middleware有没有创建出来
        boolean result = false;
        MiddlewareCR middlewareCR = null;
        for (int i = 0; i < (60 * 10 ) && !result; i++) {
            try {
                 middlewareCR = middlewareCRService.getCR(middleware.getClusterId(), middleware.getNamespace(),
                        middleware.getType(), middleware.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (middlewareCR != null) {
                result = true;
            }
            if (!result){
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (result) {
            return middleware;
        }
        return null;
    }

    @Override
    public void recovery(Middleware middleware) {
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        // pre check
        BeanCacheMiddleware beanCacheMiddleware = cacheMiddlewareService.get(middleware);
        // 1. download and read helm chart from registry
        HelmChartFile helmChart =
                helmChartService.getHelmChartFromMysql(middleware.getChartName(), middleware.getChartVersion());
        // 2. deal with values.yaml and Chart.yaml
        JSONObject values = JSONObject.parseObject(beanCacheMiddleware.getValuesYaml());
        Yaml yaml = new Yaml();
        helmChart.setValueYaml(yaml.dumpAsMap(values));
        helmChartService.coverYamlFile(helmChart);
        // 3. helm package & install
        String tgzFilePath = helmChartService.packageChart(helmChart.getTarFileName(), middleware.getChartName(),
                middleware.getChartVersion());
        helmChartService.install(middleware, tgzFilePath, cluster);
        // 删除数据库缓存
        cacheMiddlewareService.delete(middleware);
    }

    @Override
    public void update(Middleware middleware) {
        checkBaseParam(middleware);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        // pre check
        operator.updatePreCheck(middleware, cluster);
        // update
        operator.update(middleware, cluster);
        // reboot
        if (rebootCheck(middleware)) {
            reboot(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType());
        }
    }

    @Override
    public void delete(String clusterId, String namespace, String name, String type) {
        checkBaseParam(clusterId, namespace, name, type);
        Middleware middleware = new Middleware(clusterId, namespace, name, type);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        operator.delete(middleware);
    }

    @Override
    public void deleteStorage(String clusterId, String namespace, String name, String type) {
        checkBaseParam(clusterId, namespace, name, type);
        Middleware middleware = new Middleware(clusterId, namespace, name, type);
        getOperator(BaseOperator.class, BaseOperator.class, middleware).deleteStorage(middleware);
    }

    @Override
    public void switchMiddleware(String clusterId, String namespace, String name, String type, Boolean isAuto) {
        Middleware middleware = new Middleware(clusterId, namespace, name, type).setAutoSwitch(isAuto);
        getOperator(BaseOperator.class, BaseOperator.class, middleware).switchMiddleware(middleware);
    }

    @Override
    public MonitorDto monitor(String clusterId, String namespace, String name, String type, String chartVersion) {
        Middleware middleware = new Middleware(clusterId, namespace, name, type).setChartVersion(chartVersion);
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        List<BeanMiddlewareInfo> middlewareInfoList = middlewareInfoService.list(true);
        Map<String, BeanMiddlewareInfo> middlewareInfoMap  = new HashMap<>();
        for (BeanMiddlewareInfo beanMiddlewareInfo : middlewareInfoList) {
            middlewareInfoMap.put(beanMiddlewareInfo.getChartName() + ":" + beanMiddlewareInfo.getChartVersion(), beanMiddlewareInfo);
        }
        BeanMiddlewareInfo mwInfo = middlewareInfoMap.get(middleware.getType() + ":" + middleware.getChartVersion());
        if (mwInfo == null) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_NOT_EXIST);
        }
        if (StringUtils.isEmpty(mwInfo.getGrafanaId())){
            updateGrafanaId(mwInfo, middleware);
        }
        if (StringUtils.isEmpty(mwInfo.getGrafanaId())){
            throw new BusinessException(ErrorMessage.GRAFANA_ID_NOT_FOUND);
        }

        MiddlewareClusterMonitor monitor = cluster.getMonitor();
        if (monitor == null){
            throw new BusinessException(ErrorMessage.CLUSTER_MONITOR_INFO_NOT_FOUND);
        }
        MiddlewareClusterMonitorInfo monitorInfo =monitor.getGrafana();
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

    @Override
    public void reboot(String clusterId, String namespace, String name, String type) {
        try {
            MiddlewareCR mw = middlewareCRService.getCR(clusterId, namespace, type, name);
            List<MiddlewareInfo> pods = mw.getStatus().getInclude().get(PODS);
            if(!CollectionUtils.isEmpty(pods)){
                pods.forEach(pod -> podService.restart(clusterId, namespace, name, type, pod.getName()));
            }
        } catch (Exception e){
            throw new BusinessException(ErrorMessage.MIDDLEWARE_REBOOT_FAILED);
        }
    }

    @Override
    public void updateStorage(Middleware middleware) {
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        operator.updateStorage(middleware);
    }

    private void checkBaseParam(Middleware mw) {
        checkBaseParam(mw.getClusterId(), mw.getNamespace(), mw.getName(), mw.getType());
        if (StringUtils.isAnyBlank(mw.getChartName(), mw.getChartVersion())) {
            throw new IllegalArgumentException("chartName or chartVersion is null");
        }
    }

    private void checkBaseParam(String clusterId, String namespace, String name, String type) {
        if (StringUtils.isAnyBlank(clusterId, namespace, name, type)) {
            throw new IllegalArgumentException("middleware clusterId/namespace/name/type is null");
        }
    }

    public List<String> getNameList(String clusterId, String namespace, String type) {
        // 获取中间件chartName + chartVersion
        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoService.list(true);
        mwInfoList =
            mwInfoList.stream().filter(mwInfo -> mwInfo.getChartName().equals(type)).collect(Collectors.toList());
        List<String> chartList = mwInfoList.stream()
            .map(mwInfo -> mwInfo.getChartName() + "-" + mwInfo.getChartVersion()).collect(Collectors.toList());
        // 获取helm list信息
        List<HelmListInfo> helmInfoList = helmChartService.listHelm(namespace, "", clusterService.findById(clusterId));
        helmInfoList = helmInfoList.stream()
            .filter(helmInfo -> chartList.stream().anyMatch(chart -> chart.equals(helmInfo.getChart())))
            .collect(Collectors.toList());
        return helmInfoList.stream().map(HelmListInfo::getName).collect(Collectors.toList());
    }

    @Override
    public <T, R> T getOperator(Class<T> funClass, Class<R> baseClass, Object... types) {
        return super.getOperator(funClass, baseClass, types);
    }

    @Override
    public List<MiddlewareBriefInfoDTO> list(String clusterId, String namespace, String type, String keyword, String projectId)
        throws Exception {
        // 获取中间件chart包信息
        List<BeanMiddlewareInfo> beanMiddlewareInfoList = middlewareInfoService.list(true);
        // get cluster
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // helm list 并过滤获取属于中间件的发布
        List<HelmListInfo> helmListInfoList = helmChartService.listHelm(namespace, null, cluster).stream()
            .filter(info -> beanMiddlewareInfoList.stream()
                .anyMatch(mwInfo -> info.getChart().equals(mwInfo.getChartName() + "-" + mwInfo.getChartVersion())))
                .filter(info -> StringUtils.isEmpty(type) || info.getChart().contains(type))
            .collect(Collectors.toList());
        // list middleware cr
        List<Middleware> middlewareList = middlewareCRService.list(clusterId, namespace, type, false);

        List<HelmListInfo> finalHelmListInfoList = helmListInfoList;
        // 过滤掉helm中没有的middleware 并设置chart-version
        middlewareList = middlewareList.stream().filter(mw -> finalHelmListInfoList.stream().anyMatch(info -> {
            if (info.getName().equals(mw.getName()) && info.getNamespace().equals(mw.getNamespace())) {
                mw.setChartVersion(info.getChart().replace(info.getChart().split("-")[0] + "-", ""));
                return true;
            } else {
                return false;
            }
        })).collect(Collectors.toList());
        // 获取还未创建出middleware的release
        List<Middleware> finalMiddlewareList = middlewareList;
        helmListInfoList = helmListInfoList.stream()
            .filter(info -> finalMiddlewareList.stream().noneMatch(mw -> mw.getName().equals(info.getName())))
            .collect(Collectors.toList());
        helmListInfoList.forEach(info -> {
            Middleware middleware =
                new Middleware(clusterId, info.getNamespace(), info.getName(), info.getChart().split("-")[0]);
            if (compareTime(info.getUpdateTime())) {
                middleware.setStatus("failed");
            } else {
                middleware.setStatus("Preparing");
            }
            finalMiddlewareList.add(middleware);
        });
        // 获取values.yaml的详情
        final CountDownLatch count = new CountDownLatch(finalMiddlewareList.size());
        finalMiddlewareList.forEach(mw -> ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                 getOperator(BaseOperator.class, BaseOperator.class, mw).convertByHelmChart(mw, cluster);
            } finally {
                count.countDown();
            }
        }));
        count.await();
        // 获取未完全删除的中间件
        if (StringUtils.isNotBlank(type)) {
            List<BeanCacheMiddleware> beanCacheMiddlewareList = cacheMiddlewareService.list(clusterId, namespace, type);
            for (BeanCacheMiddleware beanCacheMiddleware : beanCacheMiddlewareList) {
                Middleware middleware = new Middleware();
                BeanUtils.copyProperties(beanCacheMiddleware, middleware);
                if (StringUtils.isEmpty(beanCacheMiddleware.getValuesYaml())) {
                    middleware.setStatus("Deleted");
                } else {
                    if (StringUtils.isEmpty(beanCacheMiddleware.getPvc())
                        || !pvcService.checkPvcExist(beanCacheMiddleware.getClusterId(),
                            beanCacheMiddleware.getNamespace(), beanCacheMiddleware.getPvc().split(","))) {
                        cacheMiddlewareService.delete(middleware);
                        continue;
                    } else {
                        middleware.setStatus("Deleting");
                    }
                }
                // 先移除可能因为异步导致残留的原中间件信息
                finalMiddlewareList.removeIf(mw -> mw.getName().equals(beanCacheMiddleware.getName())
                    && mw.getNamespace().equals(beanCacheMiddleware.getNamespace()));
                finalMiddlewareList.add(middleware);
            }
        }

        List<Middleware> result = finalMiddlewareList;
        // 关键词过滤
        if (StringUtils.isNotEmpty(keyword)) {
            result = result.stream()
                    .filter(mw -> mw.getName().contains(keyword)
                            || (StringUtils.isNotEmpty(mw.getAliasName()) && mw.getAliasName().contains(keyword)))
                    .collect(Collectors.toList());
        }
        if (StringUtils.isNotEmpty(projectId)) {
            // 根据项目分区进行过滤
            List<Namespace> projectNamespaceList = projectService.getNamespace(projectId);
            result = result.stream()
                .filter(mw -> projectNamespaceList.stream().anyMatch(
                    pn -> pn.getName().equals(mw.getNamespace()) && pn.getClusterId().equals(mw.getClusterId())))
                .collect(Collectors.toList());
            // 根据类型进行过滤
            if (StringUtils.isEmpty(type)) {
                Integer roleId = userRoleService.getRoleId(CurrentUserRepository.getUser().getUsername(), projectId);
                List<BeanRoleAuthority> power = roleAuthorityService.list(roleId);
                result = result.stream()
                    .filter(mw -> power.stream()
                        .anyMatch(ra -> ra.getType().equals(mw.getType()) && !"0000".equals(ra.getPower())))
                    .collect(Collectors.toList());
            }
        }
        result.sort(new MiddlewareComparator());
        // 封装数据
        List<MiddlewareBriefInfoDTO> list = new ArrayList<>();
        Map<String,
            String> middlewareImagePathMap = middlewareInfoService.filter(beanMiddlewareInfoList).stream()
                .filter(beanMiddlewareInfo -> beanMiddlewareInfo.getImagePath() != null)
                .collect(Collectors.toMap(BeanMiddlewareInfo::getChartName, BeanMiddlewareInfo::getImagePath));
        Map<String, List<Middleware>> middlewareListMap =
            result.stream().collect(Collectors.groupingBy(Middleware::getType));
        for (String key : middlewareListMap.keySet()) {
            // 根据创建时间排序
            List<Middleware> tempMiddlewareList = middlewareListMap.get(key);
            tempMiddlewareList.sort(new MiddlewareComparator());
            // 封装数据
            MiddlewareBriefInfoDTO briefInfoDTO = new MiddlewareBriefInfoDTO();
            briefInfoDTO.setChartName(key);
            briefInfoDTO.setServiceList(tempMiddlewareList);
            briefInfoDTO.setServiceNum(tempMiddlewareList.size());
            briefInfoDTO.setName(key);
            briefInfoDTO.setImagePath(middlewareImagePathMap.getOrDefault(key, null));
            list.add(briefInfoDTO);
        }
        return list;
    }

    @Override
    public List<MiddlewareInfoDTO> version(String clusterId, String namespace, String name, String type) {
        List<BeanMiddlewareInfo> middlewareInfos = middlewareInfoService.listByType(type);
        // 将倒序转为正序
        Collections.reverse(middlewareInfos);
        Middleware middleware = detail(clusterId, namespace, name, type);
        // 升级服务时，只能升级到当前服务的上一个版本，不能跨版本升级,设置标志变量existNow来判断是否是上一个版本
        AtomicBoolean existNow = new AtomicBoolean(false);
        BeanClusterMiddlewareInfo clusterMwInfo = clusterMiddlewareInfoService.get(clusterId, type);
        List<MiddlewareInfoDTO> resList = middlewareInfos.stream().map(info -> {
            MiddlewareInfoDTO dto = new MiddlewareInfoDTO();
            BeanUtils.copyProperties(info, dto);
            if (ChartVersionUtil.compare(middleware.getChartVersion(), info.getChartVersion()) < 0) {
                dto.setVersionStatus("history");
            } else if (ChartVersionUtil.compare(middleware.getChartVersion(), info.getChartVersion()) == 0) {
                dto.setVersionStatus("now");
            } else {
                dto.setVersionStatus("future");
            }
            return dto;
        }).collect(Collectors.toList());
        resList.sort(((o1, o2) -> {
            return ChartVersionUtil.compare(o1.getChartVersion(), o2.getChartVersion());
        }));
        return resList;
    }

    @Override
    public BaseResult upgradeChart(String clusterId, String namespace, String name, String type, String chartName, String upgradeChartVersion) {
        HelmChartFile helmChart = helmChartService.getHelmChartFromMysql(chartName, upgradeChartVersion);
        JSONObject upgradeValues = YamlUtil.convertYamlAsNormalJsonObject(helmChart.getValueYaml());
        YamlUtil.convertToStandardJsonObject(upgradeValues);
        JSONObject currentValues = helmChartService.getInstalledValuesAsNormalJson(name, namespace, clusterService.findById(clusterId));

        String currentChartVersion = currentValues.getString("chart-version");
        String compatibleVersions = upgradeValues.getString("compatibleVersions");
        // 检查chart版本是否符合升级要求
        BaseResult upgradeCheckRes = checkUpgradeVersion(currentChartVersion, upgradeChartVersion, compatibleVersions);
        if (!upgradeCheckRes.getSuccess()) {
            return upgradeCheckRes;
        }
        // 检查operator是否符合升级要求
        BaseResult operatorCheckRes = checkOperatorVersion(upgradeChartVersion, clusterMiddlewareInfoService.get(clusterId, type));
        if (!operatorCheckRes.getSuccess()) {
            return operatorCheckRes;
        }

        JSONObject upgradeImage = SerializationUtils.clone(upgradeValues.getJSONObject("image"));
        JSONObject currentImage = SerializationUtils.clone(currentValues.getJSONObject("image"));
        JSONObject resValues = YamlUtil.jsonMerge(currentValues, upgradeValues);
        if (currentImage != null) {
            if (currentImage.getString("repository") != null) {
                upgradeImage.put("repository", currentImage.getString("repository"));
            }
            JSONObject resImage = YamlUtil.jsonMerge(upgradeImage, currentImage);
            resValues.put("image", resImage);
        }
        resValues.put("chart-version", upgradeChartVersion);
        // 执行升级
        Middleware middleware = detail(clusterId, namespace, name, type);
        middleware.setChartName(chartName);
        middleware.setChartVersion(upgradeChartVersion);
        helmChartService.upgrade(middleware, currentValues, upgradeValues, clusterService.findById(clusterId));
        return BaseResult.ok();
    }

    @Override
    public BaseResult upgradeCheck(String clusterId, String namespace, String name, String type, String chartName, String upgradeChartVersion) {
        HelmChartFile helmChart = helmChartService.getHelmChartFromMysql(chartName, upgradeChartVersion);
        JSONObject currentValues = helmChartService.getInstalledValuesAsNormalJson(name, namespace, clusterService.findById(clusterId));

        String currentChartVersion = currentValues.getString("chart-version");
        String compatibleVersions = helmChart.getCompatibleVersions();
        // 检查chart版本是否符合升级要求
        BaseResult upgradeCheckRes = checkUpgradeVersion(currentChartVersion, upgradeChartVersion, compatibleVersions);
        if (!upgradeCheckRes.getSuccess()) {
            return upgradeCheckRes;
        }
        // 检查operator是否符合升级要求
        BaseResult operatorCheckRes = checkOperatorVersion(upgradeChartVersion, clusterMiddlewareInfoService.get(clusterId, type));
        if (!operatorCheckRes.getSuccess()) {
            return operatorCheckRes;
        }
        return BaseResult.ok();
    }

    /**
     * 更新grafanaid
     */
    public void updateGrafanaId(BeanMiddlewareInfo mwInfo, Middleware middleware) {
        HelmChartFile helm = helmChartService.getHelmChartFromMysql(middleware.getType(), middleware.getChartVersion());
        String alias;
        if (!CollectionUtils.isEmpty(helm.getDependency())) {
            alias = helm.getDependency().get("alias");
        } else {
            alias = middleware.getName();
        }
        List<HelmListInfo> helmInfos =
                helmChartService.listHelm("", alias, clusterService.findById(middleware.getClusterId()));
        if (!CollectionUtils.isEmpty(helmInfos)) {
            // 获取configmap
            HelmListInfo helmInfo = helmInfos.get(0);
            // 特殊处理pg
            if ("postgresql".equals(mwInfo.getChartName())){
                alias = alias + "-postgresql";
            }
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

    /**
     * @description 统计指定类型服务数量和异常服务数量
     * @param middlewares 所有服务
     * @param chartName 中间件chartName
     * @param serviceNum 服务数量
     * @param errServiceNum 异常服务数量
     */
    private void countServiceNum(String clusterId, List<Middleware> middlewares, String chartName, AtomicInteger serviceNum, AtomicInteger errServiceNum) {
        for (Middleware middleware : middlewares) {
            try {
                if (!clusterId.equals(middleware.getClusterId()) || !chartName.equals(middleware.getType())) {
                    continue;
                }
                serviceNum.getAndAdd(1);
                if (middleware.getStatus() != null) {
                    if (!NameConstant.RUNNING.equalsIgnoreCase(middleware.getStatus())) {
                        //中间件服务状态异常
                        errServiceNum.getAndAdd(1);
                    }
                } else {
                    errServiceNum.getAndAdd(1);
                }
            } catch (Exception e) {
                log.error("统计服务数量出错了,chartName={},type={}", chartName, e);
            }
        }
    }

    @Override
    public MiddlewareTopologyDTO topology(String clusterId, String namespace, String name, String type) throws Exception {
        Middleware middleware = podService.list(clusterId, namespace, name, type);
        MiddlewareTopologyDTO middlewareTopologyDTO =
            new MiddlewareTopologyDTO().setClusterId(clusterId).setNamespace(namespace).setName(name).setType(type)
                .setStatus(middleware.getStatus()).setPods(middleware.getPods())
                .setPodInfoGroup(middleware.getPodInfoGroup()).setMonitorResourceQuota(new MonitorResourceQuota());
        // 获取alias name
        JSONObject values = helmChartService.getInstalledValues(name, namespace, clusterService.findById(clusterId));
        middlewareTopologyDTO.setAliasName(values.getOrDefault("aliasName", "").toString());
        // 特殊处理es存储类型
        if (MiddlewareTypeEnum.ELASTIC_SEARCH.getType().equals(type)) {
            Set<String> scSet = new HashSet<>();
            middleware.getPods().forEach(pod -> {
                if (StringUtils.isNotEmpty(pod.getResources().getStorageClassName())) {
                    scSet.add(pod.getResources().getStorageClassName());
                }
            });
            StringBuilder sb = new StringBuilder();
            for (String sc : scSet) {
                sb.append(sc).append("/");
            }
            if (sb.length() != 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            middlewareTopologyDTO.setStorageClassName(sb.toString());
        } else {
            middlewareTopologyDTO.setStorageClassName(values.getOrDefault("storageClassName", "").toString());
        }

        StringBuilder pods = new StringBuilder();
        middleware.getPods().forEach(podInfo -> {
            pods.append(podInfo.getPodName()).append("|");
            podInfo.setMonitorResourceQuota(new MonitorResourceQuota());
        });
        final CountDownLatch cd = new CountDownLatch(6);
        // 查询total cpu
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                String totalCpuQuery = "sum(kube_pod_container_resource_requests_cpu_cores{pod=~\"" + pods.toString()
                    + "\",namespace=\"" + namespace + "\"}) by (pod)";
                PrometheusResponse totalCpu = prometheusResourceMonitorService.query(clusterId, totalCpuQuery);
                Map<String, Double> result = convertResponse(totalCpu);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String num = podInfo.getPodName().substring(podInfo.getPodName().length() - 1);
                    if (result.containsKey(num)) {
                        podInfo.getMonitorResourceQuota().getCpu().setTotal(result.get(num));
                    }
                });
            } catch (Exception e) {
                log.error("中间件{} 查询total cpu失败", name);
            }
            finally {
                cd.countDown();
            }
        });
        // 查询used cpu
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                String usedCpuQuery = "sum(rate(container_cpu_usage_seconds_total{pod=~\"" + pods.toString()
                    + "\",namespace=\"" + namespace + "\",endpoint!=\"\"}[5m])) by (pod)";
                PrometheusResponse usedCpu = prometheusResourceMonitorService.query(clusterId, usedCpuQuery);
                Map<String, Double> result = convertResponse(usedCpu);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String num = podInfo.getPodName().substring(podInfo.getPodName().length() - 1);
                    if (result.containsKey(num)){
                        podInfo.getMonitorResourceQuota().getCpu().setUsed(result.get(num));
                    }
                });
            } catch (Exception e) {
                log.error("中间件{} 查询used cpu失败", name);
            } finally {
                cd.countDown();
            }
        });
        // 查询total memory
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                String totalMemoryQuery = "sum(kube_pod_container_resource_requests_memory_bytes{pod=~\""
                    + pods.toString() + "\",namespace=\"" + namespace + "\"}) by (pod) /1024/1024/1024";
                PrometheusResponse totalMemory = prometheusResourceMonitorService.query(clusterId, totalMemoryQuery);
                Map<String, Double> result = convertResponse(totalMemory);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String num = podInfo.getPodName().substring(podInfo.getPodName().length() - 1);
                    if (result.containsKey(num)){
                        podInfo.getMonitorResourceQuota().getMemory().setTotal(result.get(num));
                    }
                });
            } catch (Exception e) {
                log.error("中间件{} 查询total memory失败", name);
            } finally {
                cd.countDown();
            }
        });
        // 查询used memory
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                String usedMemoryQuery = "sum(container_memory_working_set_bytes{pod=~\"" + pods.toString()
                    + "\",namespace=\"" + namespace + "\",endpoint!=\"\"}) by (pod) /1024/1024/1024/2";
                PrometheusResponse usedMemory = prometheusResourceMonitorService.query(clusterId, usedMemoryQuery);
                Map<String, Double> result = convertResponse(usedMemory);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String num = podInfo.getPodName().substring(podInfo.getPodName().length() - 1);
                    if (result.containsKey(num)){
                        podInfo.getMonitorResourceQuota().getMemory().setUsed(result.get(num));
                    }
                });
            } catch (Exception e) {
                log.error("中间件{} 查询used memory配额失败", name);
            } finally {
                cd.countDown();
            }
        });

        // 获取pvc list
        List<String> pvcList = middlewareCRService.getPvc(clusterId, namespace, type, name);
        StringBuilder pvcs = new StringBuilder();
        pvcList.forEach(pvc -> pvcs.append(pvc).append("|"));
        // 查询total storage
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                String totalStorageQuery =
                    "sum(kube_persistentvolumeclaim_resource_requests_storage_bytes{persistentvolumeclaim=~\""
                        + pvcs.toString() + "\",namespace=\"" + namespace
                        + "\"}) by (persistentvolumeclaim) /1024/1024/1024";
                PrometheusResponse totalStorage = prometheusResourceMonitorService.query(clusterId, totalStorageQuery);
                Map<String, Double> result = convertResponse(totalStorage);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String num = podInfo.getPodName().substring(podInfo.getPodName().length() - 1);
                    if (result.containsKey(num)) {
                        MonitorResourceQuotaBase cpu = new MonitorResourceQuotaBase();
                        cpu.setTotal(result.get(num));
                        podInfo.getMonitorResourceQuota().getStorage().setTotal(result.get(num));
                    }
                });
            } catch (Exception e) {
                log.error("中间件{} 查询total storage配额失败", name);
            } finally {
                cd.countDown();
            }
        });
        // 查询used storage
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                String usedStorageQuery =
                    "sum(kubelet_volume_stats_used_bytes{persistentvolumeclaim=~\""
                        + pvcs.toString() + "\",namespace=\"" + namespace
                        + "\",endpoint!=\"\"}) by (persistentvolumeclaim) /1024/1024/1024";
                PrometheusResponse usedStorage = prometheusResourceMonitorService.query(clusterId, usedStorageQuery);
                Map<String, Double> result = convertResponse(usedStorage);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String num = podInfo.getPodName().substring(podInfo.getPodName().length() - 1);
                    if (result.containsKey(num)){
                        podInfo.getMonitorResourceQuota().getStorage().setUsed(result.get(num));
                    }
                });
            } catch (Exception e) {
                log.error("中间件{} 查询used storage配额失败", name);
            } finally {
                cd.countDown();
            }
        });
        cd.await();
        double totalCpu = 0.0;
        double usedCpu = 0.0;
        double totalMemory = 0.0;
        double usedMemory = 0.0;
        double totalStorage = 0.0;
        double usedStorage = 0.0;
        // 计算使用率和中间件自身的资源使用情况
        for (PodInfo podInfo : middlewareTopologyDTO.getPods()){
            // 计算cpu使用率
            if (podInfo.getMonitorResourceQuota().getCpu().getUsed() != null
                && podInfo.getMonitorResourceQuota().getCpu().getTotal() != null) {
                double cpuUsage = podInfo.getMonitorResourceQuota().getCpu().getUsed()
                    / podInfo.getMonitorResourceQuota().getCpu().getTotal() * 100;
                podInfo.getMonitorResourceQuota().getCpu().setUsage(
                    ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(cpuUsage), 2, RoundingMode.CEILING));
                usedCpu = usedCpu + podInfo.getMonitorResourceQuota().getCpu().getUsed();
                totalCpu = totalCpu + podInfo.getMonitorResourceQuota().getCpu().getTotal();
            }

            // 计算memory使用率
            if (podInfo.getMonitorResourceQuota().getMemory().getUsed() != null && podInfo.getMonitorResourceQuota().getMemory().getTotal() != null){
                double memoryUsage = podInfo.getMonitorResourceQuota().getMemory().getUsed()
                        / podInfo.getMonitorResourceQuota().getMemory().getTotal() * 100;
                podInfo.getMonitorResourceQuota().getMemory().setUsage(
                        ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(memoryUsage), 2, RoundingMode.CEILING));
                usedMemory = usedMemory + podInfo.getMonitorResourceQuota().getMemory().getUsed();
                totalMemory = totalMemory + podInfo.getMonitorResourceQuota().getMemory().getTotal();
            }

            // 计算storage使用率
            if (podInfo.getMonitorResourceQuota().getStorage().getUsed() != null
                && podInfo.getMonitorResourceQuota().getStorage().getTotal() != null) {
                double storageUsage = podInfo.getMonitorResourceQuota().getStorage().getUsed()
                    / podInfo.getMonitorResourceQuota().getStorage().getTotal() * 100;
                podInfo.getMonitorResourceQuota().getStorage().setUsage(
                    ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(storageUsage), 2, RoundingMode.CEILING));
                usedStorage = usedStorage + podInfo.getMonitorResourceQuota().getStorage().getUsed();
                totalStorage = totalStorage + podInfo.getMonitorResourceQuota().getStorage().getTotal();
            }
        }
        // middleware cpu
        middlewareTopologyDTO.getMonitorResourceQuota().getCpu().setUsed(usedCpu);
        middlewareTopologyDTO.getMonitorResourceQuota().getCpu().setTotal(totalCpu);
        if (totalCpu != 0) {
            middlewareTopologyDTO.getMonitorResourceQuota().getCpu().setUsage(ResourceCalculationUtil
                .roundNumber(BigDecimal.valueOf(usedCpu / totalCpu * 100), 2, RoundingMode.CEILING));
        }

        // middleware memory
        middlewareTopologyDTO.getMonitorResourceQuota().getMemory().setUsed(usedMemory);
        middlewareTopologyDTO.getMonitorResourceQuota().getMemory().setTotal(totalMemory);
        if (totalMemory != 0) {
            middlewareTopologyDTO.getMonitorResourceQuota().getMemory().setUsage(ResourceCalculationUtil
                .roundNumber(BigDecimal.valueOf(usedMemory / totalMemory * 100), 2, RoundingMode.CEILING));
        }

        // middleware storage
        middlewareTopologyDTO.getMonitorResourceQuota().getStorage().setUsed(usedStorage);
        middlewareTopologyDTO.getMonitorResourceQuota().getStorage().setTotal(totalStorage);
        if (totalStorage != 0) {
            middlewareTopologyDTO.getMonitorResourceQuota().getStorage().setUsage(ResourceCalculationUtil
                .roundNumber(BigDecimal.valueOf(usedStorage / totalStorage * 100), 2, RoundingMode.CEILING));
        }

        return middlewareTopologyDTO;
    }

    @Override
    public String platform(String clusterId, String namespace, String name, String type) {
        if (!type.equals(MiddlewareTypeEnum.ELASTIC_SEARCH.getType())
                && !type.equals(MiddlewareTypeEnum.KAFKA.getType())
                && !type.equals(MiddlewareTypeEnum.ROCKET_MQ.getType())) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_MANAGER_PLATFORM_NOT_SUPPORT);
        }
        List<IngressDTO> ingressDTOS = ingressService.get(clusterId, namespace, type, name);
        String servicePort = ServiceNameConvertUtil.getManagePlatformServicePort(type);
        for (IngressDTO ingressDTO : ingressDTOS) {
            if (!CollectionUtils.isEmpty(ingressDTO.getRules())) {
                List<IngressRuleDTO> rules = ingressDTO.getRules();
                for (IngressRuleDTO rule : rules) {
                    String domain = rule.getDomain();
                    List<IngressHttpPath> ingressHttpPaths = rule.getIngressHttpPaths();
                    if (!CollectionUtils.isEmpty(ingressHttpPaths)) {
                        for (IngressHttpPath ingressHttpPath : ingressHttpPaths) {
                            assert servicePort != null;
                            if (servicePort.equals(ingressHttpPath.getServicePort())) {
                                return domain + ingressHttpPath.getPath();
                            }
                        }
                    }
                }
            }
            List<ServiceDTO> serviceList = ingressDTO.getServiceList();
            Set<String> ipSet = ingressService.listIngressIp(clusterId, ingressDTO.getIngressClassName());
            String exposeIp = "";
            for (String ip : ipSet) {
                exposeIp = ip;
                break;
            }
            if (!CollectionUtils.isEmpty(serviceList)) {
                for (ServiceDTO serviceDTO : serviceList) {
                    if (serviceDTO.getServicePort().equals(servicePort)) {
                        return (StringUtils.isBlank(ingressDTO.getExposeIP()) ? exposeIp : ingressDTO.getExposeIP()) + ":" + serviceDTO.getExposePort();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Integer middlewareCount(String clusterId, String namespace, String name, String type) {
        
        return null;
    }

    @Override
    public String middlewareImage(String type, String version) {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("chart_name", type);
        wrapper.eq("chart_version", version);
        BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoMapper.selectOne(wrapper);
        if (beanMiddlewareInfo == null) {
            return "";
        }
        return beanMiddlewareInfo.getImagePath();
    }

    /**
     * 服务排序类，按服务数量进行排序
     */
    public static class ServiceMapComparator implements Comparator<Map> {
        @Override
        public int compare(Map service1, Map service2) {
            if (Integer.parseInt(service1.get("serviceNum").toString()) > Integer.parseInt(service2.get("serviceNum").toString())) {
                return -1;
            } else if (Integer.parseInt(service1.get("serviceNum").toString()) == Integer.parseInt(service2.get("serviceNum").toString())) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    /**
     * 服务排序类，按服务数量进行排序
     */
    public static class MiddlewareBriefInfoDTOComparator implements Comparator<MiddlewareBriefInfoDTO> {
        @Override
        public int compare(MiddlewareBriefInfoDTO o1, MiddlewareBriefInfoDTO o2) {
            if (o1.getServiceNum() > o2.getServiceNum()) {
                return -1;
            } else if (o1.getServiceNum() == o2.getServiceNum()) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    /**
     * 中间件类型排序类
     */
    public static class MiddlewareInfoDTOComparator implements Comparator<MiddlewareInfoDTO> {
        @Override
        public int compare(MiddlewareInfoDTO o1, MiddlewareInfoDTO o2) {
            int temp = o1.getChartName().compareTo(o2.getChartName());
            if (temp > 0) {
                return 1;
            } else if (temp < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * 服务排序类，按创建时间排序
     */
    public static class MiddlewareComparator implements Comparator<Middleware> {
        @Override
        public int compare(Middleware o1, Middleware o2) {
            return o1.getCreateTime() == null ? 1
                : o2.getCreateTime() == null ? 1 : o2.getCreateTime().compareTo(o1.getCreateTime());
        }
    }

    /**
     * 检查中间件chart升级版本是否符合要求
     * @param currentVersion 中间件当前版本
     * @param upgradeVersion 中间件升级版本
     * @param compatibleVersions 升级版本所需最低版本
     * @return
     */
    private static BaseResult checkUpgradeVersion(String currentVersion, String upgradeVersion, String compatibleVersions) {
        // 1.判断是否升级到了低版本
        if (ChartVersionUtil.compare(currentVersion, upgradeVersion) < 0) {
            log.error("不能升级到更低版本");
            return BaseResult.error("不能升级到更低版本", ErrorMessage.UPGRADE_LOWER_VERSION_FAILED);
        }
        if (StringUtils.isNotBlank(compatibleVersions)) {
            // 2.判断是否满足升级至当前chart所需最低版本
            if (ChartVersionUtil.compare(currentVersion, compatibleVersions) > 0) {
                log.error("不满足升级所需最低版本");
                return BaseResult.error("因版本兼容性问题，请先升级到 " + compatibleVersions, ErrorMessage.UPGRADE_NOT_SATISFY_LOWEST_VERSION);
            }
        } else {
            // 3.判断是否跨大版本升级,即判断大版本号是否相同，相同才可以升级
            String current = currentVersion.split("\\.")[0];
            String upgrade = upgradeVersion.split("\\.")[0];
            if (!current.equals(upgrade)) {
                return BaseResult.error("不能跨大版本升级", ErrorMessage.UPGRADE_OVER_VERSION_FAILED);
            }
        }
        return BaseResult.ok();
    }

    /**
     * 检查当前operator版本是否满足升级要求，operator版本需要比升级chart版本高，才可以进行升级，否则需要升级operator
     * @param upgradeChartVersion
     * @param clusterMwInfo
     */
    private static BaseResult checkOperatorVersion(String upgradeChartVersion, BeanClusterMiddlewareInfo clusterMwInfo) {
        if (ChartVersionUtil.compare(upgradeChartVersion, clusterMwInfo.getChartVersion()) < 0) {
            log.error("operator版本比升级chart版本小，需要升级operator版本");
            return BaseResult.error("请先升级中间件版本到 " + upgradeChartVersion, ErrorMessage.UPGRADE_OPERATOR_TOO_LOWER);
        } else {
            if (clusterMwInfo.getStatus() == 0) {
                log.error("operator升级中");
                return BaseResult.error("中间件升级中,请稍后升级", ErrorMessage.UPGRADE_OPERATOR_UPDATING);
            }
        }
        return BaseResult.ok();
    }

    /**
     * 检查是否跨大版本升级
     * @param currentVersion 当前中间件chart版本
     * @param upgradeVersion 升级chart版本
     * @return
     */
    private static boolean isOverBigVersion(String currentVersion, String upgradeVersion) {
        String current = currentVersion.split("\\.")[0];
        String upgrade = upgradeVersion.split("\\.")[0];
        return !current.equals(upgrade);
    }

    public Map<String, Double> convertResponse(PrometheusResponse response) {
        Map<String, Double> map = new HashMap<>();
        response.getData().getResult().forEach(res -> {
            res.getMetric().forEach((k, v) -> {
                map.put(v.substring(v.length() - 1), ResourceCalculationUtil.roundNumber(
                    BigDecimal.valueOf(Double.parseDouble(res.getValue().get(1))), 2, RoundingMode.CEILING));
            });
        });
        return map;
    }

    /**
     * 检查是否需要重启服务
     * @param middleware
     * @return
     */
    public boolean rebootCheck(Middleware middleware) {
        if (middleware.getStdoutEnabled() != null || middleware.getFilelogEnabled() != null) {
            return true;
        }
        return false;
    }

    public boolean compareTime(String time) {
        String[] dateTimes = time.split("\\.");
        Date date = DateUtils.parseDate(dateTimes[0], "yyyy-MM-ddHH:mm:ss");
        date = DateUtils.addInteger(date, Calendar.HOUR_OF_DAY, 8);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,-10);
        Date now = calendar.getTime();
        int compareTo = now.compareTo(date);
        if (compareTo == 1) {
            return true;
        }
        return false;
    }

    /**
     * 填充镜像仓库信息
     */
    public void updateRegistry(Middleware middleware, MiddlewareClusterDTO middlewareClusterDTO) {
        Registry registry = middlewareClusterDTO.getRegistry();
        if (StringUtils.isNotEmpty(middleware.getMirrorImageId())) {
            getMirrorImageDTO(middleware.getMirrorImageId(),registry);
        }
        if (middleware.getDynamicValues() != null) {
            if (StringUtils.isNotEmpty(middleware.getDynamicValues().get("mirrorImageId"))) {
                getMirrorImageDTO(middleware.getDynamicValues().get("mirrorImageId"),registry);
            }
        }
    }

    public void getMirrorImageDTO(String mirrorImageId, Registry registry) {
        ImageRepositoryDTO imageRepositoryDTO = imageRepositoryService.detailById(Integer.valueOf(mirrorImageId));
        BeanUtils.copyProperties(imageRepositoryDTO,registry);
        registry.setUser(imageRepositoryDTO.getUsername());
        registry.setChartRepo(imageRepositoryDTO.getProject());
        registry.setPort(imageRepositoryDTO.getPort());
        registry.setAddress(imageRepositoryDTO.getHostAddress());
    }
}
