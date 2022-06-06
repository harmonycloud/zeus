package com.harmonycloud.zeus.service.k8s.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.*;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PERSISTENT_VOLUME_CLAIMS;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PODS;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.zeus.service.middleware.ImageRepositoryService;
import com.harmonycloud.zeus.service.prometheus.PrometheusResourceMonitorService;
import com.harmonycloud.zeus.service.user.ProjectService;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorCodeMessage;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.enums.middleware.ResourceUnitEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.*;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;
import com.harmonycloud.zeus.integration.cluster.PrometheusWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.service.registry.RegistryService;
import com.harmonycloud.zeus.service.user.ClusterRoleService;
import com.harmonycloud.zeus.util.K8sClient;
import com.harmonycloud.zeus.util.MathUtil;
import com.harmonycloud.zeus.util.YamlUtil;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Slf4j
@Service
public class ClusterServiceImpl implements ClusterService {

    private static final Map<String, MiddlewareClusterDTO> CLUSTER_MAP = new ConcurrentHashMap<>();
    private static boolean run = true;
    @Value("${system.upload.path:/usr/local/zeus-pv/upload}")
    private String uploadPath;

    @Autowired
    private MiddlewareClusterService middlewareClusterService;
    @Autowired
    private ClusterCertService clusterCertService;
    @Autowired
    private K8sClient k8sClient;
    @Autowired
    private RegistryService registryService;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private K8sDefaultClusterService k8SDefaultClusterService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;
    @Autowired
    private PrometheusWrapper prometheusWrapper;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private ClusterRoleService clusterRoleService;
    @Autowired
    private PrometheusResourceMonitorService prometheusResourceMonitorService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private ClusterComponentService clusterComponentService;
    @Autowired
    private IngressComponentService ingressComponentService;
    @Autowired
    private ImageRepositoryService imageRepositoryService;
    @Autowired
    private ProjectService projectService;

    @Value("${k8s.component.middleware:/usr/local/zeus-pv/middleware}")
    private String middlewarePath;
    @Value("${k8s.component.crd:/usr/local/zeus-pv/components/platform/crds/middlewarecluster-crd.yaml}")
    private String middlewareCrdYamlPath;


    @Override
    public List<MiddlewareClusterDTO> listClusters() {
        return listClusters(false, null);
    }

    @Override
    public List<MiddlewareClusterDTO> listClusters(boolean detail, String key) {
        return listClusters(detail, key, null);
    }

    @Override
    public List<MiddlewareClusterDTO> listClusters(boolean detail, String key, String projectId) {
        List<MiddlewareClusterDTO> clusters;
        List<MiddlewareCluster> clusterList = middlewareClusterService.listClusters();
        if (clusterList.size() <= 0) {
            return new ArrayList<>(0);
        }
        // 封装数据
        clusters = clusterList.stream().map(c -> {
            MiddlewareClusterInfo info = c.getSpec().getInfo();
            MiddlewareClusterDTO cluster = new MiddlewareClusterDTO();
            BeanUtils.copyProperties(info, cluster);
            cluster.setId(K8sClient.getClusterId(c.getMetadata())).setHost(info.getAddress())
                    .setName(c.getMetadata().getName()).setDcId(c.getMetadata().getNamespace())
                    .setIngressList(info.getIngressList()).setAnnotations(c.getMetadata().getAnnotations());
            if (!CollectionUtils.isEmpty(c.getMetadata().getAnnotations())) {
                cluster.setNickname(c.getMetadata().getAnnotations().get(NAME));
            }
            JSONObject attributes = new JSONObject();
            attributes.put(CREATE_TIME, DateUtils.parseUTCDate(c.getMetadata().getCreationTimestamp()));
            cluster.setAttributes(attributes);

            return SerializationUtils.clone(cluster);
        }).collect(Collectors.toList());
        if (StringUtils.isNotEmpty(key)){
            clusters = clusters.stream().filter(clusterDTO -> clusterDTO.getNickname().contains(key))
                    .collect(Collectors.toList());
        }
        // 返回命名空间信息
        if (detail && clusters.size() > 0) {
            clusters.parallelStream().forEach(cluster -> {
                // 初始化集群信息
                initClusterAttributes(cluster);
                try {
                    List<Namespace> list = namespaceService.list(cluster.getId(), false, false, false, null, projectId);
                    cluster.getAttributes().put(NS_COUNT, list.size());
                    cluster.setNamespaceList(list);
                } catch (Exception e) {
                    cluster.getAttributes().put(NS_COUNT, 0);
                    log.error("集群：{}，查询命名空间列表异常", cluster.getId(), e);
                }
                //计算集群cpu和memory
                if (cluster.getMonitor() != null && cluster.getMonitor().getPrometheus() != null){
                    clusterResource(cluster);
                }
                //判断集群是否可删除
                cluster.setRemovable(checkDelete(cluster.getId()));
            });
        }
        // 根据项目进行过滤
        if (StringUtils.isNotEmpty(projectId)) {
            List<String> availableClusterList = projectService.getClusters(projectId);
            clusters = clusters.stream()
                .filter(cluster -> availableClusterList.stream().anyMatch(ac -> ac.equals(cluster.getId())))
                .collect(Collectors.toList());
        }
        return clusters;
    }

    @Override
    public void initClusterAttributes(List<MiddlewareClusterDTO> clusters) {
        if (CollectionUtils.isEmpty(clusters)) {
            return;
        }
        clusters.parallelStream().forEach(this::initClusterAttributes);
    }

    @Override
    public void initClusterAttributes(MiddlewareClusterDTO cluster) {
        if (!CollectionUtils.isEmpty(cluster.getAttributes()) && cluster.getAttributes().get(KUBELET_VERSION) != null) {
            return;
        }
        nodeService.setClusterVersion(cluster);
    }


    @Override
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
        refresh(clusterId);
        return CLUSTER_MAP.get(clusterId);
    }

    @Override
    public MiddlewareClusterDTO detail(String clusterId) {
        List<MiddlewareClusterDTO> clusterList = listClusters(true, null).stream()
            .filter(clusterDTO -> clusterDTO.getId().equals(clusterId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(clusterList)) {
            throw new CaasRuntimeException(ErrorMessage.CLUSTER_NOT_FOUND);
        }
        return clusterList.get(0);
    }

    @Override
    public MiddlewareClusterDTO findByIdAndCheckRegistry(String clusterId) {
        MiddlewareClusterDTO cluster = findById(clusterId);
        if (cluster.getRegistry() == null || StringUtils.isBlank(cluster.getRegistry().getAddress())) {
            throw new IllegalArgumentException("harbor info is illegal");
        }
        return cluster;
    }

    @Override
    public void addCluster(MiddlewareClusterDTO cluster) {
        // 校验集群基本信息参数
        if (cluster == null) {
            throw new IllegalArgumentException("cluster base info is null");
        }
        // 校验集群基本信息
        checkParams(cluster);
        // 校验集群是否已存在
        checkClusterExistent(cluster, false);
        cluster.setId(K8sClient.getClusterId(cluster));
        // 设置证书信息
        clusterCertService.setCertByAdminConf(cluster.getCert());

        // 校验registry
        registryService.validate(cluster.getRegistry());

        try {
            // 先添加fabric8客户端，否则无法用fabric8调用APIServer
            k8sClient.addK8sClient(cluster, false);
            K8sClient.getClient(cluster.getId()).namespaces().withName(DEFAULT).get();
        } catch (CaasRuntimeException ignore) {
        } catch (Exception e) {
            log.error("集群：{}，校验基本信息异常", cluster.getName(), e);
            // 移除fabric8客户端
            K8sClient.removeClient(cluster.getId());
            throw new BusinessException(DictEnum.CLUSTER, cluster.getName(), ErrorMessage.AUTH_FAILED);
        }
        // 保存证书
        try {
            clusterCertService.saveCert(cluster);
            // 若为第一个集群 则将clusterId, url, serviceAccount存入数据库
            if (k8SDefaultClusterService.get() == null) {
                k8SDefaultClusterService.create(cluster);
            }
        } catch (Exception e) {
            log.error("集群{}，保存证书异常", cluster.getId(), e);
        }
        //发布middlewareCluster的crd
        try {
            createMiddlewareCrd(cluster);
        } catch (Exception e){
            k8SDefaultClusterService.delete(cluster.getId());
            throw new BusinessException(ErrorMessage.MIDDLEWARE_CONTROLLER_INSTALL_FAILED);
        }
        // 保存集群
        MiddlewareCluster mw = convert(cluster);
        try {
            middlewareClusterService.create(cluster.getId(), mw);
            JSONObject attributes = new JSONObject();
            attributes.put(CREATE_TIME, new Date());
            cluster.setAttributes(attributes);
            CLUSTER_MAP.put(cluster.getId(), cluster);
        } catch (Exception e) {
            log.error("集群id：{}，添加集群异常", cluster.getId());
            throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.ADD_FAIL);
        }
        // 将镜像仓库信息存进数据库
        insertMysqlImageRepository(cluster);
        // 将chart包存进数据库
        insertMysqlChart(cluster.getId());
        // 判断middleware-operator分区是否存在，不存在则创建
        synchronized (this) {
            List<Namespace> namespaceList = namespaceService.list(cluster.getId(), false, "middleware-operator");
            // 检验分区是否存在
            if (CollectionUtils.isEmpty(namespaceList)) {
                Map<String, String> label = new HashMap<>();
                label.put("middleware", "middleware");
                namespaceService.save(cluster.getId(), "middleware-operator", label, null);
            }
        }
    }

    @Override
    public void updateCluster(MiddlewareClusterDTO cluster) {
        // 校验集群基本信息参数
        if (StringUtils.isAnyEmpty(cluster.getNickname())) {
            throw new IllegalArgumentException("cluster nickname is null");
        }
        checkParams(cluster);

        // 校验集群基本信息
        // 校验集群是否已存在
        MiddlewareClusterDTO oldCluster = findById(cluster.getId());
        if (oldCluster == null) {
            throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.NOT_EXIST);
        }
        checkClusterExistent(cluster, true);
        // 设置证书信息
        clusterCertService.setCertByAdminConf(cluster.getCert());
        k8sClient.updateK8sClient(cluster);

        // 校验registry
        registryService.validate(cluster.getRegistry());

        // 校验es（包含重置es客户端）
        /*if (StringUtils.isNotBlank(cluster.getLogging().getElasticSearch().getHost())
            && (!esComponentService.checkEsConnection(cluster) || esComponentService.resetEsClient(cluster) == null)) {
            throw new BusinessException(DictEnum.ES_COMPONENT, cluster.getLogging().getElasticSearch().getAddress(),
                ErrorMessage.VALIDATE_FAILED);
        }*/

        // 只修改昵称，证书，ingress，制品服务，es
        oldCluster.setNickname(cluster.getNickname());
        oldCluster.setCert(cluster.getCert());
        oldCluster.setIngressList(cluster.getIngressList());
        oldCluster.setRegistry(cluster.getRegistry());
        oldCluster.setLogging(cluster.getLogging());

        update(oldCluster);
        // 修改镜像仓库信息
        updateMysqlImageRepository(cluster);
    }

    @Override
    public void update(MiddlewareClusterDTO cluster) {
        try {
            middlewareClusterService.update(cluster.getId(), convert(cluster));
            if (CLUSTER_MAP.containsKey(cluster.getId())){
                CLUSTER_MAP.put(cluster.getId(), cluster);
            }
        } catch (Exception e) {
            log.error("集群{}的accessToken更新失败", cluster.getId());
            throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.UPDATE_FAIL);
        }
    }

    private void checkParams(MiddlewareClusterDTO cluster) {
        if (cluster.getCert() == null || StringUtils.isEmpty(cluster.getCert().getCertificate())) {
            throw new IllegalArgumentException("cluster cert info is null");
        }

        // 校验集群使用的制品服务参数
        Registry registry = cluster.getRegistry();
        if (registry == null || StringUtils.isAnyEmpty(registry.getProtocol(), registry.getAddress(),
            registry.getChartRepo(), registry.getUser(), registry.getPassword())) {
            registry = new Registry();
            registry.setAddress("middleware.harmonycloud.cn").setProtocol("http").setPort(38080).setUser("admin")
                .setPassword("Hc@Cloud01").setType("harbor").setChartRepo("middleware");
            cluster.setRegistry(registry);
        }
        // 设置默认参数
        // 如果没有数据中心，默认用default命名空间
        if (StringUtils.isBlank(cluster.getDcId())) {
            cluster.setDcId(DEFAULT);
        }
        
        // 设置es信息
        if (cluster.getLogging() == null) {
            cluster.setLogging(new MiddlewareClusterLogging());
        }
        if (cluster.getLogging().getElasticSearch() == null) {
            cluster.getLogging().setElasticSearch(new MiddlewareClusterLoggingInfo());
        }
        
        // 初始化存储相关内容
        if (cluster.getStorage() == null) {
            cluster.setStorage(new MiddlewareClusterStorage());
        }
        //cluster.getStorage().computeIfAbsent(SUPPORT, k -> new HashMap<String, Object>());
    }

    @Override
    public void removeCluster(String clusterId) {
        if (!checkDelete(clusterId)){
            throw new BusinessException(ErrorMessage.CLUSTER_NOT_EMPTY);
        }
        MiddlewareClusterDTO cluster = this.findById(clusterId);
        if (cluster == null) {
            return;
        }
        try {
            middlewareClusterService.delete(clusterId);
        } catch (Exception e) {
            log.error("集群id：{}，删除集群异常", clusterId, e);
            throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.DELETE_FAIL);
        }
        // 从map中移除
        k8SDefaultClusterService.delete(clusterId);
        CLUSTER_MAP.remove(clusterId);
        // 关联数据库信息删除
        bindResourceDelete(cluster);
        // 移除镜像仓库信息
        imageRepositoryService.removeImageRepository(clusterId);
    }

    public void bindResourceDelete(MiddlewareClusterDTO cluster){
        // 删除集群组件信息
        clusterComponentService.delete(cluster.getId());
        // 删除ingress信息
        ingressComponentService.delete(cluster.getId());
    }

    private void checkClusterExistent(MiddlewareClusterDTO cluster, boolean expectExisting) {
        // 获取已有集群信息
        List<MiddlewareClusterDTO> clusterList = new ArrayList<>();
        try {
            clusterList.addAll(listClusters());
        } catch (Exception e){
        }
        // 校验内存中集群信息
        if (expectExisting) {
            // 期望集群存在 && 实际不存在
            if (clusterList.stream().noneMatch(clusterDTO -> clusterDTO.getId().equals(cluster.getId()))) {
                throw new BusinessException(DictEnum.CLUSTER, cluster.getName(), ErrorMessage.NOT_EXIST);
            }
            // 如果nickname重名
            if (clusterList.stream()
                .anyMatch(c -> !c.getId().equals(cluster.getId()) && c.getNickname().equals(cluster.getNickname()))) {
                throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.EXIST);
            }
        } else {
            // 获取所有集群
            for (MiddlewareClusterDTO c : clusterList) {
                // 集群名称
                if (c.getId().equals(cluster.getId())) {
                    throw new BusinessException(DictEnum.CLUSTER, cluster.getName(), ErrorMessage.EXIST);
                }
                // 集群昵称
                if (c.getNickname().equals(cluster.getNickname())) {
                    throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.EXIST);
                }
                // APIServer地址
                if (c.getHost().equals(cluster.getHost())) {
                    throw new BusinessException(DictEnum.CLUSTER, cluster.getAddress(), ErrorMessage.EXIST);
                }
            }
        }
    }

    /**
     * 判断集群是否可以被删除
     */
    public boolean checkDelete(String clusterId) {
        try {
            List<MiddlewareCR> middlewareCRList = middlewareCRService.listCR(clusterId, null, null);
            if (!CollectionUtils.isEmpty(middlewareCRList) && middlewareCRList.stream().anyMatch(
                middlewareCRD -> !"escluster-middleware-elasticsearch".equals(middlewareCRD.getMetadata().getName())
                    && !"mysqlcluster-zeus-mysql".equals(middlewareCRD.getMetadata().getName()))) {
                return false;
            }
        } catch (Exception e) {
            log.error("查询middleware失败，默认可以删除");
        }
        return true;
    }

    private MiddlewareCluster convert(MiddlewareClusterDTO cluster) {
        ObjectMeta meta = new ObjectMeta();
        meta.setName(cluster.getName());
        meta.setNamespace(cluster.getDcId());
        Map<String, String> annotations = new HashMap<>();
        annotations.put(NAME, cluster.getNickname());
        meta.setAnnotations(annotations);
        MiddlewareClusterInfo clusterInfo = new MiddlewareClusterInfo();
        BeanUtils.copyProperties(cluster, clusterInfo);
        clusterInfo.setAddress(cluster.getHost());
        return new MiddlewareCluster().setMetadata(meta).setSpec(new MiddlewareClusterSpec().setInfo(clusterInfo));
    }

    public void insertMysqlChart(String clusterId) {
        File file = new File(middlewarePath);
        for (String name : file.list()) {
            ThreadPoolExecutorFactory.executor.execute(() -> {
                File f = new File(middlewarePath + File.separator + name);
                if (f.getAbsolutePath().contains(".tgz")) {
                    HelmChartFile chartFile = helmChartService.getHelmChartFromFile(null, null, f);
                    middlewareInfoService.insert(chartFile, f);
                }
            });
        }
    }

    public void insertMysqlImageRepository(MiddlewareClusterDTO clusterDTO) {
        ImageRepositoryDTO imageRepositoryDTO = imageRepositoryService.convertRegistry(clusterDTO.getRegistry());
        imageRepositoryService.insert(clusterDTO.getId(), imageRepositoryDTO);
    }

    public void updateMysqlImageRepository(MiddlewareClusterDTO clusterDTO) {
        Registry registry = clusterDTO.getRegistry();
        ImageRepositoryDTO imageRepositoryDTO = imageRepositoryService.detailByClusterId(clusterDTO.getName());
        BeanUtils.copyProperties(registry, imageRepositoryDTO);
        imageRepositoryDTO.setUsername(registry.getUser());
        imageRepositoryDTO.setProject(registry.getChartRepo());
        imageRepositoryDTO.setIsDefault(CommonConstant.NUM_ONE);
        imageRepositoryDTO.setPort(registry.getPort());
        imageRepositoryDTO.setHostAddress(registry.getAddress());
        imageRepositoryService.update(clusterDTO.getId(), imageRepositoryDTO);
    }

    public void clusterResource(MiddlewareClusterDTO cluster){
        Map<String, String> query = new HashMap<>();
        Map<String, String> resource = new HashMap<>();
        ClusterQuotaDTO clusterQuotaDTO = new ClusterQuotaDTO();
        //获取cpu总量
        try {
            query.put("query", "sum(count(node_cpu_seconds_total{ mode='system'}) by (kubernetes_pod_node_name))");
            PrometheusResponse cpuTotal = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO.setTotalCpu(Double.parseDouble(cpuTotal.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e){
            clusterQuotaDTO.setTotalCpu(0);
            log.error("集群查询cpu总量失败");
        }
        //获取cpu使用量
        try {
            query.put("query", "sum(sum(irate(node_cpu_seconds_total{mode!=\"idle\"}[5m])) by (kubernetes_pod_node_name))");
            PrometheusResponse cpuUsing = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO.setUsedCpu(Double.parseDouble(cpuUsing.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e){
            clusterQuotaDTO.setUsedCpu(0);
            log.error("集群查询cpu使用量失败");
        }
        //获取memory总量
        try {
            query.put("query", "sum(node_memory_MemTotal_bytes/1024/1024/1024)");
            PrometheusResponse memoryTotal = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO.setTotalMemory(Double.parseDouble(memoryTotal.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e){
            clusterQuotaDTO.setTotalMemory(0);
            log.error("集群查询memory总量失败");
        }
        //获取memory使用量
        try {
            query.put("query", "sum(((node_memory_MemTotal_bytes - node_memory_MemFree_bytes - node_memory_Cached_bytes - node_memory_Buffers_bytes - node_memory_Slab_bytes)/1024/1024/1024))");
            PrometheusResponse memoryUsing = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO.setUsedMemory(Double.parseDouble(memoryUsing.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e){
            clusterQuotaDTO.setUsedMemory(0);
            log.error("集群查询memory使用量失败");
        }
        cluster.setClusterQuotaDTO(clusterQuotaDTO);
    }

    @Override
    public List<Namespace> getRegisteredNamespaceNum(List<MiddlewareClusterDTO> clusterDTOList) {
        List<Namespace> namespaces = new ArrayList<>();
        clusterDTOList.forEach(clusterDTO -> {
            namespaces.addAll(getRegisteredNamespaceNum(clusterDTO));
        });
        return namespaces;
    }

    @Override
    public ClusterQuotaDTO getClusterQuota(List<MiddlewareClusterDTO> clusterDTOList) {
        ClusterQuotaDTO clusterQuotaSum = new ClusterQuotaDTO();
        clusterDTOList.forEach(clusterDTO -> {
            ClusterQuotaDTO clusterQuota = getClusterQuota(clusterDTO);
            if (clusterQuota != null) {
                clusterQuotaSum.setTotalCpu(clusterQuotaSum.getTotalCpu() + clusterQuota.getTotalCpu());
                clusterQuotaSum.setUsedCpu(clusterQuotaSum.getUsedCpu() + clusterQuota.getUsedCpu());
                clusterQuotaSum.setTotalMemory(clusterQuotaSum.getTotalMemory() + clusterQuota.getTotalMemory());
                clusterQuotaSum.setUsedMemory(clusterQuotaSum.getUsedMemory() + clusterQuota.getUsedMemory());
            }
        });
        clusterQuotaSum.setCpuUsedPercent(MathUtil.calcPercent(clusterQuotaSum.getUsedCpu(), clusterQuotaSum.getTotalCpu()));
        clusterQuotaSum.setMemoryUsedPercent(MathUtil.calcPercent(clusterQuotaSum.getUsedMemory(), clusterQuotaSum.getTotalMemory()));
        return clusterQuotaSum;
    }

    @Override
    public List<MiddlewareResourceInfo> getMwResource(String clusterId) throws Exception {
        // 获取集群下所有中间件信息
        List<MiddlewareCR> mwCrdList = middlewareCRService.listCR(clusterId, null, null);
        // 过滤未注册的分区
        List<Namespace> namespaceList = namespaceService.list(clusterId);
        mwCrdList = mwCrdList.stream()
            .filter(
                mwCrd -> namespaceList.stream().anyMatch(ns -> ns.getName().equals(mwCrd.getMetadata().getNamespace())))
            .collect(Collectors.toList());
        // 获取中间件图片路径
        List<MiddlewareInfoDTO> middlewareInfoDTOList = middlewareInfoService.list(clusterId).stream()
            .filter(info -> info.getImagePath() != null).collect(Collectors.toList());
        Map<String, String> imagePathMap = middlewareInfoDTOList.stream()
            .collect(Collectors.toMap(MiddlewareInfoDTO::getChartName, MiddlewareInfoDTO::getImagePath));
        List<MiddlewareResourceInfo> mwResourceInfoList = new ArrayList<>();
        final CountDownLatch clusterCountDownLatch = new CountDownLatch(mwCrdList.size());
        mwCrdList.forEach(mwCrd -> ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                Middleware middleware = middlewareService.detail(clusterId, mwCrd.getMetadata().getNamespace(),
                    mwCrd.getSpec().getName(), MiddlewareTypeEnum.findTypeByCrdType(mwCrd.getSpec().getType()));
                MiddlewareResourceInfo middlewareResourceInfo = new MiddlewareResourceInfo();
                BeanUtils.copyProperties(middleware, middlewareResourceInfo);
                middlewareResourceInfo.setClusterId(clusterId);
                middlewareResourceInfo.setImagePath(imagePathMap.getOrDefault(middleware.getType(), null));
                Map<String, String> queryMap = new HashMap<>();
                StringBuilder pods = getPodName(mwCrd);

                // 查询cpu配额
                try {
                    String cpuRequestQuery = "sum(kube_pod_container_resource_requests_cpu_cores{pod=~\"" + pods.toString()
                            + "\",namespace=\"" + mwCrd.getMetadata().getNamespace() + "\"})";
                    queryMap.put("query", cpuRequestQuery);
                    PrometheusResponse cpuRequest =
                            prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);
                    if (!CollectionUtils.isEmpty(cpuRequest.getData().getResult())) {
                        middlewareResourceInfo.setRequestCpu(ResourceCalculationUtil.roundNumber(
                                BigDecimal.valueOf(
                                        Double.parseDouble(cpuRequest.getData().getResult().get(0).getValue().get(1))),
                                2, RoundingMode.CEILING));
                    }
                }catch (Exception e){
                    log.error("中间件{} 查询cpu配额失败", middleware.getName());
                }
                // 查询cpu每5分钟平均用量
                try {
                    String per5MinCpuUsedQuery = "sum(rate(container_cpu_usage_seconds_total{pod=~\"" + pods.toString()
                        + "\",namespace=\"" + mwCrd.getMetadata().getNamespace() + "\",endpoint!=\"\"}[5m]))";
                    queryMap.put("query", per5MinCpuUsedQuery);
                    PrometheusResponse per5MinCpuUsed =
                        prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);
                    if (!CollectionUtils.isEmpty(per5MinCpuUsed.getData().getResult())) {
                        middlewareResourceInfo.setPer5MinCpu(ResourceCalculationUtil.roundNumber(
                            BigDecimal.valueOf(
                                Double.parseDouble(per5MinCpuUsed.getData().getResult().get(0).getValue().get(1))),
                            2, RoundingMode.CEILING));
                    }
                } catch (Exception e) {
                    log.error("中间件{} 查询cpu5分钟平均用量失败", middleware.getName());
                }
                // 查询memory配额
                try {
                    String memoryRequestQuery = "sum(kube_pod_container_resource_requests_memory_bytes{pod=~\""
                            + pods.toString() + "\",namespace=\"" + mwCrd.getMetadata().getNamespace() + "\"})";
                    queryMap.put("query", memoryRequestQuery);
                    PrometheusResponse memoryRequest =
                            prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);
                    if (!CollectionUtils.isEmpty(memoryRequest.getData().getResult())) {
                        middlewareResourceInfo.setRequestMemory(
                                ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(ResourceCalculationUtil.getResourceValue(
                                        memoryRequest.getData().getResult().get(0).getValue().get(1), MEMORY,
                                        ResourceUnitEnum.GI.getUnit())), 2, RoundingMode.CEILING));
                    }
                }catch (Exception e){
                    log.error("中间件{} 查询memory配额失败", middleware.getName());
                }
                // 查询memory每5分钟平均用量
                try {
                    String per5MinMemoryUsedQuery = "sum(avg_over_time(container_memory_working_set_bytes{pod=~\""
                        + pods.toString() + "\",namespace=\"" + mwCrd.getMetadata().getNamespace() + "\",endpoint!=\"\"}[5m])) /1024/1024/1024/2";
                    queryMap.put("query", per5MinMemoryUsedQuery);
                    PrometheusResponse per5MinMemoryUsed =
                        prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);
                    if (!CollectionUtils.isEmpty(per5MinMemoryUsed.getData().getResult())) {
                        middlewareResourceInfo.setPer5MinMemory(ResourceCalculationUtil.roundNumber(
                            BigDecimal.valueOf(
                                Double.parseDouble(per5MinMemoryUsed.getData().getResult().get(0).getValue().get(1))),
                            2, RoundingMode.CEILING));
                    }
                } catch (Exception e) {
                    log.error("中间件{} 查询memory5分钟平均用量失败", middleware.getName());
                }
                // 查询pvc总量
                List<String> pvcList = middlewareCRService.getPvc(mwCrd);
                StringBuilder pvcs = new StringBuilder();
                pvcList.forEach(pvc -> pvcs.append(pvc).append("|"));
                try {
                    String pvcTotalQuery =
                        "sum(kube_persistentvolumeclaim_resource_requests_storage_bytes{persistentvolumeclaim=~\""
                            + pvcs.toString() + "\",namespace=\"" + mwCrd.getMetadata().getNamespace()
                            + "\"}) by (persistentvolumeclaim) /1024/1024/1024";
                    Double pvcTotal = prometheusResourceMonitorService.queryAndConvert(clusterId, pvcTotalQuery);
                    middlewareResourceInfo.setRequestStorage(pvcTotal);
                } catch (Exception e) {
                    log.error("中间件{} 查询storage总量失败", middleware.getName());
                }
                // 查询pvc使用量
                try {
                    String pvcUsedQuery = "sum(kubelet_volume_stats_used_bytes{persistentvolumeclaim=~\""
                        + pvcs.toString() + "\",namespace=\"" + mwCrd.getMetadata().getNamespace()
                        + "\",endpoint!=\"\"}) by (persistentvolumeclaim) /1024/1024/1024";
                    Double pvcUsed = prometheusResourceMonitorService.queryAndConvert(clusterId, pvcUsedQuery);
                    middlewareResourceInfo.setPer5MinStorage(pvcUsed);
                } catch (Exception e) {
                    log.error("中间件{} 查询storage5分钟平均用量失败", middleware.getName());
                }

                // 计算cpu使用率
                if (middlewareResourceInfo.getRequestCpu() != null && middlewareResourceInfo.getPer5MinCpu() != null) {
                    double cpuRate =
                        middlewareResourceInfo.getPer5MinCpu() / middlewareResourceInfo.getRequestCpu() * 100;
                    middlewareResourceInfo.setCpuRate(
                        ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(cpuRate), 2, RoundingMode.CEILING));
                }
                // 计算memory使用率
                if (middlewareResourceInfo.getRequestMemory() != null
                    && middlewareResourceInfo.getPer5MinMemory() != null) {
                    double memoryRate =
                        middlewareResourceInfo.getPer5MinMemory() / middlewareResourceInfo.getRequestMemory() * 100;
                    middlewareResourceInfo.setMemoryRate(
                        ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(memoryRate), 2, RoundingMode.CEILING));
                }
                // 计算pvc使用率
                if (middlewareResourceInfo.getRequestStorage() != null
                        && middlewareResourceInfo.getPer5MinStorage() != null) {
                    double storageRate =
                            middlewareResourceInfo.getPer5MinStorage() / middlewareResourceInfo.getRequestStorage() * 100;
                    middlewareResourceInfo.setStorageRate(
                            ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(storageRate), 2, RoundingMode.CEILING));
                }
                mwResourceInfoList.add(middlewareResourceInfo);
            } catch (Exception e) {
                log.error("查询资源使用额度出错了", e);
            } finally {
                log.info("{}查询完成", mwCrd.getMetadata().getName());
                clusterCountDownLatch.countDown();
            }
        }));
        clusterCountDownLatch.await();
        log.info("查询完成，返回数据");
        return mwResourceInfoList;
    }

    @Override
    public List<ClusterNodeResourceDto> getNodeResource(String clusterId) throws Exception {
        List<Node> nodeList = nodeService.list(clusterId);
        // 查询cpu使用量
        String nodeCpuQuery = "sum(irate(node_cpu_seconds_total{mode!=\"idle\"}[5m])) by (kubernetes_pod_node_name)";
        Map<String, Double> nodeCpuUsed = nodeQuery(clusterId, nodeCpuQuery);
        // 查询memory使用量
        String nodeMemoryQuery = "((node_memory_MemTotal_bytes - node_memory_MemFree_bytes - node_memory_Cached_bytes - node_memory_Buffers_bytes - node_memory_Slab_bytes)/1024/1024/1024)";
        Map<String, Double> nodeMemoryUsed = nodeQuery(clusterId, nodeMemoryQuery);
        // 查询memory总量
        String nodeMemoryTotalQuery = "(node_memory_MemTotal_bytes/1024/1024/1024)";
        Map<String, Double> nodeMemoryTotal = nodeQuery(clusterId, nodeMemoryTotalQuery);
        return nodeList.stream().map(node -> {
            ClusterNodeResourceDto nodeRs = new ClusterNodeResourceDto();
            nodeRs.setClusterId(clusterId);
            nodeRs.setIp(node.getIp());
            nodeRs.setStatus(node.getStatus());
            nodeRs.setCreateTime(node.getCreateTime());
            // 设置cpu
            nodeRs.setCpuUsed(nodeCpuUsed.getOrDefault(node.getName(), null));
            nodeRs.setCpuTotal(Double.parseDouble(node.getCpu().getTotal()));
            if (nodeRs.getCpuUsed() != null) {
                nodeRs.setCpuRate(ResourceCalculationUtil.roundNumber(
                    BigDecimal.valueOf(nodeRs.getCpuUsed() / nodeRs.getCpuTotal() * 100), 2, RoundingMode.CEILING));
            }
            // 设置memory
            nodeRs.setMemoryUsed(nodeMemoryUsed.getOrDefault(node.getName(), null));
            nodeRs.setMemoryTotal(nodeMemoryTotal.getOrDefault(node.getName(), null));
            if (nodeRs.getMemoryUsed() != null){
                nodeRs.setMemoryRate(ResourceCalculationUtil.roundNumber(
                        BigDecimal.valueOf(nodeRs.getMemoryUsed() / nodeRs.getMemoryTotal() * 100), 2, RoundingMode.CEILING));
            }
            return nodeRs;
        }).sorted((o1, o2) -> o1.getCreateTime() == null ? -1
            : o2.getCreateTime() == null ? -1 : o2.getCreateTime().compareTo(o1.getCreateTime()))
            .collect(Collectors.toList());
    }

    @Override
    public List<ClusterNamespaceResourceDto> getNamespaceResource(String clusterId) throws Exception {
        List<Namespace> namespaceList = namespaceService.list(clusterId);
        Map<String, String> queryMap = new HashMap<>();
        // 查询cpu配额
        String cpuRequestQuery = "sum(container_spec_cpu_quota) by (namespace)/100000";
        queryMap.put("query", cpuRequestQuery);
        PrometheusResponse cpuRequest = prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

        // 查询cpu每5分钟平均用量
        String per5MinCpuUsedQuery = "sum(rate(container_cpu_usage_seconds_total{endpoint!=\"\"}[3m])) by (namespace)";
        queryMap.put("query", per5MinCpuUsedQuery);
        PrometheusResponse per5MinCpuUsed =
            prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

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

        // 查询pvc总量
        String pvcTotalQuery = "sum(kube_persistentvolumeclaim_resource_requests_storage_bytes) by (namespace) /1024/1024/1024";
        queryMap.put("query", pvcTotalQuery);
        PrometheusResponse pvcTotal =
                prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

        // 查询pvc使用量
        String pvcUsingQuery = "sum(kubelet_volume_stats_used_bytes{endpoint!=\"\"}) by (namespace) /1024/1024/1024";
        queryMap.put("query", pvcUsingQuery);
        PrometheusResponse pvcUsing =
                prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

        Map<Map<String, String>, List<String>> cpuRequestResult = getResultMap(cpuRequest);
        Map<Map<String, String>, List<String>> cpuPer5MinResult = getResultMap(per5MinCpuUsed);
        Map<Map<String, String>, List<String>> memoryRequestResult = getResultMap(memoryRequest);
        Map<Map<String, String>, List<String>> memoryPer5MinResult = getResultMap(per5MinMemoryUsed);
        Map<Map<String, String>, List<String>> pvcRequestResult = getResultMap(pvcTotal);
        Map<Map<String, String>, List<String>> pvcPer5MinResult = getResultMap(pvcUsing);
        return namespaceList.stream().map(ns -> {
            ClusterNamespaceResourceDto nsResource = new ClusterNamespaceResourceDto();
            Map<String, String> nsMap = new HashMap<>();
            nsMap.put("namespace", ns.getName());
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
            if (nsResource.getCpuRequest() != null && nsResource.getPer5MinCpu() != null && nsResource.getCpuRequest() != 0) {
                double cpuRate = nsResource.getPer5MinCpu() / nsResource.getCpuRequest() * 100;
                nsResource.setCpuRate(ResourceCalculationUtil.roundNumber2TwoDecimalWithCeiling(cpuRate));
            }
            // 计算memory使用率
            if (nsResource.getMemoryRequest() != null && nsResource.getPer5MinMemory() != null && nsResource.getMemoryRequest() != 0) {
                double memoryRate = nsResource.getPer5MinMemory() / nsResource.getMemoryRequest() * 100;
                nsResource.setMemoryRate(ResourceCalculationUtil.roundNumber2TwoDecimalWithCeiling(memoryRate));
            }
            // 计算pvc使用率
            if (nsResource.getPvcRequest() != null && nsResource.getPer5MinPvc() != null && nsResource.getPvcRequest() != 0) {
                double pvcRate = nsResource.getPer5MinPvc() / nsResource.getPvcRequest() * 100;
                nsResource.setPvcRate(ResourceCalculationUtil.roundNumber2TwoDecimalWithCeiling(pvcRate));
            }
            return nsResource.setClusterId(clusterId).setName(ns.getName());
        }).collect(Collectors.toList());
    }

    @Override
    public String getClusterJoinCommand(String clusterName, String apiAddress, String userToken) {
        String clusterJoinUrl = apiAddress + "/clusters/quickAdd";
        String curlCommand = "curl -X POST --url %s?name=%s --header Content-Type:multipart/form-data --header userToken:%s -F adminConf=@/etc/kubernetes/admin.conf";
        String res = String.format(curlCommand, clusterJoinUrl, clusterName, userToken);
        return res;
    }

    @Override
    public BaseResult quickAdd(MultipartFile adminConf, String name) {
        String filePath = uploadPath + "/" + adminConf.getName();
        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    log.error("文件夹创建失败");
                    return BaseResult.error();
                }
            }
            File file = new File(filePath);
            adminConf.transferTo(file);
        } catch (IOException e) {
            log.error("文件读取失败", e);
        }
        MiddlewareClusterDTO cluster = new MiddlewareClusterDTO();
        try {
            // 获取admin.conf全部内容
            String certificate = YamlUtil.convertToString(filePath);
            String serverAddress = YamlUtil.getServerAddress(filePath);
            // 删除admin.conf文件
            File file = new File(filePath);
            file.deleteOnExit();

            ClusterCert clusterCert = new ClusterCert();
            clusterCert.setCertificate(certificate);
            cluster.setCert(clusterCert);
            cluster.setName(name);
            cluster.setNickname(name);
            setClusterAddressInfo(cluster, serverAddress);
        } catch (Exception e) {
            log.error("集群添加失败", e);
            throw new BusinessException(DictEnum.CLUSTER, name, ErrorMessage.ADD_FAIL);
        }
        addCluster(cluster);
        return BaseResult.ok("集群添加成功");
    }

    public List<Namespace> listRegisteredNamespace(String clusterId) {
        if (StringUtils.isEmpty(clusterId)) {
            return Collections.emptyList();
        }
        List<Namespace> namespaces = namespaceService.list(clusterId, false, false, false, null, null);
        return namespaces.stream().filter(namespace -> namespace.isRegistered()).collect(Collectors.toList());
    }

    public Map<Map<String, String>, List<String>> getResultMap(PrometheusResponse response){
        return response.getData().getResult().stream().collect(Collectors.toMap(PrometheusResult::getMetric, PrometheusResult::getValue));
    }

    public Double getResourceResult(String num){
        return ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(Double.parseDouble(num)), 2, RoundingMode.CEILING);
    }

    public StringBuilder getPodName(MiddlewareCR mwCrd) {
        StringBuilder pods = new StringBuilder();
        List<MiddlewareInfo> podInfo = mwCrd.getStatus().getInclude().get(PODS);
        if (!CollectionUtils.isEmpty(podInfo)) {
            for (MiddlewareInfo middlewareInfo : podInfo) {
                pods.append(middlewareInfo.getName()).append("|");
            }
        }
        return pods;
    }

    public StringBuilder getPvcs(MiddlewareCR mwCrd){
        StringBuilder pvcs = new StringBuilder();
        List<MiddlewareInfo> pvcInfo = mwCrd.getStatus().getInclude().get(PERSISTENT_VOLUME_CLAIMS);
        if (!CollectionUtils.isEmpty(pvcInfo)) {
            for (MiddlewareInfo middlewareInfo : pvcInfo) {
                pvcs.append(middlewareInfo.getName()).append("|");
            }
        }
        return pvcs;
    }

    public Map<String, Double> nodeQuery(String clusterId, String query){
        Map<String, Double> resultMap = new HashMap<>();
        Map<String, String> queryMap = new HashMap<>();
        // 查询cpu使用量
        try {
            queryMap.put("query", query);
            PrometheusResponse nodeMemoryRequest =
                    prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);
            if (!CollectionUtils.isEmpty(nodeMemoryRequest.getData().getResult())) {
                nodeMemoryRequest.getData().getResult().forEach(result -> {
                    resultMap.put(result.getMetric().get("kubernetes_pod_node_name"), ResourceCalculationUtil.roundNumber(
                            BigDecimal.valueOf(Double.parseDouble(result.getValue().get(1))), 2, RoundingMode.CEILING));
                });
            }
        }catch (Exception e){
            log.error("node列表，查询cpu失败");
        }
        return resultMap;
    }


    /**
     * 获取集群注册的分区
     *
     * @param clusterDTO 集群dto
     * @return
     */
    public List<Namespace> getRegisteredNamespaceNum(MiddlewareClusterDTO clusterDTO) {
        if (clusterDTO == null) {
            return new ArrayList();
        }
        List<Namespace> namespaces = namespaceService.list(clusterDTO.getId(), false, false, false, null, null);
        return namespaces.stream().filter(namespace -> namespace.isRegistered()).collect(Collectors.toList());
    }

    /**
     * 获取集群资源配额及使用量
     *
     * @param clusterDTO 集群dto
     * @return
     */
    public ClusterQuotaDTO getClusterQuota(MiddlewareClusterDTO clusterDTO) {
        return clusterDTO.getClusterQuotaDTO();
    }

    private void createMiddlewareCrd(MiddlewareClusterDTO middlewareClusterDTO){
        //MiddlewareClusterDTO middlewareClusterDTO = clusterService.findById(clusterId);

        boolean error = false;
        Process process = null;
        try {
            String execCommand;
            execCommand = MessageFormat.format(
                    "kubectl apply -f {0} --server={1} --token={2} --insecure-skip-tls-verify=true",
                    middlewareCrdYamlPath, middlewareClusterDTO.getAddress(), middlewareClusterDTO.getAccessToken());
            log.info("执行kubectl命令：{}", execCommand);
            String[] commands = execCommand.split(" ");
            process = Runtime.getRuntime().exec(commands);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = stdInput.readLine()) != null) {
                log.info("执行指令执行成功:{}", line);
            }

            while ((line = stdError.readLine()) != null) {
                log.error("执行指令错误:{}", line);
                error = true;
            }
            if (error) {
                throw new Exception();
            }

        } catch (Exception e) {
            log.error("出现异常:", e);
            throw new CaasRuntimeException(String.valueOf(ErrorCodeMessage.RUN_COMMAND_ERROR));
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
    }

    /**
     * 设置集群地址信息
     * @param cluster 集群
     * @param serverAddress 集群master server信息
     */
    private void setClusterAddressInfo(MiddlewareClusterDTO cluster,String serverAddress){
        String[] serverInfos = serverAddress.split(":");
        String host = serverInfos[1].replaceAll("//", "");
        cluster.setProtocol(serverInfos[0]);
        cluster.setHost(host);
        cluster.setPort(Integer.parseInt(serverInfos[2]));
    }

    public void refresh(String clusterId) {
        if (run) {
            ThreadPoolExecutorFactory.executor.execute(() -> {
                run = false;
                List<MiddlewareClusterDTO> clusterList = listClusters().stream()
                    .filter(clusterDTO -> clusterDTO.getId().equals(clusterId)).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clusterList)) {
                    log.error("刷新集群信息失败，未找到集群:{}", clusterId);
                }
                MiddlewareClusterDTO dto = clusterList.get(0);
                CLUSTER_MAP.put(clusterId, SerializationUtils.clone(dto));
                try {
                    log.info("刷新集群信息成功，将静默10s");
                    Thread.sleep(10000);
                    log.info("静默完成，可再次刷新");
                    run = true;
                } catch (InterruptedException e) {
                    log.error("线程休眠异常", e);
                }
            });
        }
    }

}
