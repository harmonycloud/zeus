package com.middleware.zeus.service.k8s.abstractService;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.bean.BeanActiveArea;
import com.middleware.zeus.common.constants.NameConstant;
import com.middleware.zeus.common.enums.ComponentsEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.middleware.ResourceUnitEnum;
import com.middleware.zeus.common.exception.CaasRuntimeException;
import com.middleware.zeus.common.model.*;
import com.middleware.zeus.common.model.k8s.PvDo;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.common.model.user.OrganizationDto;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.zeus.dao.BeanActiveAreaMapper;
import com.middleware.zeus.integration.cluster.NamespaceWrapper;
import com.middleware.zeus.integration.cluster.PrometheusWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.MiddlewareCrTypeService;
import com.middleware.zeus.service.middleware.MiddlewareInfoService;
import com.middleware.zeus.service.middleware.MiddlewarePvcService;
import com.middleware.zeus.service.prometheus.PrometheusResourceMonitorService;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.user.OrganizationService;
import com.middleware.zeus.service.user.PlatformQuotaService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.K8sClient;
import com.middleware.zeus.util.RequestUtil;
import com.middleware.zeus.util.ThreadPoolExecutorFactory;
import com.middleware.zeus.util.numeric.MathUtil;
import com.middleware.zeus.util.numeric.ResourceCalculationUtil;
import com.middleware.zeus.util.page.PageUtil;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.CommonConstant.*;
import static com.middleware.zeus.common.constants.CommonConstant.TYPE;
import static com.middleware.zeus.common.constants.NameConstant.*;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.KUBE_SYSTEM;
import static com.middleware.zeus.common.constants.registry.HelmChartConstant.SVG;
import static com.middleware.zeus.common.constants.user.UserConstant.USERNAME;

/**
 * @author xutianhong
 * @Date 2023/3/26 4:59 下午
 */
@Slf4j
public abstract class AbstractClusterService {

    protected static final Map<String, MiddlewareClusterDTO> CLUSTER_MAP = new ConcurrentHashMap<>();
    protected final ReentrantLock lock = new ReentrantLock();

    /**
     * 查询集群列表
     *
     * @return List<MiddlewareClusterDTO>
     */
    protected abstract List<MiddlewareClusterDTO> baseListCluster();

    
    @Autowired
    protected NamespaceService namespaceService;
    @Autowired
    protected MiddlewareCRService middlewareCrService;
    @Autowired
    protected ProjectService projectService;
    @Autowired
    protected NodeService nodeService;
    @Autowired
    protected ClusterCertService clusterCertService;
    @Autowired
    protected ClusterComponentService clusterComponentService;
    @Autowired
    protected PrometheusWrapper prometheusWrapper;
    @Autowired
    protected StorageService storageService;
    @Autowired
    protected OrganizationService organizationService;
    @Autowired
    protected PlatformQuotaService platformQuotaService;
    @Autowired
    private NamespaceWrapper namespaceWrapper;
    @Autowired
    protected MiddlewareCrTypeService middlewareCrTypeService;
    @Autowired
    protected MiddlewareInfoService middlewareInfoService;
    @Autowired
    protected HelmChartService helmChartService;
    @Autowired
    protected PrometheusResourceMonitorService prometheusResourceMonitorService;
    @Autowired
    protected BeanActiveAreaMapper beanActiveAreaMapper;
    @Autowired
    protected MiddlewarePvcService middlewarePvcService;
    @Autowired
    protected UserService userService;
    @Autowired
    protected PvService pvService;


    public List<MiddlewareClusterDTO> listClusters() {
        return listClusters(false, null);
    }


    public List<MiddlewareClusterDTO> listClusters(boolean detail, String key) {
        return listClusters(detail, key, null, null);
    }

    public List<MiddlewareClusterDTO> listClusters(boolean detail, String key, String organId, String projectId) {
        List<MiddlewareClusterDTO> clusters = baseListCluster();
        if (clusters.size() <= 0) {
            return new ArrayList<>(0);
        }
        // 设置集群状态及可用区
        clusters.forEach(this::setActiveActiveInfo);
        // 关键词过滤
        clusters = clusters.stream()
                .filter(clusterDTO -> StringUtils.isEmpty(key) || clusterDTO.getNickname().contains(key))
                .collect(Collectors.toList());
        // 返回命名空间信息
        if (detail && clusters.size() > 0) {
            final CountDownLatch clusterCountDownLatch = new CountDownLatch(clusters.size());
            CurrentUser currentUser = CurrentUserRepository.getUser();
            List<MiddlewareClusterDTO> finalClusters = clusters;
            ThreadPoolExecutorFactory.executor.execute(() -> {
                for (MiddlewareClusterDTO cluster : finalClusters) {
                    try {
                        CurrentUserRepository.setUser(currentUser);
                        initClusterAttributes(cluster);
                        try {
                            List<Namespace> list =
                                    namespaceService.list(cluster.getId(), false, false, false, null, organId, projectId);
                            cluster.getAttributes().put(NS_COUNT, list.size());
                            cluster.setNamespaceList(list);
                        } catch (Exception e) {
                            cluster.getAttributes().put(NS_COUNT, 0);
                            log.error("集群：{}，查询命名空间列表异常", cluster.getId(), e);
                        }
                        // 判断集群是否可删除
                        cluster.setRemovable(checkDelete(cluster.getId()));
                    } catch (Exception ignored) {
                    } finally {
                        clusterCountDownLatch.countDown();
                    }
                }
            });
            try {
                clusterCountDownLatch.await();
            } catch (Exception ignored) {
            }
        }
        List<MiddlewareClusterDTO> res = clusters;
        // 根据项目进行过滤
        if (StringUtils.isNotEmpty(projectId)) {
            Set<String> availableClusterList = projectService.getRelationClusterIds(organId, projectId);
            res = clusters.stream()
                    .filter(cluster -> availableClusterList.stream().anyMatch(ac -> ac.equals(cluster.getId())))
                    .collect(Collectors.toList());
        }
        return res;
    }


    public void initClusterAttributes(List<MiddlewareClusterDTO> clusters) {
        if (CollectionUtils.isEmpty(clusters)) {
            return;
        }
        clusters.parallelStream().forEach(this::initClusterAttributes);
    }


    public void initClusterAttributes(MiddlewareClusterDTO cluster) {
        if (!CollectionUtils.isEmpty(cluster.getAttributes()) && cluster.getAttributes().get(KUBELET_VERSION) != null) {
            return;
        }
        nodeService.setClusterVersion(cluster);
    }

    public List<Namespace> getRegisteredNamespaceNum(List<MiddlewareClusterDTO> clusterDTOList) {
        List<Namespace> namespaces = new ArrayList<>();
        clusterDTOList.forEach(clusterDTO -> {
            namespaces.addAll(getRegisteredNamespaceNum(clusterDTO));
        });
        return namespaces;
    }

    public MiddlewareClusterDTO findById(String clusterId) {
        if (!CLUSTER_MAP.containsKey(clusterId)) {
            List<MiddlewareClusterDTO> clusterList = listClusters().stream()
                    .filter(clusterDTO -> clusterDTO.getId().equals(clusterId)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(clusterList)) {
                throw new CaasRuntimeException(ErrorMessage.CLUSTER_NOT_FOUND);
            }
            MiddlewareClusterDTO dto = clusterList.get(0);
            // 如果accessToken为空，尝试生成token
            if (StringUtils.isBlank(dto.getAccessToken())) {
                clusterCertService.generateTokenByCert(dto);
            }
            CLUSTER_MAP.put(clusterId, SerializationUtils.clone(dto));
        }
        try {
            refresh(clusterId);
        } catch (Exception e){
            log.error("刷新集群信息出现异常", e);
        }
        return CLUSTER_MAP.get(clusterId);
    }

    public MiddlewareClusterDTO findByIdAndCheckRegistry(String clusterId) {
        MiddlewareClusterDTO cluster = findById(clusterId);
        if (cluster.getRegistry() == null || StringUtils.isBlank(cluster.getRegistry().getAddress())) {
            throw new IllegalArgumentException("harbor info is illegal");
        }
        return cluster;
    }

    public ClusterQuotaDTO monitoring(String clusterId) {
        // 计算集群cpu和memory
        MiddlewareClusterDTO cluster = findById(clusterId);
        ClusterComponentsDto componentsDto = clusterComponentService.get(clusterId, "prometheus");
        if (componentsDto != null) {
            clusterResource(cluster);
        }
        return cluster.getClusterQuotaDTO();
    }

    public void clusterResource(MiddlewareClusterDTO cluster) {
        Map<String, String> query = new HashMap<>();
        Map<String, String> resource = new HashMap<>();
        ClusterQuotaDTO clusterQuotaDTO = new ClusterQuotaDTO();
        // 获取cpu总量
        try {
            query.put("query", "sum(count(node_cpu_seconds_total{ mode='system'}) by (kubernetes_pod_node_name))");
            PrometheusResponse cpuTotal = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO.setTotalCpu(Double.parseDouble(cpuTotal.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e) {
            clusterQuotaDTO.setTotalCpu(0);
            log.error("集群查询cpu总量失败");
        }
        // 获取cpu使用量
        try {
            query.put("query",
                    "sum(sum(irate(node_cpu_seconds_total{mode!=\"idle\"}[5m])) by (kubernetes_pod_node_name))");
            PrometheusResponse cpuUsing = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO.setUsedCpu(Double.parseDouble(cpuUsing.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e) {
            clusterQuotaDTO.setUsedCpu(0);
            log.error("集群查询cpu使用量失败");
        }
        // 获取memory总量
        try {
            query.put("query", "sum(node_memory_MemTotal_bytes/1024/1024/1024)");
            PrometheusResponse memoryTotal = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO
                    .setTotalMemory(Double.parseDouble(memoryTotal.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e) {
            clusterQuotaDTO.setTotalMemory(0);
            log.error("集群查询memory总量失败");
        }
        // 获取memory使用量
        try {
            query.put("query",
                    "sum(((node_memory_MemTotal_bytes - node_memory_MemFree_bytes - node_memory_Cached_bytes - node_memory_Buffers_bytes - node_memory_Slab_bytes)/1024/1024/1024))");
            PrometheusResponse memoryUsing = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO
                    .setUsedMemory(Double.parseDouble(memoryUsing.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e) {
            clusterQuotaDTO.setUsedMemory(0);
            log.error("集群查询memory使用量失败");
        }
        cluster.setClusterQuotaDTO(clusterQuotaDTO);
    }

    public ClusterQuotaDTO getClusterQuota(List<MiddlewareClusterDTO> clusterDTOList) {
        ClusterQuotaDTO clusterQuotaSum = new ClusterQuotaDTO();
        clusterDTOList.forEach(clusterDTO -> {
            ClusterQuotaDTO clusterQuota = monitoring(clusterDTO.getId());
            if (clusterQuota != null) {
                clusterQuotaSum.setTotalCpu(clusterQuotaSum.getTotalCpu() + clusterQuota.getTotalCpu());
                clusterQuotaSum.setUsedCpu(clusterQuotaSum.getUsedCpu() + clusterQuota.getUsedCpu());
                clusterQuotaSum.setTotalMemory(clusterQuotaSum.getTotalMemory() + clusterQuota.getTotalMemory());
                clusterQuotaSum.setUsedMemory(clusterQuotaSum.getUsedMemory() + clusterQuota.getUsedMemory());
            }
        });
        clusterQuotaSum
                .setCpuUsedPercent(MathUtil.calcPercent(clusterQuotaSum.getUsedCpu(), clusterQuotaSum.getTotalCpu()));
        clusterQuotaSum.setMemoryUsedPercent(
                MathUtil.calcPercent(clusterQuotaSum.getUsedMemory(), clusterQuotaSum.getTotalMemory()));
        return clusterQuotaSum;
    }

    public List<Namespace> listRegisteredNamespace(String clusterId, String organId, String projectId) {
        if (StringUtils.isEmpty(clusterId)) {
            return Collections.emptyList();
        }
        List<Namespace> namespaces = namespaceService.list(clusterId, false, false, false, null, organId, projectId);
        return namespaces.stream().filter(Namespace::getRegistered).collect(Collectors.toList());
    }

    public ResourceQuotaDo getResourceQuotaInfo(String clusterId, Boolean detail) {
        // 获取节点资源总额
        ResourceQuotaDo nodeQuota = nodeService.getResourceQuota(clusterId);
        // 获取存储总额
        List<StorageDto> storageDtoList = storageService.list(clusterId, null, null, false);
        List<StorageQuota> storageQuotaList =
                storageDtoList.stream().filter(storageDto -> storageDto.getTotalStorage() != null).map(storageDto -> {
                    StorageQuota storageQuota = new StorageQuota();
                    // 设置存储总量
                    QuotaBase storage = new QuotaBase();
                    storage.setRequest(storageDto.getTotalStorage());
                    storageQuota.setName(storageDto.getAliasName());
                    storageQuota.setStorageClass(storageDto.getStorageClassList().stream().map(StorageClassInfo::getName)
                            .collect(Collectors.toList()));
                    storageQuota.setStorageType(storageDto.getStorageClassList().stream()
                            .map(StorageClassInfo::getVolumeType).collect(Collectors.toList()));
                    storageQuota.setStorageId(storageDto.getStorageId());
                    storageQuota.setStorage(storage);
                    return storageQuota;
                }).collect(Collectors.toList());
        // 分配存储总额
        nodeQuota.setStorageList(storageQuotaList);
        // 查询组织分配情况
        if (detail){
            List<OrganizationDto> organizationDtoList = organizationService.list(null);
            List<String> uidList = organizationDtoList.stream().map(OrganizationDto::getOrganId).collect(Collectors.toList());
            List<ResourceQuotaDo> resourceQuotaDoList = platformQuotaService.getQuota(ORGAN, uidList, CPU, MEMORY, STORAGE).stream().filter(rq -> rq.getClusterId().equals(clusterId)).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(resourceQuotaDoList)){
                nodeQuota.setClusterId(clusterId);
                List<ResourceQuotaDo> clusterResourceQuota = new ArrayList<>();
                clusterResourceQuota.add(nodeQuota);
                clusterResourceQuota = platformQuotaService.convertUsedResource(clusterResourceQuota, resourceQuotaDoList);

                nodeQuota = clusterResourceQuota.get(0);
            }
        }
        return nodeQuota;
    }

    public boolean checkWithInCluster(String clusterId) {
        io.fabric8.kubernetes.api.model.Namespace targetNs = namespaceWrapper.get(clusterId, KUBE_SYSTEM);
        io.fabric8.kubernetes.api.model.Namespace defaultNs =
                namespaceWrapper.get(K8sClient.DEFAULT_CLIENT, KUBE_SYSTEM);
        return targetNs.getMetadata().getUid().equals(defaultNs.getMetadata().getUid());
    }

    public Map<String, String> getClusterAliasName() {
        List<MiddlewareClusterDTO> clusterList = this.listClusters();
        if (CollectionUtils.isEmpty(clusterList)) {
            return null;
        }
        return clusterList.stream()
                .collect(Collectors.toMap(MiddlewareClusterDTO::getId, MiddlewareClusterDTO::getNickname));
    }

    public PageInfo<MiddlewareResourceInfo> getMwResource(String clusterId, MiddlewareResourceQueryDto queryDto)
        throws Exception {
        // 获取middleware列表
        List<Middleware> middlewareList = middlewareCrService.list(clusterId, null, null, false);
        // 根据命名空间进行过滤
        if (!CollectionUtils.isEmpty(queryDto.getNamespaceList())) {
            middlewareList = middlewareList.stream()
                .filter(mw -> queryDto.getNamespaceList().stream().anyMatch(ns -> ns.equals(mw.getNamespace())))
                .collect(Collectors.toList());
        } else {
            // 获取命名空间列表
            List<Namespace> namespaceList = namespaceService.list(clusterId);
            // 根据命名空间进行过滤
            middlewareList = middlewareList.stream()
                .filter(mw -> namespaceList.stream().anyMatch(ns -> ns.getName().equals(mw.getNamespace())))
                .collect(Collectors.toList());
        }
        // 根据类型进行过滤
        if (!CollectionUtils.isEmpty(queryDto.getTypeList())) {
            middlewareList = middlewareList.stream()
                .filter(mw -> queryDto.getTypeList().stream().anyMatch(type -> type.equals(mw.getType())))
                .collect(Collectors.toList());
        }

        // 进一步封装middleware信息
        middlewareList = helmChartService.convertMiddlewareList(middlewareList);
        // 对中间件进行关键词过滤
        if (StringUtils.isNotEmpty(queryDto.getKeyword())) {
            middlewareList = middlewareList.stream().filter(
                mw -> mw.getName().contains(queryDto.getKeyword()) || mw.getAliasName().contains(queryDto.getKeyword()))
                .collect(Collectors.toList());
        }

        // 获取中间件监控信息
        List<MiddlewareResourceInfo> middlewareResourceInfoList =
            this.getMwResource(middlewareList, queryDto.getTarget());
        // 对监控数据进行排序筛选
        queryDto.sortMiddlewareResourceInfo(middlewareResourceInfoList);
        // 封装page对象
        return PageUtil.convertPage(middlewareResourceInfoList, queryDto.getCurrent(), queryDto.getSize());
    }

    public List<MiddlewareResourceInfo> getMwResource(List<Middleware> middlewareList, String target) throws Exception {
        List<MiddlewareResourceInfo> mwResourceInfoList = new ArrayList<>();
        if (CollectionUtils.isEmpty(middlewareList)) {
            return mwResourceInfoList;
        }
        // 获取集群id集合
        List<String> clusterList =
            middlewareList.stream().map(Middleware::getClusterId).distinct().collect(Collectors.toList());
        for (String clusterId : clusterList) {
            // 封装pod名称
            StringBuilder pods = new StringBuilder();
            for (Middleware mw : middlewareList) {
                pods.append(getPodName(mw));
            }
            // 封装命名空间
            List<String> nsList =
                middlewareList.stream().map(Middleware::getNamespace).distinct().collect(Collectors.toList());
            StringBuilder namespace = new StringBuilder();
            for (String ns : nsList) {
                namespace.append(ns).append("|");
            }
            // 初始化数据对象
            PrometheusResponse requestResponse = new PrometheusResponse();
            PrometheusResponse usedResponse = new PrometheusResponse();

            if (target.equals(CPU)) {
                // 查询cpu配额
                try {
                    String cpuRequestQuery = "kube_pod_container_resource_requests{resource=\"cpu\",pod=~\"" + pods
                        + "\",namespace=~\"" + namespace + "\"}";
                    requestResponse = prometheusResourceMonitorService.query(clusterId, cpuRequestQuery);
                } catch (Exception e) {
                    log.error("查询中间件cpu配额失败");
                }
                // 查询cpu每5分钟平均用量
                try {
                    String per5MinCpuUsedQuery = "rate(container_cpu_usage_seconds_total{pod=~\"" + pods
                        + "\",namespace=~\"" + namespace + "\",endpoint!=\"\",container!=\"\"}[5m])";
                    usedResponse = prometheusResourceMonitorService.query(clusterId, per5MinCpuUsedQuery);
                } catch (Exception e) {
                    log.error("查询中间件cpu5分钟平均用量失败");
                }

                Map<String, Double> cpuRequestMap =
                    prometheusResourceMonitorService.sumResponseByTarget(requestResponse, POD);
                Map<String, Double> per5MinCpuUsedMap =
                    prometheusResourceMonitorService.sumResponseByTarget(usedResponse, POD);

                for (Middleware mw : middlewareList) {
                    // 设置各服务的cpu使用量
                    MiddlewareResourceInfo mwRsInfo = new MiddlewareResourceInfo(clusterId, mw.getNamespace(),
                        mw.getName(), middlewareCrTypeService.findTypeByCrType(mw.getType()));
                    // 复制 imagePath chartVersion aliasName信息
                    BeanUtils.copyProperties(mw, mwRsInfo);

                    double cpuRequest = 0;
                    double cpuUsed = 0;
                    for (PodInfo podInfo : mw.getPods()) {
                        String key = mw.getNamespace() + LINE + podInfo.getPodName();
                        cpuRequest += cpuRequestMap.getOrDefault(key, 0.0);
                        cpuUsed += per5MinCpuUsedMap.getOrDefault(key, 0.0);
                    }
                    // 设置cpu配额和cpu使用量
                    mwRsInfo.setRequestCpu(
                        ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(cpuRequest), 2, RoundingMode.CEILING));
                    mwRsInfo.setPer5MinCpu(
                        ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(cpuUsed), 2, RoundingMode.CEILING));
                    // 计算cpu使用率
                    if (mwRsInfo.getRequestCpu() != null && mwRsInfo.getRequestCpu() != 0
                        && mwRsInfo.getPer5MinCpu() != null) {
                        double cpuRate = mwRsInfo.getPer5MinCpu() / mwRsInfo.getRequestCpu() * 100;
                        mwRsInfo.setCpuRate(
                            ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(cpuRate), 2, RoundingMode.CEILING));
                    }
                    mwResourceInfoList.add(mwRsInfo);
                }
            }
            if (target.equals(MEMORY)) {
                // 查询memory配额
                try {
                    String memoryRequestQuery = "kube_pod_container_resource_requests{resource=\"memory\",pod=~\""
                        + pods + "\",namespace=~\"" + namespace + "\"}";
                    requestResponse = prometheusResourceMonitorService.query(clusterId, memoryRequestQuery);
                } catch (Exception e) {
                    log.error("查询中间件memory配额失败");
                }
                // 查询memory每5分钟平均用量
                try {
                    String per5MinMemoryUsedQuery = "avg_over_time(container_memory_working_set_bytes{pod=~\"" + pods
                        + "\",namespace=~\"" + namespace + "\",endpoint!=\"\",container!=\"\"}[5m]) /1024/1024/1024";
                    usedResponse = prometheusResourceMonitorService.query(clusterId, per5MinMemoryUsedQuery);
                } catch (Exception e) {
                    log.error("查询中间件memory5分钟平均用量失败");
                }
                Map<String, Double> memoryRequestMap =
                    prometheusResourceMonitorService.sumResponseByTarget(requestResponse, POD);
                Map<String, Double> per5MinMemoryUsedMap =
                    prometheusResourceMonitorService.sumResponseByTarget(usedResponse, POD);

                for (Middleware mw : middlewareList) {
                    // 设置各服务的memory使用量
                    MiddlewareResourceInfo mwRsInfo = new MiddlewareResourceInfo(clusterId, mw.getNamespace(),
                        mw.getName(), middlewareCrTypeService.findTypeByCrType(mw.getType()));
                    // 复制 imagePath chartVersion aliasName信息
                    BeanUtils.copyProperties(mw, mwRsInfo);
                    double memoryRequest = 0;
                    double memoryUsed = 0;
                    for (PodInfo podInfo : mw.getPods()) {
                        String key = mw.getNamespace() + LINE + podInfo.getPodName();
                        memoryRequest += memoryRequestMap.getOrDefault(key, 0.0);
                        memoryUsed += per5MinMemoryUsedMap.getOrDefault(key, 0.0);
                    }
                    // 设置cpu配额和cpu使用量
                    mwRsInfo.setRequestMemory(ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(memoryRequest), 2,
                        RoundingMode.CEILING));
                    mwRsInfo.setPer5MinMemory(
                        ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(memoryUsed), 2, RoundingMode.CEILING));
                    // 计算memory使用率
                    if (mwRsInfo.getRequestMemory() != null && mwRsInfo.getRequestMemory() != 0
                        && mwRsInfo.getPer5MinMemory() != null) {
                        double memoryRate = mwRsInfo.getPer5MinMemory() / mwRsInfo.getRequestMemory() * 100;
                        mwRsInfo.setMemoryRate(ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(memoryRate), 2,
                            RoundingMode.CEILING));
                    }
                    mwResourceInfoList.add(mwRsInfo);
                }
            }

            if (target.equals(STORAGE)) {
                List<PvDo> pvDoList = pvService.listPv(clusterId, null, null);
                pvDoList = pvDoList.stream().filter(pv -> nsList.stream().anyMatch(ns -> ns.equals(pv.getNamespace())))
                    .collect(Collectors.toList());
                StringBuilder pvs = new StringBuilder();
                for (PvDo pv : pvDoList) {
                    pvs.append(pv.getPvName()).append("|");
                }
                // 查询pvc总量
                try {
                    String pvcTotalQuery = "total_size_kb{pv=~\"" + pvs + "\"} /1024/1024";
                    requestResponse = prometheusResourceMonitorService.query(clusterId, pvcTotalQuery);
                } catch (Exception e) {
                    log.error("查询中间件storage总量失败");
                }
                // 查询pvc使用量
                try {
                    String pvcUsedQuery = "used_size_kb{pv=~\"" + pvs + "\"} /1024/1024";
                    usedResponse = prometheusResourceMonitorService.query(clusterId, pvcUsedQuery);
                } catch (Exception e) {
                    log.error("查询中间件storage5分钟平均用量失败");
                }

                // 根据namespace-pvc name 为key，将pvDoList转化为map
                Map<String, String> pvcNameAndPvMap = pvDoList.stream()
                    .collect(Collectors.toMap(pvDo -> pvDo.getNamespace() + LINE + pvDo.getPvcName(), PvDo::getPvName));
                // 整理监控数据
                Map<String, Double> storageRequestMap =
                    prometheusResourceMonitorService.sumResponseByTarget(requestResponse, PV);
                Map<String, Double> storageUsedMap =
                    prometheusResourceMonitorService.sumResponseByTarget(usedResponse, PV);
                for (Middleware mw : middlewareList) {
                    double storageRequest = 0;
                    double storageUsed = 0;
                    for (String pvc : mw.getPvcs()) {
                        String key = mw.getNamespace() + LINE + pvc;

                        String pvName = pvcNameAndPvMap.get(key);
                        if (pvName == null) {
                            continue;
                        }
                        storageRequest += storageRequestMap.getOrDefault(MIDDLEWARE_OPERATOR + LINE + pvName, 0.0);
                        storageUsed += storageUsedMap.getOrDefault(MIDDLEWARE_OPERATOR + LINE + pvName, 0.0);
                    }
                    // 设置各服务的storage使用量
                    MiddlewareResourceInfo mwRsInfo = new MiddlewareResourceInfo(clusterId, mw.getNamespace(),
                        mw.getName(), middlewareCrTypeService.findTypeByCrType(mw.getType()));
                    // 复制 imagePath chartVersion aliasName信息
                    BeanUtils.copyProperties(mw, mwRsInfo);
                    // 设置pvc总量和pvc使用量
                    mwRsInfo.setRequestStorage(ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(storageRequest),
                        2, RoundingMode.CEILING));
                    mwRsInfo.setPer5MinStorage(
                        ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(storageUsed), 2, RoundingMode.CEILING));
                    // 计算pvc使用率
                    if (mwRsInfo.getRequestStorage() != null && mwRsInfo.getRequestStorage() != 0
                        && mwRsInfo.getPer5MinStorage() != null) {
                        double storageRate = mwRsInfo.getPer5MinStorage() / mwRsInfo.getRequestStorage() * 100;
                        mwRsInfo.setStorageRate(ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(storageRate), 2,
                            RoundingMode.CEILING));
                    }
                    mwResourceInfoList.add(mwRsInfo);
                }
            }
            log.info("查询完成，返回数据");
        }
        return mwResourceInfoList;
    }
    
    public Map<String, List<String>> getMwResourceQueryCondition(String clusterId) {
        // 获取middleware列表
        List<Middleware> middlewareList = middlewareCrService.list(clusterId, null, null, false);
        // 获取命名空间列表
        List<Namespace> namespaceList = namespaceService.list(clusterId);
        // 根据命名空间进行过滤
        middlewareList = middlewareList.stream()
                .filter(mw -> namespaceList.stream().anyMatch(ns -> ns.getName().equals(mw.getNamespace())))
                .collect(Collectors.toList());

        Map<String, List<String>> queryCondition = new HashMap<>();
        // 获取命名空间阈值
        List<String> namespaceListStr = namespaceList.stream().map(Namespace::getName).distinct().collect(Collectors.toList());
        queryCondition.put(NAMESPACE, namespaceListStr);
        // 获取中间件类型阈值
        List<String> middlewareTypeListStr = middlewareList.stream().map(Middleware::getType).distinct().collect(Collectors.toList());
        queryCondition.put(TYPE, middlewareTypeListStr);
        return queryCondition;
    }

    public List<ClusterNodeResourceDto> getNodeResource(String clusterId) {
        List<Node> nodeList = nodeService.list(clusterId);
        return nodeService.getNodeResource(clusterId, nodeList, true);
    }

    public PageInfo<ClusterNamespaceResourceDto> getNamespaceResource(String clusterId, String target, String keyword,
        Integer current, Integer size) throws Exception {
        List<Namespace> namespaceList = namespaceService.list(clusterId);
        // 根据name和aliasName进行模糊匹配
        if (StringUtils.isNotEmpty(keyword)) {
            namespaceList = namespaceList.stream().filter(
                ns -> (StringUtils.isNotEmpty(ns.getName()) && StringUtils.containsIgnoreCase(ns.getName(), keyword))
                    || (StringUtils.isNotEmpty(ns.getAliasName())
                        && StringUtils.containsIgnoreCase(ns.getClusterAliasName(), keyword)))
                .collect(Collectors.toList());
        }
        
        PageInfo<Namespace> namespacePageInfo = PageUtil.convertPage(namespaceList, current, size);
        Map<String, String> queryMap = new HashMap<>();

        Map<Map<String, String>, List<String>> cpuRequestResult = new HashMap<>();
        Map<Map<String, String>, List<String>> cpuPer5MinResult = new HashMap<>();
        if (target.equals(CPU)){
            // 查询cpu配额
            String cpuRequestQuery = "sum(container_spec_cpu_quota) by (namespace)/100000";
            queryMap.put("query", cpuRequestQuery);
            PrometheusResponse cpuRequest = prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

            // 查询cpu每5分钟平均用量
            String per5MinCpuUsedQuery = "sum(rate(container_cpu_usage_seconds_total{endpoint!=\"\"}[3m])) by (namespace)";
            queryMap.put("query", per5MinCpuUsedQuery);
            PrometheusResponse per5MinCpuUsed =
                    prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

            cpuRequestResult.putAll(getResultMap(cpuRequest));
            cpuPer5MinResult.putAll(getResultMap(per5MinCpuUsed));
        }

        Map<Map<String, String>, List<String>> memoryRequestResult = new HashMap<>();
        Map<Map<String, String>, List<String>> memoryPer5MinResult = new HashMap<>();
        if (target.equals(MEMORY)){
            // 查询memory配额
            String memoryRequestQuery = "(sum(container_spec_memory_limit_bytes) by (namespace))/1024/1024/1024";
            queryMap.put("query", memoryRequestQuery);
            PrometheusResponse memoryRequest =
                    prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

            // 查询memory每5分钟平均用量
            String per5MinMemoryUsedQuery =
                    "(sum(avg_over_time(container_memory_usage_bytes{endpoint!=\"\"}[5m])) by (namespace))/1024/1024/1024";
            queryMap.put("query", per5MinMemoryUsedQuery);
            PrometheusResponse per5MinMemoryUsed =
                    prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

            memoryRequestResult.putAll(getResultMap(memoryRequest));
            memoryPer5MinResult.putAll(getResultMap(per5MinMemoryUsed));
        }

        Map<Map<String, String>, List<String>> pvcRequestResult = new HashMap<>();
        Map<Map<String, String>, List<String>> pvcPer5MinResult = new HashMap<>();
        if (target.equals(STORAGE)){
            // 查询pvc总量
            String pvcTotalQuery =
                    "sum(kube_persistentvolumeclaim_resource_requests_storage_bytes) by (namespace) /1024/1024/1024";
            queryMap.put("query", pvcTotalQuery);
            PrometheusResponse pvcTotal = prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

            // 查询pvc使用量
            String pvcUsingQuery = "sum(kubelet_volume_stats_used_bytes{endpoint!=\"\"}) by (namespace) /1024/1024/1024";
            queryMap.put("query", pvcUsingQuery);
            PrometheusResponse pvcUsing = prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

            pvcRequestResult.putAll(getResultMap(pvcTotal));
            pvcPer5MinResult.putAll(getResultMap(pvcUsing));
        }
        // 初始化返回数据对象
        PageInfo<ClusterNamespaceResourceDto> clusterNamespaceResourceDtoPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(namespacePageInfo, clusterNamespaceResourceDtoPageInfo, "list");

        List<ClusterNamespaceResourceDto> clusterNamespaceResourceDtoList = new ArrayList<>();
        // 循环遍历namespace 进行数据封装
        for (Namespace namespace : namespacePageInfo.getList()){
            ClusterNamespaceResourceDto nsResource = new ClusterNamespaceResourceDto();
            Map<String, String> nsMap = new HashMap<>();
            nsMap.put(NAMESPACE, namespace.getName());
            // 获取cpu配额
            if (cpuRequestResult.containsKey(nsMap)) {
                nsResource.setCpuRequest(getResourceResult(cpuRequestResult.get(nsMap).get(1)));
            }
            // 获取cpu5分钟平均使用量
            if (cpuPer5MinResult.containsKey(nsMap)) {
                nsResource.setPer5MinCpu(getResourceResult(cpuPer5MinResult.get(nsMap).get(1)));
            }
            // 获取memory配额
            if (memoryRequestResult.containsKey(nsMap)) {
                nsResource.setMemoryRequest(getResourceResult(memoryRequestResult.get(nsMap).get(1)));
            }
            // 获取memory5分钟平均使用量
            if (memoryPer5MinResult.containsKey(nsMap)) {
                nsResource.setPer5MinMemory(getResourceResult((memoryPer5MinResult.get(nsMap).get(1))));
            }
            // 获取pvc总额
            if (pvcRequestResult.containsKey(nsMap)) {
                nsResource.setPvcRequest(getResourceResult(pvcRequestResult.get(nsMap).get(1)));
            }
            // 获取pvc使用量
            if (pvcPer5MinResult.containsKey(nsMap)) {
                nsResource.setPer5MinPvc(getResourceResult((pvcPer5MinResult.get(nsMap).get(1))));
            }
            // 计算cpu使用率
            if (nsResource.getCpuRequest() != null && nsResource.getPer5MinCpu() != null
                    && nsResource.getCpuRequest() != 0) {
                double cpuRate = nsResource.getPer5MinCpu() / nsResource.getCpuRequest() * 100;
                nsResource.setCpuRate(ResourceCalculationUtil.roundNumber2TwoDecimalWithCeiling(cpuRate));
            }
            // 计算memory使用率
            if (nsResource.getMemoryRequest() != null && nsResource.getPer5MinMemory() != null
                    && nsResource.getMemoryRequest() != 0) {
                double memoryRate = nsResource.getPer5MinMemory() / nsResource.getMemoryRequest() * 100;
                nsResource.setMemoryRate(ResourceCalculationUtil.roundNumber2TwoDecimalWithCeiling(memoryRate));
            }
            // 计算pvc使用率
            if (nsResource.getPvcRequest() != null && nsResource.getPer5MinPvc() != null
                    && nsResource.getPvcRequest() != 0) {
                double pvcRate = nsResource.getPer5MinPvc() / nsResource.getPvcRequest() * 100;
                nsResource.setPvcRate(ResourceCalculationUtil.roundNumber2TwoDecimalWithCeiling(pvcRate));
            }
            nsResource.setClusterId(clusterId).setName(namespace.getName());
            clusterNamespaceResourceDtoList.add(nsResource);
        }
        clusterNamespaceResourceDtoPageInfo.setList(clusterNamespaceResourceDtoList);
        return clusterNamespaceResourceDtoPageInfo;
    }

    public Map<Map<String, String>, List<String>> getResultMap(PrometheusResponse response) {
        return response.getData().getResult().stream()
                .collect(Collectors.toMap(PrometheusResult::getMetric, PrometheusResult::getValue));
    }

    public Double getResourceResult(String num) {
        return ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(Double.parseDouble(num)), 2,
                RoundingMode.CEILING);
    }

    /**
     * 判断集群是否可以被删除
     */
    protected boolean checkDelete(String clusterId) {
        try {
            List<MiddlewareCR> middlewareCrList = middlewareCrService.listCR(clusterId, null, null);
            boolean flag = !CollectionUtils.isEmpty(middlewareCrList) && middlewareCrList.stream()
                .anyMatch(middlewareCrd -> !"escluster-kubernetes-logging".equals(middlewareCrd.getMetadata().getName())
                    && !"mysqlcluster-zeus-mysql".equals(middlewareCrd.getMetadata().getName()));
            if (flag) {
                return false;
            }
        } catch (Exception e) {
            log.error("查询middleware失败，默认可以删除");
        }
        return true;
    }

    /**
     * 刷新集群缓存
     */
    private void refresh(String clusterId) {
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                if (lock.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        List<MiddlewareClusterDTO> clusterList = listClusters().stream()
                                .filter(clusterDTO -> clusterDTO.getId().equals(clusterId)).collect(Collectors.toList());
                        if (CollectionUtils.isEmpty(clusterList)) {
                            log.error("刷新集群信息失败，未找到集群:{}", clusterId);
                        }
                        MiddlewareClusterDTO dto = clusterList.get(0);
                        CLUSTER_MAP.put(clusterId, SerializationUtils.clone(dto));
                        try {
                            log.info("集群:{},accessToken:{}",dto.getName(),dto.getAccessToken());
                            log.info("刷新集群信息成功，将静默10s");
                            Thread.sleep(10000);
                            log.info("静默完成，可再次刷新");
                        } catch (InterruptedException e) {
                            log.error("线程休眠异常", e);
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    /**
     * 获取pod名称
     */
    public StringBuilder getPodName(Middleware mw) {
        StringBuilder pods = new StringBuilder();
        List<PodInfo> podInfoList = mw.getPods();
        if (!CollectionUtils.isEmpty(podInfoList)) {
            for (PodInfo podInfo : podInfoList) {
                pods.append(podInfo.getPodName()).append("|");
            }
        }
        return pods;
    }

    /**
     * 获取集群注册的分区
     *
     * @param clusterDTO
     *            集群dto
     * @return
     */
    public List<Namespace> getRegisteredNamespaceNum(MiddlewareClusterDTO clusterDTO) {
        if (clusterDTO == null) {
            return new ArrayList();
        }
        List<Namespace> namespaces = namespaceService.list(clusterDTO.getId(), false, false, false, null, null, null);
        return namespaces.stream().filter(Namespace::getRegistered).collect(Collectors.toList());
    }

    /**
     * 获取集群是否已开启可用区
     * @param clusterDTO
     * @return
     */
    private void setActiveActiveInfo(MiddlewareClusterDTO clusterDTO) {
        QueryWrapper<BeanActiveArea> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("cluster_id", clusterDTO.getId());
        List<BeanActiveArea> areaList = beanActiveAreaMapper.selectList(queryWrapper);
        clusterDTO.setActiveActive(!CollectionUtils.isEmpty(areaList));
        clusterDTO.setActiveAreaNum(areaList.size());
    }

}
