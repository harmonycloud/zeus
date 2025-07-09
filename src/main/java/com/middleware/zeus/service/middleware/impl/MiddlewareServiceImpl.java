package com.middleware.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.bean.BeanCacheMiddleware;
import com.middleware.zeus.bean.BeanClusterMiddlewareInfo;
import com.middleware.zeus.bean.BeanMiddlewareInfo;
import com.middleware.zeus.bean.user.BeanRoleAuthority;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.constants.NameConstant;
import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.middleware.MiddlewareGrafanaNameEnum;
import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.common.enums.middleware.MysqlCharSetEnum;
import com.middleware.zeus.common.enums.middleware.PostgresqlCharSetEnum;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.*;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.common.model.registry.HelmChartFile;
import com.middleware.zeus.common.model.user.UserRole;
import com.middleware.zeus.dao.BeanMiddlewareInfoMapper;
import com.middleware.zeus.integration.cluster.CustomResourceDefinitionWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;
import com.middleware.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.middleware.zeus.operator.BaseOperator;
import com.middleware.zeus.service.AbstractBaseService;
import com.middleware.zeus.service.k8s.IngressService;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.*;
import com.middleware.zeus.service.prometheus.PrometheusResourceMonitorService;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.LicenseService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.service.user.RoleAuthorityService;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.DateUtil;
import com.middleware.zeus.util.RequestUtil;
import com.middleware.zeus.util.ThreadPoolExecutorFactory;
import com.middleware.zeus.util.YamlUtil;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.util.middleware.ChartVersionUtil;
import com.middleware.zeus.util.middleware.MiddlewareModeUtil;
import com.middleware.zeus.util.middleware.MiddlewareResourceCalculateUtil;
import com.middleware.zeus.util.numeric.ResourceCalculationUtil;
import com.skyview.language.annotations.TranslateAfterResult;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.NameConstant.*;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 处理中间件
 */
@Slf4j
@Service
public class MiddlewareServiceImpl extends AbstractBaseService implements MiddlewareService {

    @Value("${system.opsManager.enable:false}")
    private Boolean opsManager;

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
    private UserService userService;
    @Autowired
    private RoleAuthorityService roleAuthorityService;
    @Autowired
    private MiddlewareCrTypeService middlewareCrTypeService;
    @Autowired
    private BeanMiddlewareInfoMapper middlewareInfoMapper;
    @Autowired
    private PvcService pvcService;
    @Autowired
    private ResourceQuotaService resourceQuotaService;
    @Autowired
    private LicenseService licenseService;
    @Autowired
    private ClusterComponentService clusterComponentService;
    @Autowired
    private ServiceAccountService serviceAccountService;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private CustomResourceDefinitionWrapper customResourceDefinitionWrapper;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private MiddlewareConfigYamlService middlewareConfigYamlService;
    @Autowired
    private MiddlewarePvcService middlewarePvcService;
    @Autowired
    private OpsManagerService opsManagerService;
    @Autowired
    private MiddlewareDisableVersionService middlewareDisableVersionService;

    @Value("${system.privateRegistry.middlewareServiceAccount:default}")
    private String middlewareServiceAccount;
    @Value("${system.wrongTypeKeys:tolerations,customVolumes}")
    private String wrongTypeKeys;
    @Value("${active-active.label.key:topology.kubernetes.io/zone}")
    private String zoneKey;

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
        Middleware detail = getOperator(BaseOperator.class, BaseOperator.class, middleware).detail(middleware);
        // 刷新ops manager用户
        if (opsManager && type.equals(MiddlewareTypeEnum.MONGODB.getType())) {
            projectService.getUser(RequestUtil.getOrganId(), RequestUtil.getProjectId(), false);
        }
        return detail;
    }

    @Override
    public Boolean checkExist(String clusterId, String namespace, String name, String type) {
        // check exist
        List<HelmListInfo> helms = helmChartService.listHelm(namespace, null, clusterService.findById(clusterId));
        if (helms.stream().anyMatch(h -> name.equals(h.getName()))) {
            return true;
        }
        // 数据仍未清清除
        if (!ObjectUtils.isEmpty(cacheMiddlewareService.get(clusterId, namespace, type, name))) {
            return true;
        }
        return false;
    }

    @Override
    public SwitchInfo autoSwitch(String clusterId, String namespace, String name, String type) {
        Middleware middleware =
                new Middleware().setClusterId(clusterId).setNamespace(namespace).setType(type).setName(name);
        return getOperator(BaseOperator.class, BaseOperator.class, middleware).getAutoSwitch(middleware);
    }

    @Override
    public SwitchInfo manualSwitch(String clusterId, String namespace, String name, String type) {
        Middleware middleware =
                new Middleware().setClusterId(clusterId).setNamespace(namespace).setType(type).setName(name);
        return getOperator(BaseOperator.class, BaseOperator.class, middleware).getManualSwitch(middleware);
    }


    @Override
    public Middleware create(Middleware middleware) {
        checkBaseParam(middleware);
        checkLicense(middleware.getClusterId());
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        // pre check
        operator.createPreCheck(middleware, cluster);
        updateRegistry(middleware,cluster);
        // set default uid、gid
        setDefaultSecurityContext(middleware);
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
        // 清理ID_MAP
        if (opsManager) {
            opsManagerService.clearIdMap();
        }
        if (result) {
            return middleware;
        }
        return null;
    }

    @Override
    public void recovery(Middleware middleware) {
        checkLicense(middleware.getClusterId());
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
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
        helmChartService.install(middleware, tgzFilePath, cluster, null);
        // 删除数据库缓存
        cacheMiddlewareService.delete(middleware);
    }

    @Override
    public void update(Middleware middleware) {
        checkBaseParam(middleware);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        // pre check
        operator.updatePreCheck(middleware, cluster);
        // update
        operator.update(middleware, cluster);
        // reboot
        if (rebootCheck(middleware)) {
            reboot(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType(), null);
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
    public SwitchInfo switchMiddleware(String clusterId, String namespace, String name, String type, String slaveName, Boolean isAuto, String chartVersion) {
        Middleware middleware = new Middleware(clusterId, namespace, name, type).setAutoSwitch(isAuto).setChartVersion(chartVersion);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        // redis分片内单独切换主从
        if (MiddlewareTypeEnum.REDIS.getType().equals(type)){
            return operator.switchMiddleware(middleware, slaveName);
        }else {
            return operator.switchMiddleware(middleware);
        }

    }

    @Override
    public List<MonitorDto> monitor(String clusterId, String namespace, String name, String type, String chartVersion) {
        // 构建middleware对象
        Middleware middleware = new Middleware(clusterId, namespace, name, type).setChartVersion(chartVersion);
        // 获取面板配置文件名称
        String dashboardName = getDashboardName(middleware);
        // 查询配置文件
        ConfigMap configMap = null;
        try {
            if (dashboardName.equals(name + "-dashboard")) {
                configMap = configMapService.get(clusterId, namespace, dashboardName);
            } else {
                configMap = configMapService.get(clusterId, MIDDLEWARE_OPERATOR, dashboardName);
            }
        } catch (Exception e) {
            log.error("集群{} 分区{} 中间件{} 查询监控面板失败", clusterId, namespace, name, e);
        }

        //获取组件信息
        ClusterComponentsDto grafana = clusterComponentService.get(clusterId, "grafana");

        // 封装数据
        List<MonitorDto> monitorDtoList = new ArrayList<>();
        for (String key : configMap.getData().keySet()) {
            // 初始化数据结构
            MonitorDto monitorDto = new MonitorDto();
            // 将面板转换为json
            JSONObject dashboard = JSONObject.parseObject(configMap.getData().get(key));
            // 获取title
            monitorDto.setTitle(dashboard.getString("title"));
            // 获取uid
            monitorDto.setUid(dashboard.getString("uid"));
            // 获取url
            String url = grafana.getAddress() + "/d/" + monitorDto.getUid() + "/" + middleware.getType()
                    + "?var-namespace=" + middleware.getNamespace() + "&"
                    + MiddlewareGrafanaNameEnum.findByType(middleware.getType()).getName() + "=" + middleware.getName();
            //特殊处理Pgbouncer
            if(monitorDto.getTitle().equals("PgBouncer")){
                monitorDto.setTitle("Pgbouncer");
                url = url + "-pgbouncer";
            }
            monitorDto.setUrl(url);
            monitorDtoList.add(monitorDto);
        }
        // 获取proxy信息
        Boolean withProxy =
            getOperator(BaseOperator.class, BaseOperator.class, middleware).withProxy(clusterId, middleware);
        if (!withProxy) {
            monitorDtoList = monitorDtoList.stream()
                .filter(
                    monitorDto -> !"cBk_ZGpGz1".equals(monitorDto.getUid()) && !"dHlrS1PVz".equals(monitorDto.getUid()))
                .collect(Collectors.toList());
        }
        // 获取pgbouncer信息
        Boolean withPgbouncer =
                getOperator(BaseOperator.class, BaseOperator.class, middleware).withPgbouncer(clusterId, middleware);
        if (!withPgbouncer) {
            monitorDtoList = monitorDtoList.stream()
                    .filter(
                            monitorDto -> !"pgbouncer_00001".equals(monitorDto.getUid()))
                    .collect(Collectors.toList());
        }
        if(monitorDtoList.get(0).getTitle().equals("Pgbouncer")){
            MonitorDto monitorDto = monitorDtoList.get(0);
            monitorDtoList.set(0, monitorDtoList.get(1));
            monitorDtoList.set(1, monitorDto);
        }
        return monitorDtoList;
    }

    @Override
    public void reboot(String clusterId, String namespace, String name, String type, String podType) {
        Middleware middleware =
                new Middleware().setClusterId(clusterId).setNamespace(namespace).setType(type).setName(name);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        operator.reboot(clusterId, namespace, name, type, podType);
    }

    @Override
    public void updateStorage(Middleware middleware) {
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        operator.updateStorage(middleware);
    }

    /**
     * 设置默认uid、gid
     * @param middleware
     */
    private void setDefaultSecurityContext(Middleware middleware) {
        Namespace ns = namespaceService.get(middleware.getClusterId(), middleware.getNamespace());
        ContainerIdentityRange uidRange = ns.getContainerUIDRange();
        if ((middleware.getContainerUID() == null) && (uidRange != null) && (uidRange.getMin() != null)) {
            middleware.setContainerUID(uidRange.getMin());
            ContainerIdentityRange gidRange = ns.getContainerGIDRange();
            if ((gidRange != null) && (gidRange.getMin() != null)) {
                middleware.setContainerGID(gidRange.getMin());
            }
        }
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

    private void checkLicense(String clusterId){
        if (!licenseService.check(clusterId)){
            throw new BusinessException(ErrorMessage.LICENSE_CPU_RESOURCE_NOT_ENOUGH);
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
    public List<MiddlewareBriefInfoDTO> list(String clusterId, String namespace, String type, String keyword, String organId, String projectId)
        throws Exception {
        // get cluster
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 获取中间件chart包信息
        List<BeanMiddlewareInfo> beanMiddlewareInfoList = middlewareInfoService.list(true);
        // 获取helm list
        List<HelmInfoDo> helmInfoDoList = helmChartService.listInstalledValues(clusterId, namespace, true);
        //过滤获取属于中间件的helm release
        List<HelmInfoDo> mwHelmInfoDoList = helmInfoDoList.stream()
            .filter(info -> beanMiddlewareInfoList.stream()
                .anyMatch(mwInfo -> info.getChartName().equals(mwInfo.getChartName()) && info.getChartVersion().equals(mwInfo.getChartVersion())))
            .filter(info -> StringUtils.isEmpty(type) || info.getChartName().contains(type)).collect(Collectors.toList());
        // list middleware cr
        List<Middleware> middlewareList = middlewareCRService.list(clusterId, namespace, type, false);

        // 从middleware list中去除不存在helm release的对象，并针对存在的对象设置chartVersion
        List<Middleware> existMiddlewareList =
         middlewareList.stream().filter(mw -> mwHelmInfoDoList.stream().anyMatch(info -> {
            if (info.getName().equals(mw.getName()) && info.getNamespace().equals(mw.getNamespace())) {
                mw.setChartVersion(info.getChartVersion());
                mw.setValues(info.getValues());
                return true;
            } else {
                return false;
            }
        })).collect(Collectors.toList());
        // 获取还未创建出middleware的release
        List<HelmInfoDo> creatingMwHelmInfoDoList = mwHelmInfoDoList.stream()
            .filter(info -> existMiddlewareList.stream().noneMatch(mw -> mw.getName().equals(info.getName())))
            .collect(Collectors.toList());
        creatingMwHelmInfoDoList.forEach(info -> {
            Middleware middleware =
                new Middleware(clusterId, info.getNamespace(), info.getName(), info.getChartName());
            // 如果超过10分钟未能创建出middleware，则设置为创建失败
            if (info.getUpdateTime().before(DateUtil.addMinute(new Date(), -10))) {
                middleware.setStatus("failed");
            } else {
                middleware.setStatus("Preparing");
            }
            middleware.setValues(info.getValues());
            existMiddlewareList.add(middleware);
        });
        // 获取values.yaml的详情
        final CountDownLatch count = new CountDownLatch(existMiddlewareList.size());
        existMiddlewareList.forEach(mw -> ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                 getOperator(BaseOperator.class, BaseOperator.class, mw).convertByHelmChart(mw, cluster, mw.getValues());
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
                if (!StringUtils.isEmpty(beanCacheMiddleware.getValuesYaml())) {
                    JSONObject values = JSONObject.parseObject(beanCacheMiddleware.getValuesYaml());
                    if (values.containsKey(VERSION)) {
                        middleware.setVersion(values.getString(VERSION));
                    }
                    if (middleware.getType().equals(MiddlewareTypeEnum.REDIS.getType())) {
                        getOperator(BaseOperator.class, BaseOperator.class, middleware).convertByHelmChart(middleware,
                            cluster, values);
                    }
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
                existMiddlewareList.removeIf(mw -> mw.getName().equals(beanCacheMiddleware.getName())
                    && mw.getNamespace().equals(beanCacheMiddleware.getNamespace()));
                existMiddlewareList.add(middleware);
            }
        }

        List<Middleware> result = new ArrayList<>(existMiddlewareList);
        // 关键词过滤
        if (StringUtils.isNotEmpty(keyword)) {
            result = result.stream()
                    .filter(mw -> mw.getName().contains(keyword)
                            || (StringUtils.isNotEmpty(mw.getAliasName()) && mw.getAliasName().contains(keyword)))
                    .collect(Collectors.toList());
        }
        if (StringUtils.isNotEmpty(projectId)) {
            // 根据项目分区进行过滤
            List<Namespace> projectNamespaceList = projectService.getNamespace(organId, projectId);
            result = result.stream()
                .filter(mw -> projectNamespaceList.stream().anyMatch(
                    pn -> pn.getName().equals(mw.getNamespace()) && pn.getClusterId().equals(mw.getClusterId())))
                .collect(Collectors.toList());
            // 根据类型进行过滤
            if (StringUtils.isEmpty(type)) {
                UserRole userRole = userService.getUserRole(CurrentUserRepository.getUser().getUsername(), organId, projectId);
                if (userRole != null && userRole.getRoleId() != null){
                    List<BeanRoleAuthority> power = roleAuthorityService.list(userRole.getRoleId());
                    result = result.stream()
                            .filter(mw -> power.stream()
                                    .anyMatch(ra -> ra.getType().equals(mw.getType()) && !"0000".equals(ra.getPower())))
                            .collect(Collectors.toList());
                }
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
        // 获取过滤掉的版本
        List<MiddlewareDisableVersionDo> middlewareDisableVersionDoList = middlewareDisableVersionService
            .get(middleware.getClusterId(), type, middleware.getChartVersion());
        // 升级服务时，只能升级到当前服务的上一个版本，不能跨版本升级,设置标志变量existNow来判断是否是上一个版本
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
            if (info.getVersion() != null && !CollectionUtils.isEmpty(middlewareDisableVersionDoList)
                && info.getChartVersion().equals(middleware.getChartVersion())) {
                List<String> versionList = Arrays.asList(info.getVersion().split(","));
                versionList = versionList.stream()
                    .filter(version -> middlewareDisableVersionDoList.stream()
                        .noneMatch(disableVersionDo -> disableVersionDo.getVersion().equals(version)))
                    .collect(Collectors.toList());
                // 将version写会以，分隔的字符串
                dto.setVersion(StringUtils.join(versionList, ","));
            }

            return dto;
        }).sorted(((o1, o2) -> ChartVersionUtil.compare(o1.getChartVersion(), o2.getChartVersion())))
            .collect(Collectors.toList());
        return resList;
    }

    @Override
    public BaseResult upgradeChart(String clusterId, String namespace, String name, String type, String chartName, String upgradeChartVersion) {
        HelmChartFile helmChart = helmChartService.getHelmChartFromMysql(chartName, upgradeChartVersion);
        JSONObject upgradeValues = YamlUtil.convertYamlAsNormalJsonObject(helmChart.getValueYaml());
        convertToStandardJsonObject(upgradeValues);

        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        JSONObject currentValues = helmChartService.getInstalledValuesAsNormalJson(name, namespace, cluster);
        convertToStandardJsonObject(currentValues);

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
            resValues.put("image", upgradeImage);
        }else if (type.equals(MiddlewareTypeEnum.POSTGRESQL.getType())){
            // 新版本pg的升级问题
            JSONObject operatorValues = helmChartService.getInstalledValues("postgresql-operator", MIDDLEWARE_OPERATOR, cluster);
            upgradeImage.put("repository", operatorValues.getJSONObject("image").getString("repository"));
            resValues.put("image", upgradeImage);
        }
        resValues.put("chart-version", upgradeChartVersion);
        // 执行升级
        Middleware middleware = new Middleware();
        middleware.setClusterId(clusterId);
        middleware.setChartName(chartName);
        middleware.setChartVersion(upgradeChartVersion);
        middleware.setName(name);
        middleware.setNamespace(namespace);
        middleware.setType(type);
        helmChartService.upgrade(middleware, currentValues, resValues, cluster);
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

    private void convertToStandardJsonObject(JSONObject values) {
        String[] ary = wrongTypeKeys.split(",");
        for (String key : ary) {
            YamlUtil.convertToStandardJsonObject(values, key);
        }
    }

    private void checkAndBindImagePullSecret(String clusterId, String namespace, String registryId) {
        ServiceAccount serviceAccount = serviceAccountService.get(clusterId, namespace, middlewareServiceAccount);
        List<LocalObjectReference> imagePullSecrets = serviceAccount.getImagePullSecrets();
        Secret secret = imageRepositoryService.getImagePullSecret(clusterId, namespace, registryId);
        imagePullSecrets = imagePullSecrets.stream().filter(
                imagePullSecret -> secret != null && imagePullSecret.getName().equals(secret.getMetadata().getName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(imagePullSecrets)) {
            imageRepositoryService.createOrReplaceImagePullSecret(clusterId, namespace, Integer.parseInt(registryId));
            List<Secret> secrets = imageRepositoryService.listImagePullSecret(clusterId, namespace);
            serviceAccountService.bindImagePullSecret(clusterId, namespace, serviceAccount, secrets);
        }
    }

    /**
     * 更新grafanaid
     */
    public String getDashboardName(Middleware middleware) {
        HelmChartFile helm = helmChartService.getHelmChartFromMysql(middleware.getType(), middleware.getChartVersion());
        String alias;
        if (!CollectionUtils.isEmpty(helm.getDependency()) && helm.getDependency().containsKey("alias")) {
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
            if ("postgresql".equals(middleware.getType())){
                alias = alias + "-postgresql";
            }
        }
        return alias + "-dashboard";
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
    @TranslateAfterResult
    public MiddlewareTopologyDTO topology(String clusterId, String namespace, String name, String type) throws Exception {
        Middleware middleware = podService.list(clusterId, namespace, name, type);
        MiddlewareTopologyDTO middlewareTopologyDTO =
            new MiddlewareTopologyDTO().setClusterId(clusterId).setNamespace(namespace).setName(name).setType(type)
                .setStatus(middleware.getStatus()).setPods(middleware.getPods())
                .setPodInfoGroup(middleware.getPodInfoGroup()).setMonitorResourceQuota(new MonitorResourceQuota());
        // 设置 PROVISIONER
        if (!CollectionUtils.isEmpty(middleware.getPods())) {
            loop:
            for (PodInfo podInfo : middleware.getPods()) {
                if (CollectionUtils.isEmpty(podInfo.getStorageResources())) {
                    continue;
                }
                for (MiddlewareQuota scr : podInfo.getStorageResources()) {
                    if (StringUtils.isNotEmpty(scr.getProvisioner())) {
                        middlewareTopologyDTO.setProvisioner(scr.getProvisioner());
                        break loop;
                    }
                }
            }
        }
        // 获取alias name
        JSONObject values = helmChartService.getInstalledValues(name, namespace, clusterService.findById(clusterId));
        middlewareTopologyDTO.setAliasName(values.getOrDefault("aliasName", "").toString());
        // 特殊处理es存储类型
        if (MiddlewareTypeEnum.ELASTIC_SEARCH.getType().equals(type)) {
            Set<String> scSet = new HashSet<>();
            middleware.getPods().forEach(pod -> {
                if (!CollectionUtils.isEmpty(pod.getStorageResources())) {
                    for (MiddlewareQuota sr : pod.getStorageResources()) {
                        if (StringUtils.isNotEmpty(sr.getStorageClassName())) {
                            scSet.add(sr.getStorageClassName());
                        }
                    }
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
            if (MiddlewareTypeEnum.ZOOKEEPER.getType().equals(type)) {
                JSONObject persistence = values.getJSONObject(PERSISTENCE);
                middlewareTopologyDTO.setStorageClassName(persistence.getOrDefault("storageClassName", "").toString());
            } else {
                middlewareTopologyDTO.setStorageClassName(values.getOrDefault("storageClassName", "").toString());
            }
        }
        // 如果此时存储类型依然为空，则可能是分盘目录部署
        if (StringUtils.isEmpty(middlewareTopologyDTO.getStorageClassName())
            && !CollectionUtils.isEmpty(middlewareTopologyDTO.getPods())
            && !CollectionUtils.isEmpty(middlewareTopologyDTO.getPods().get(0).getStorageResources())) {
            middlewareTopologyDTO.setStorageClassName(
                    middlewareTopologyDTO.getPods().get(0).getStorageResources().get(0).getStorageClassName());
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
                String totalCpuQuery = "sum(kube_pod_container_resource_requests{resource=\"cpu\",pod=~\"" + pods.toString()
                    + "\",namespace=\"" + namespace + "\"}) by (pod)";
                PrometheusResponse totalCpu = prometheusResourceMonitorService.query(clusterId, totalCpuQuery);
                Map<String, Double> result = convertResponse(totalCpu);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String podName = podInfo.getPodName();
                    for(String key : result.keySet()){
                        if (key.contains(podName)){
                            podInfo.getMonitorResourceQuota().getCpu().setTotal(result.get(key));
                            break;
                        }
                    }
                });
            } catch (Exception e) {
                log.error("中间件{} 查询total cpu失败", name);
            } finally {
                cd.countDown();
            }
        });
        // 查询used cpu
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                String usedCpuQuery = "sum(rate(container_cpu_usage_seconds_total{pod=~\"" + pods.toString()
                    + "\",namespace=\"" + namespace + "\",endpoint!=\"\",container!=\"\"}[5m])) by (pod)";
                PrometheusResponse usedCpu = prometheusResourceMonitorService.query(clusterId, usedCpuQuery);
                Map<String, Double> result = convertResponse(usedCpu);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String podName = podInfo.getPodName();
                    for(String key : result.keySet()){
                        if (key.contains(podName)){
                            podInfo.getMonitorResourceQuota().getCpu().setUsed(result.get(key));
                            break;
                        }
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
                String totalMemoryQuery = "sum(kube_pod_container_resource_requests{resource=\"memory\",pod=~\""
                    + pods.toString() + "\",namespace=\"" + namespace + "\"}) by (pod) /1024/1024/1024";
                PrometheusResponse totalMemory = prometheusResourceMonitorService.query(clusterId, totalMemoryQuery);
                Map<String, Double> result = convertResponse(totalMemory);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String podName = podInfo.getPodName();
                    for(String key : result.keySet()){
                        if (key.contains(podName)){
                            podInfo.getMonitorResourceQuota().getMemory().setTotal(result.get(key));
                            break;
                        }
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
                    + "\",namespace=\"" + namespace + "\",endpoint!=\"\",container!=\"\"}) by (pod) /1024/1024/1024";
                PrometheusResponse usedMemory = prometheusResourceMonitorService.query(clusterId, usedMemoryQuery);
                Map<String, Double> result = convertResponse(usedMemory);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String podName = podInfo.getPodName();
                    for(String key : result.keySet()){
                        if (key.contains(podName)){
                            podInfo.getMonitorResourceQuota().getMemory().setUsed(result.get(key));
                            break;
                        }
                    }
                });
            } catch (Exception e) {
                log.error("中间件{} 查询used memory配额失败", name);
            } finally {
                cd.countDown();
            }
        });

        List<PersistentVolumeClaim> pvcList = middlewarePvcService.listMiddlewarePvc(clusterId, namespace, name, type);
        StringBuilder pvcs = new StringBuilder();
        for (PersistentVolumeClaim pvc : pvcList) {
            pvcs.append(pvc.getVolumeName()).append("|");
        }
        Map<String,
            String> pvcVolumeMap = pvcList.stream()
                .filter(pvc -> StringUtils.isNotEmpty(pvc.getVolumeName()) && StringUtils.isNotEmpty(pvc.getName()))
                .collect(Collectors.toMap(PersistentVolumeClaim::getName,
                    PersistentVolumeClaim::getVolumeName));
        // 查询total storage
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                String totalStorageQuery = "sum(total_size_kb{pv=~\"" + pvcs.toString() + "\"}) by (pv) /1024/1024";
                PrometheusResponse totalStorage = prometheusResourceMonitorService.query(clusterId, totalStorageQuery);
                Map<String, Double> result = convertResponse(totalStorage);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String podName = podInfo.getPodName();
                    for(String key : pvcVolumeMap.keySet()){
                        if (key.contains(podName) && result.containsKey(pvcVolumeMap.get(key))){
                            podInfo.getMonitorResourceQuota().getStorage().setTotal(result.get(pvcVolumeMap.get(key)));
                            break;
                        }
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
                String usedStorageQuery = "sum(used_size_kb{pv=~\"" + pvcs.toString() + "\"}) by (pv) /1024/1024";
                PrometheusResponse usedStorage = prometheusResourceMonitorService.query(clusterId, usedStorageQuery);
                Map<String, Double> result = convertResponse(usedStorage);
                middlewareTopologyDTO.getPods().forEach(podInfo -> {
                    String podName = podInfo.getPodName();
                    for(String key : pvcVolumeMap.keySet()){
                        if (key.contains(podName) && result.containsKey(pvcVolumeMap.get(key))){
                            podInfo.getMonitorResourceQuota().getStorage().setUsed(result.get(pvcVolumeMap.get(key)));
                            break;
                        }
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
    public List<IngressDTO> listHostNetworkAddress(String clusterId, String namespace, String name, String type) {
        Middleware middleware = new Middleware();
        middleware.setType(type);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        return operator.listHostNetworkAddress(clusterId, namespace, name, type);
    }

    @Override
    public String platform(String clusterId, String namespace, String name, String type) {
        if (!type.equals(MiddlewareTypeEnum.ELASTIC_SEARCH.getType())
                && !type.equals(MiddlewareTypeEnum.KAFKA.getType())
                && !type.equals(MiddlewareTypeEnum.ROCKET_MQ.getType())) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_MANAGER_PLATFORM_NOT_SUPPORT);
        }
        List<IngressDTO> ingressDTOS = ingressService.get(clusterId, namespace, type, name);
        String servicePort = getManagePlatformServicePort(clusterId, namespace, name, type);
        for (IngressDTO ingressDTO : ingressDTOS) {
            // 如果是ingress7层方式暴露
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
            if (CollectionUtils.isEmpty(serviceList)) {
                continue;
            }
            String exposeIp = "";
            if (StringUtils.isNotEmpty(ingressDTO.getIngressClassName())) {
                // 如果服务暴露方式为ingress
                exposeIp = ingressService.getIngressIp(clusterId, ingressDTO.getIngressClassName());
            } else {
                // 如果暴露方式为nodeport
                exposeIp = nodeService.getNodeIp(clusterId);
            }
            for (ServiceDTO serviceDTO : serviceList) {
                if (serviceDTO.getServicePort().equals(servicePort)) {
                    return (StringUtils.isBlank(ingressDTO.getExposeIP()) ? exposeIp : ingressDTO.getExposeIP()) + ":" + serviceDTO.getExposePort();
                }
            }
        }
        return null;
    }

    @Override
    public String getManagePlatformServicePort(String clusterId, String namespace, String name, String type) {
        if (type.equals(MiddlewareTypeEnum.ELASTIC_SEARCH.getType())) {
            JSONObject values = helmChartService.getInstalledValues(name, namespace, clusterService.findById(clusterId));
            if (values.getJSONObject("port") != null && values.getJSONObject("port").containsKey("esKibanaPort")) {
                return values.getJSONObject("port").getString("esKibanaPort");
            } else {
                return "5200";
            }
        } else if (type.equals(MiddlewareTypeEnum.KAFKA.getType())) {
            return "9000";
        } else if (type.equals(MiddlewareTypeEnum.ROCKET_MQ.getType())) {
            return "8080";
        } else {
            return null;
        }
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

    @Override
    public ActiveAreaAnnotationDto getActiveAreaAnnotation(String clusterId, String namespace, String type, String middlewareName) {
        Middleware middleware = new Middleware();
        middleware.setType(type);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        return operator.getActiveAreaAnnotation(clusterId, namespace, type, middlewareName);
    }

    @Override
    public List<K8sResource> getResources(String clusterId, String namespace, String type, String middlewareName) {
        MiddlewareCR cr = middlewareCRService.getCR(clusterId, namespace, type, middlewareName);
        List<K8sResource> resources = new ArrayList<>();
        Map<String, List<MiddlewareInfo>> include = cr.getStatus().getInclude();
        include.forEach((k, v) -> {
            List<String> resourceNameList = v.stream().map(MiddlewareInfo::getName).collect(Collectors.toList());
            resources.add(new K8sResource(k, resourceNameList));
        });
        if (middlewareCrTypeService.isType(type)) {
            String pluralName = customResourceDefinitionWrapper.getCRPluralName(clusterId, middlewareCrTypeService.findByType(type));
            if (StringUtils.isNotEmpty(pluralName)) {
                resources.add(new K8sResource(pluralName, Arrays.asList(middlewareName)));
            }
        }
        // 添加configmap资源
        List<String> cmNameList = middlewareConfigYamlService.nameList(clusterId, namespace, middlewareName, type, null);
        resources.add(new K8sResource("configmaps", cmNameList));

        return resources;
    }

    @Override
    public Boolean middlewareResourceCheck(Middleware middleware) {
        boolean cpu = true;
        boolean memory = true;
        boolean storage = true;
        try {
            ResourceQuotaDo resourceQuotaDo = resourceQuotaService.list(middleware.getClusterId(), middleware.getNamespace());
            Map<String, Double> map = MiddlewareResourceCalculateUtil.middlewareResourceCalculate(middleware);
            if (resourceQuotaDo.getCpu() != null && resourceQuotaDo.getCpu().getRequest() != null && resourceQuotaDo.getCpu().getUsed() != null){
                cpu = map.get(CPU) <= resourceQuotaDo.getCpu().getRequest() - resourceQuotaDo.getCpu().getUsed();
            }
            if (resourceQuotaDo.getCpu() != null && resourceQuotaDo.getCpu().getRequest() != null && resourceQuotaDo.getCpu().getUsed() != null){
                memory = map.get(MEMORY) <= resourceQuotaDo.getMemory().getRequest() - resourceQuotaDo.getMemory().getUsed();
            }
            for (String key : map.keySet()) {
                if (key.equals(CPU) || key.equals(MEMORY)) {
                    continue;
                }
                if (!CollectionUtils.isEmpty(resourceQuotaDo.getStorageList())) {
                    Map<String, QuotaBase> storageQuota = resourceQuotaDo.getStorageList().stream()
                        .collect(Collectors.toMap(StorageQuota::getName, StorageQuota::getStorage));
                    String[] scNames;
                    Double storageRequest = map.get(key);
                    if (key.contains(",")) {
                        storageRequest = storageRequest / 2;
                        scNames = key.split(",");
                    } else {
                        scNames = new String[1];
                        scNames[0] = key;
                    }
                    for (String scName : scNames) {
                        if (storageQuota.containsKey(scName) && storage) {
                            storage = storageRequest <= storageQuota.get(scName).getRequest()
                                - storageQuota.get(scName).getUsed();
                        }
                    }
                }
            }
        } catch (Exception e){
            log.error("检查中间件{} 资源配额情况失败", middleware.getName(), e);
        }
        return cpu && memory && storage;
    }

    @Override
    public Double calculateCpuRequest(Middleware middleware, JSONObject values) {
        return getOperator(BaseOperator.class, BaseOperator.class, middleware).calculateCpuRequest(values);
    }

    @Override
    public List<MiddlewareInfo> listMiddlewarePod(String clusterId, String namespace, String type, String middlewareName) {
        MiddlewareCR middlewareCR = middlewareCRService.getCR(clusterId, namespace, type, middlewareName);
        if (middlewareCR == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middlewareName, ErrorMessage.DOES_NOT_EXIST);
        }
        if (middlewareCR.getStatus() == null || middlewareCR.getStatus().getInclude() == null) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_CLUSTER_STATUS_ABNORMAL);
        }
        return middlewareCR.getStatus().getInclude().get("pods");
    }

    @Override
    public List<MiddlewareInfo> listMiddlewareService(String clusterId, String namespace, String type, String middlewareName) {
        MiddlewareCR middlewareCR = middlewareCRService.getCR(clusterId, namespace, type, middlewareName);
        if (middlewareCR == null || middlewareCR.getStatus() == null || middlewareCR.getStatus().getInclude() == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middlewareName, ErrorMessage.DOES_NOT_EXIST);
        }
        return middlewareCR.getStatus().getInclude().get("services");
    }

    @Override
    public boolean activeActiveMiddlewareCheck(String clusterId, String namespace, String middlewareName, String type) {
        String mode = helmChartService.getMiddlewareMode(middlewareName, namespace, clusterId);
        Boolean activeMode = MiddlewareModeUtil.activeActiveModeCheck(type, mode);
        if (!activeMode) {
            return false;
        }
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        return values != null && values.containsKey("podAntiAffinityTopologKey") && zoneKey.equals(values.getString("podAntiAffinityTopologKey"));
    }

    @Override
    public List<PodInfoGroup> podGroupInfo(String clusterId, String namespace, String middlewareName, String type) {
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, type);
        return getOperator(BaseOperator.class, BaseOperator.class, middleware).podInfoGroup(middleware);
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
                double value = ResourceCalculationUtil.roundNumber(
                    BigDecimal.valueOf(Double.parseDouble(res.getValue().get(1))), 2, RoundingMode.CEILING);
                if (map.containsKey(v)) {
                    map.put(v, map.get(v) + value);
                } else {
                    map.put(v, value);
                }
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
        if (!MiddlewareTypeEnum.POSTGRESQL.getType().equals(middleware.getType())
            && !MiddlewareTypeEnum.MYSQL.getType().equals(middleware.getType())
            && (middleware.getStdoutEnabled() != null || middleware.getFilelogEnabled() != null)) {
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
        String repositoryId = null;
        if (StringUtils.isNotEmpty(middleware.getMirrorImageId())) {
            repositoryId = middleware.getMirrorImageId();
        }
        if (middleware.getDynamicValues() != null) {
            if (StringUtils.isNotEmpty(middleware.getDynamicValues().get("mirrorImageId"))) {
                repositoryId = middleware.getDynamicValues().get("mirrorImageId");
            }
        }
        if (StringUtils.isEmpty(repositoryId)) {
            return;
        }
        middlewareClusterDTO.setRegistry(imageRepositoryService.generateRegistry(repositoryId));
    }

    @Override
    public Map<String,List<String>> getChatSet(String type,String version){
        switch (type){
            case "mysql":
                return MysqlCharSetEnum.getByVersion(version);
            case "postgresql":
                return PostgresqlCharSetEnum.getByVersion(version);
            default:
                return new HashMap<>();
        }
    }
}
