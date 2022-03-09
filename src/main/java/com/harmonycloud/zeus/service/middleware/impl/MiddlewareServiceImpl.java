package com.harmonycloud.zeus.service.middleware.impl;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PODS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.harmonycloud.caas.common.model.MonitorResourceQuota;
import com.harmonycloud.caas.common.model.MonitorResourceQuotaBase;
import com.harmonycloud.caas.common.model.PrometheusResponse;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;
import com.harmonycloud.zeus.service.prometheus.PrometheusResourceMonitorService;
import com.harmonycloud.zeus.util.ChartVersionUtil;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.zeus.bean.BeanCacheMiddleware;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.operator.BaseOperator;
import com.harmonycloud.zeus.service.AbstractBaseService;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.CacheMiddlewareService;
import com.harmonycloud.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.util.YamlUtil;

import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;

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
    private NamespaceService namespaceService;
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

    @Override
    public List<Middleware> simpleList(String clusterId, String namespace, String type, String keyword) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);

        Map<String, String> label = null;
        List<String> nameList = new ArrayList<>();
        boolean nameFilter = false;
        if (StringUtils.isNotEmpty(type)){
            if (MiddlewareTypeEnum.isType(type)){
                label = new HashMap<>(1);
                label.put("type", MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType());
            }
            else {
                nameList = getNameList(clusterId, namespace, type);
                nameFilter = true;
            }
        }
        List<MiddlewareCRD> mwList = middlewareCRService.listCR(clusterId, namespace, label);
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
            String middlewareType = MiddlewareTypeEnum.findTypeByCrdType(mw.getSpec().getType());
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
    public String create(Middleware middleware) {
        checkBaseParam(middleware);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        // pre check
        operator.createPreCheck(middleware, cluster);
        // create
        operator.create(middleware, cluster);
        // 查看middleware有没有创建出来
        boolean result = false;
        for (int i = 0; i < (60 * 10 ) && !result; i++) {
            Middleware mw = middlewareCRService.simpleDetail(middleware.getClusterId(), middleware.getNamespace(),
                    middleware.getType(), middleware.getName());
            if (mw != null) {
                result = true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (result) {
            return middleware.getName();
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

    @Override
    public void reboot(String clusterId, String namespace, String name, String type) {
        try {
            MiddlewareCRD mw = middlewareCRService.getCR(clusterId, namespace, type, name);
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
    public List<MiddlewareBriefInfoDTO> getMiddlewareBriefInfoList(List<MiddlewareClusterDTO> clusterDTOList) {
        // 从数据库查询集群已安装的所有中间件版本、图片路径等基础信息
        List<BeanMiddlewareInfo> middlewareInfoList = middlewareInfoService.listInstalledByClusters(clusterDTOList);
        List<MiddlewareBriefInfoDTO> middlewareBriefInfoDTOList = new ArrayList<>();
        // 查询集群内创建的所有中间件CR信息
        List<Middleware> middlewares = queryAllClusterService(clusterDTOList);
        middlewareInfoList.forEach(middlewareInfo -> {
            AtomicInteger serviceNum = new AtomicInteger();
            AtomicInteger errServiceNum = new AtomicInteger();
            MiddlewareBriefInfoDTO middlewareBriefInfoDTO = new MiddlewareBriefInfoDTO();
            countServiceNum(middlewareInfo.getClusterId(), middlewares, middlewareInfo.getChartName(), serviceNum, errServiceNum);
            middlewareBriefInfoDTO.setName(middlewareInfo.getName());
            middlewareBriefInfoDTO.setChartName(middlewareInfo.getChartName());
            middlewareBriefInfoDTO.setImagePath(middlewareInfo.getImagePath());
            middlewareBriefInfoDTO.setServiceNum(serviceNum.get());
            middlewareBriefInfoDTO.setErrServiceNum(errServiceNum.get());
            middlewareBriefInfoDTO.setAliasName(MiddlewareOfficialNameEnum.findByMiddlewareName(middlewareInfo.getChartName()));
            middlewareBriefInfoDTOList.add(middlewareBriefInfoDTO);
        });
        try {
            Collections.sort(middlewareBriefInfoDTOList, new MiddlewareBriefInfoDTOComparator());
        } catch (Exception e) {
            log.error("对服务排序出错了", e);
        }
        return middlewareBriefInfoDTOList;
    }

    @Override
    public List<ResourceMenuDto> listAllMiddlewareAsMenu(String clusterId) {
        List<ResourceMenuDto> subMenuList = new ArrayList<>();
        try {
            List<BeanClusterMiddlewareInfo> middlewareInfos = clusterMiddlewareInfoService.list(clusterId, false);
            if (CollectionUtils.isEmpty(middlewareInfos)) {
                return subMenuList;
            }
            AtomicInteger weight = new AtomicInteger(1);
            for (BeanClusterMiddlewareInfo middlewareInfoDTO : middlewareInfos) {
                if (middlewareInfoDTO.getStatus() == 2) {
                    continue;
                }
                ResourceMenuDto resourceMenuDto = new ResourceMenuDto();
                resourceMenuDto.setName(middlewareInfoDTO.getChartName());
                resourceMenuDto.setAliasName(middlewareInfoDTO.getChartName());
                resourceMenuDto.setAvailable(true);
                resourceMenuDto.setWeight(weight.get());
                weight.getAndIncrement();
                subMenuList.add(resourceMenuDto);
            }
        } catch (Exception e) {
            log.error("查询服务列表错误", e);
        }
        return subMenuList;
    }

    @Override
    public List<MiddlewareBriefInfoDTO> list(String clusterId, String namespace, String type, String keyword) {
        // 获取中间件chart包信息
        List<BeanMiddlewareInfo> beanMiddlewareInfoList = middlewareInfoService.list(true);
        // get cluster
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // helm list 并过滤获取属于中间件的发布
        List<HelmListInfo> helmListInfoList =
            helmChartService.listHelm(namespace, null, cluster).stream()
                .filter(info -> beanMiddlewareInfoList.stream()
                    .anyMatch(mwInfo -> info.getChart().equals(mwInfo.getChartName() + "-" + mwInfo.getChartVersion())))
                .collect(Collectors.toList());
        // 过滤获取指定类型
        if (StringUtils.isNotEmpty(type)) {
            helmListInfoList =
                helmListInfoList.stream().filter(info -> info.getChart().contains(type)).collect(Collectors.toList());
        }
        // list middleware cr
        List<Middleware> middlewareList = middlewareCRService.list(clusterId, namespace, type, false);

        List<HelmListInfo> finalHelmListInfoList = helmListInfoList;
        // 过滤掉helm中没有的middleware
        middlewareList = middlewareList.stream().filter(mw -> finalHelmListInfoList.stream().anyMatch(info -> info.getName().equals(mw.getName()))).collect(Collectors.toList());
        // 获取还未创建出middleware的release
        List<Middleware> finalMiddlewareList = middlewareList;
        helmListInfoList = helmListInfoList.stream().filter(info -> finalMiddlewareList.stream().noneMatch(mw -> mw.getName().equals(info.getName()))).collect(Collectors.toList());
        helmListInfoList.forEach(info -> {
            Middleware middleware = new Middleware(clusterId, namespace, info.getName(), info.getChart().split("-")[0]);
            middleware.setStatus("Preparing");
            finalMiddlewareList.add(middleware);
        });
        finalMiddlewareList.stream().forEach(middleware -> {
            if ("Preparing".equals(middleware.getStatus())) {
                if (compareTime(middleware.getCreateTime())) {
                    middleware.setStatus("failed");
                }
            }
        });
        // 获取values.yaml的详情
        finalMiddlewareList.forEach(mw -> mw = getOperator(BaseOperator.class, BaseOperator.class, mw).convertByHelmChart(mw, cluster));
        // 获取未完全删除的中间件
        if (StringUtils.isNotBlank(type)) {
            List<BeanCacheMiddleware> beanCacheMiddlewareList = cacheMiddlewareService.list(clusterId, namespace, type);
            for (BeanCacheMiddleware beanCacheMiddleware : beanCacheMiddlewareList) {
                Middleware middleware = new Middleware();
                BeanUtils.copyProperties(beanCacheMiddleware, middleware);
                middleware.setStatus("Deleted");
                // 先移除可能因为异步导致残留的原中间件信息
                finalMiddlewareList.add(middleware);
            }
        }
        finalMiddlewareList.sort(new MiddlewareComparator());
        // 封装数据
        List<MiddlewareBriefInfoDTO> list = new ArrayList<>();
        Map<String, String> middlewareImagePathMap = middlewareInfoService.filter(beanMiddlewareInfoList).stream()
            .collect(Collectors.toMap(BeanMiddlewareInfo::getChartName, BeanMiddlewareInfo::getImagePath));
        Map<String, List<Middleware>> middlewareListMap =
            finalMiddlewareList.stream().collect(Collectors.groupingBy(Middleware::getType));
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
        if (currentImage.getString("repository") != null) {
            upgradeImage.put("repository", currentImage.getString("repository"));
        }
        JSONObject resImage = YamlUtil.jsonMerge(upgradeImage, currentImage);

        JSONObject resValues = YamlUtil.jsonMerge(currentValues, upgradeValues);
        resValues.put("image", resImage);
        resValues.put("chart-version", upgradeChartVersion);
        // 执行升级
        Middleware middleware = detail(clusterId, namespace, name, type);
        middleware.setChartName(chartName);
        helmChartService.upgrade(middleware, currentValues, upgradeValues, clusterService.findById(clusterId));
        return BaseResult.ok();
    }

    @Override
    public BaseResult upgradeCheck(String clusterId, String namespace, String name, String type, String chartName, String upgradeChartVersion) {
        HelmChartFile helmChart = helmChartService.getHelmChartFromMysql(chartName, upgradeChartVersion);
        JSONObject upgradeValues = YamlUtil.convertYamlAsNormalJsonObject(helmChart.getValueYaml());
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

    /**
     * 查询集群所有已发布的中间件
     * @param clusterDTOList 集群列表
     * @return
     */
    @Override
    public List<Middleware> queryAllClusterService(List<MiddlewareClusterDTO> clusterDTOList) {
        List<Namespace> namespaceList = new ArrayList<>();
        clusterDTOList.forEach(cluster -> {
            namespaceList.addAll(namespaceService.list(cluster.getId(), false, null));
        });
        List<Middleware> middlewareServiceList = new ArrayList<>();
        namespaceList.forEach(namespace -> {
            middlewareServiceList.addAll(simpleList(namespace.getClusterId(), namespace.getName(), null, ""));
        });
        return middlewareServiceList;
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
                    + "\",namespace=\"" + namespace + "\"}[5m])) by (pod)";
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
                    + "\",namespace=\"" + namespace + "\"}) by (pod) /1024/1024/1024";
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
                        + "\"}) by (persistentvolumeclaim) /1024/1024/1024";
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
        if (!current.equals(upgrade)) {
            return false;
        }
        return true;
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

    public boolean compareTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,-10);
        Date now = calendar.getTime();
        int compareTo = now.compareTo(date);
        if (compareTo == 1) {
            return true;
        }
        return false;
    }

}
