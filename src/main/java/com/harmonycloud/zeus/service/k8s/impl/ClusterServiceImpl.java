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
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.api.R;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.enums.middleware.ResourceUnitEnum;
import com.harmonycloud.caas.common.model.*;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;
import com.harmonycloud.zeus.util.YamlUtil;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.*;
import com.harmonycloud.caas.common.enums.middleware.StorageClassProvisionerEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.integration.cluster.ClusterWrapper;
import com.harmonycloud.zeus.integration.cluster.PrometheusWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.EsService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.service.registry.RegistryService;
import com.harmonycloud.zeus.util.K8sClient;
import com.harmonycloud.zeus.util.MathUtil;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Slf4j
@Service
public class ClusterServiceImpl implements ClusterService {

    /**
     * 默认存储限额
     */
    private static final String DEFAULT_STORAGE_LIMIT = "100Gi";
    @Value("${system.upload.path:/usr/local/zeus-pv/upload}")
    private String uploadPath;

    @Autowired
    private ClusterWrapper clusterWrapper;
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
    private EsService esService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;
    @Autowired
    private PrometheusWrapper prometheusWrapper;
    @Autowired
    private MiddlewareCRDService middlewareCRDService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private ClusterComponentService clusterComponentService;

    @Value("${k8s.component.components:/usr/local/zeus-pv/components}")
    private String componentsPath;
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
        List<MiddlewareClusterDTO> clusters;
        List<MiddlewareCluster> clusterList = clusterWrapper.listClusters();
        if (clusterList.size() <= 0) {
            return new ArrayList<>(0);
        }
        clusters = clusterList.stream().map(c -> {
            MiddlewareClusterInfo info = c.getSpec().getInfo();
            MiddlewareClusterDTO cluster = new MiddlewareClusterDTO();
            BeanUtils.copyProperties(info, cluster);
            cluster.setId(K8sClient.getClusterId(c.getMetadata())).setHost(info.getAddress())
                .setName(c.getMetadata().getName()).setDcId(c.getMetadata().getNamespace())
                .setIngress(info.getIngress());
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
                    List<Namespace> list = namespaceService.list(cluster.getId());
                    cluster.getAttributes().put(NS_COUNT, list.size());
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
        // 深拷贝对象返回，避免其他地方修改内容
        return SerializationUtils.clone(dto);
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
            MiddlewareCluster c = clusterWrapper.create(mw);
            JSONObject attributes = new JSONObject();
            attributes.put(CREATE_TIME, DateUtils.parseUTCDate(c.getMetadata().getCreationTimestamp()));
            cluster.setAttributes(attributes);
        } catch (IOException e) {
            log.error("集群id：{}，添加集群异常", cluster.getId());
            throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.ADD_FAIL);
        }
        // 将chart包存进数据库
        insertMysqlChart(cluster.getId());
        // 安装组件
        /*createComponents(cluster);
        //初始化集群索引模板
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                esService.initEsIndexTemplate();
                log.info("集群:{}索引模板初始化完成", cluster.getName());
            } catch (Exception e) {
                log.error("集群:{}索引模板初始化失败", cluster.getName(), e);
            }
        });*/
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
        oldCluster.setIngress(cluster.getIngress());
        oldCluster.setRegistry(cluster.getRegistry());
        oldCluster.setLogging(cluster.getLogging());

        update(oldCluster);
    }

    @Override
    public void update(MiddlewareClusterDTO cluster) {
        try {
            clusterWrapper.update(convert(cluster));
        } catch (IOException e) {
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
        
        // 设置ingress
        if (cluster.getIngress() != null && cluster.getIngress().getTcp() == null) {
            cluster.getIngress().setTcp(new MiddlewareClusterIngress.IngressConfig());
        }
        
        // 设置es信息
        if (cluster.getLogging() == null) {
            cluster.setLogging(new MiddlewareClusterLogging());
        }
        if (cluster.getLogging().getElasticSearch() == null) {
            cluster.getLogging().setElasticSearch(new MiddlewareClusterLoggingInfo());
        }
        
        // 设置存储限额
        if (cluster.getStorage() == null) {
            cluster.setStorage(new HashMap<>());
        }
        if (cluster.getStorage().get(SUPPORT) == null) {
            List<String> defaultSupportList = StorageClassProvisionerEnum.getDefaultSupportType();
            Map<String, String> support =
                defaultSupportList.stream().collect(Collectors.toMap(s -> s, s -> DEFAULT_STORAGE_LIMIT));
            cluster.getStorage().put(SUPPORT, support);
        }
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
            clusterWrapper.delete(cluster.getDcId(), cluster.getName());
        } catch (IOException e) {
            log.error("集群id：{}，删除集群异常", clusterId, e);
            throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.DELETE_FAIL);
        }
        // 从map中移除
        k8SDefaultClusterService.delete(clusterId);
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
        List<MiddlewareCRD> middlewareCRDList;
        try {
            middlewareCRDList = middlewareCRDService.listCR(clusterId, null, null);
        } catch (Exception e) {
            return true;
        }
        if (!CollectionUtils.isEmpty(middlewareCRDList) && middlewareCRDList.stream().anyMatch(
            middlewareCRD -> !"escluster-middleware-elasticsearch".equals(middlewareCRD.getMetadata().getName())
                && !"mysqlcluster-zeus-mysql".equals(middlewareCRD.getMetadata().getName()))) {
            return false;
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

    public void clusterResource(MiddlewareClusterDTO cluster){
        Map<String, String> query = new HashMap<>();
        Map<String, String> resource = new HashMap<>();
        ClusterQuotaDTO clusterQuotaDTO = new ClusterQuotaDTO();
        //获取cpu总量
        try {
            query.put("query", "sum(harmonycloud_node_cpu_total)");
            PrometheusResponse cpuTotal = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO.setTotalCpu(Double.parseDouble(cpuTotal.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e){
            clusterQuotaDTO.setTotalCpu(0);
            log.error("集群查询cpu总量失败");
        }
        //获取cpu使用量
        try {
            query.put("query", "sum(harmonycloud_node_cpu_using)");
            PrometheusResponse cpuUsing = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO.setUsedCpu(Double.parseDouble(cpuUsing.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e){
            clusterQuotaDTO.setUsedCpu(0);
            log.error("集群查询cpu使用量失败");
        }
        //获取memory总量
        try {
            query.put("query", "sum(harmonycloud_node_memory_total)");
            PrometheusResponse memoryTotal = prometheusWrapper.get(cluster.getId(), PROMETHEUS_API_VERSION, query);
            clusterQuotaDTO.setTotalMemory(Double.parseDouble(memoryTotal.getData().getResult().get(0).getValue().get(1)));
        } catch (Exception e){
            clusterQuotaDTO.setTotalMemory(0);
            log.error("集群查询memory总量失败");
        }
        //获取memory使用量
        try {
            query.put("query", "sum(harmonycloud_node_memory_using)");
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
        List<MiddlewareCRD> mwCrDList = middlewareCRDService.listCR(clusterId, null, null);
        // 获取中间件图片路径
        List<MiddlewareInfoDTO> middlewareInfoDTOList = middlewareInfoService.list(clusterId);
        Map<String, String> imagePathMap = middlewareInfoDTOList.stream().collect(Collectors.toMap(MiddlewareInfoDTO::getChartName, MiddlewareInfoDTO::getImagePath));
        List<MiddlewareResourceInfo> mwResourceInfoList = new ArrayList<>();
        final CountDownLatch clusterCountDownLatch = new CountDownLatch(mwCrDList.size());
        mwCrDList.forEach(mwCrd -> ThreadPoolExecutorFactory.executor.execute(() -> {
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
                        + "\",namespace=\"" + mwCrd.getMetadata().getNamespace() + "\"}[5m]))";
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
                        + pods.toString() + "\",namespace=\"" + mwCrd.getMetadata().getNamespace() + "\"}[5m]))";
                    queryMap.put("query", per5MinMemoryUsedQuery);
                    PrometheusResponse per5MinMemoryUsed =
                        prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);
                    if (!CollectionUtils.isEmpty(per5MinMemoryUsed.getData().getResult())) {
                        middlewareResourceInfo.setPer5MinMemory(
                            ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(ResourceCalculationUtil
                                .getResourceValue(per5MinMemoryUsed.getData().getResult().get(0).getValue().get(1),
                                    MEMORY, ResourceUnitEnum.GI.getUnit())),
                                2, RoundingMode.CEILING));
                    }
                } catch (Exception e) {
                    log.error("中间件{} 查询memory5分钟平均用量失败", middleware.getName());
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
                mwResourceInfoList.add(middlewareResourceInfo);
            } catch (Exception e) {
            } finally {
                clusterCountDownLatch.countDown();
            }
        }));
        clusterCountDownLatch.await();
        return mwResourceInfoList;
    }

    @Override
    public List<ClusterNodeResourceDto> getNodeResource(String clusterId) throws Exception {
        List<Node> nodeList = nodeService.list(clusterId);

        Map<String, Double> nodeCpuUsed = getNodeCpuUsed(clusterId);
        return nodeList.stream().map(node -> {
            ClusterNodeResourceDto nodeRs = new ClusterNodeResourceDto();
            nodeRs.setClusterId(clusterId);
            nodeRs.setIp(node.getIp());
            nodeRs.setCpuUsed(nodeCpuUsed.getOrDefault(node.getName(), null));
            nodeRs.setCpuTotal(Double.parseDouble(node.getCpu().getTotal()));
            if (nodeRs.getCpuUsed() != null) {
                nodeRs.setCpuRate(ResourceCalculationUtil.roundNumber(
                    BigDecimal.valueOf(nodeRs.getCpuUsed() / nodeRs.getCpuTotal() * 100), 2, RoundingMode.CEILING));
            }
            nodeRs
                .setMemoryUsed(ResourceCalculationUtil.roundNumber(
                    BigDecimal.valueOf((Double.parseDouble(node.getMemory().getTotal())
                        - Double.parseDouble(node.getMemory().getAllocated())) / 1024 / 1024),
                    2, RoundingMode.CEILING));
            nodeRs.setMemoryTotal(ResourceCalculationUtil.roundNumber(
                BigDecimal.valueOf(Double.parseDouble(node.getMemory().getTotal()) / 1024 / 1024), 2,
                RoundingMode.CEILING));
            nodeRs.setMemoryRate(ResourceCalculationUtil.roundNumber(
                BigDecimal.valueOf(nodeRs.getMemoryUsed() / nodeRs.getMemoryTotal() * 100), 2, RoundingMode.CEILING));
            nodeRs.setStatus(node.getStatus());
            nodeRs.setCreateTime(node.getCreateTime());
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
        String per5MinCpuUsedQuery = "sum(rate(container_cpu_usage_seconds_total[3m])) by (namespace)";
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
            "(sum(avg_over_time(container_memory_usage_bytes[5m])) by (namespace))/1024/1024/1024";
        queryMap.put("query", per5MinMemoryUsedQuery);
        PrometheusResponse per5MinMemoryUsed =
            prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);

        Map<Map<String, String>, List<String>> cpuRequestResult = getResultMap(cpuRequest);
        Map<Map<String, String>, List<String>> cpuPer5MinResult = getResultMap(per5MinCpuUsed);
        Map<Map<String, String>, List<String>> memoryRequestResult = getResultMap(memoryRequest);
        Map<Map<String, String>, List<String>> memoryPer5MinResult = getResultMap(per5MinMemoryUsed);
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
            // 计算cpu使用率
            if (nsResource.getCpuRequest() != null && nsResource.getPer5MinCpu() != null) {
                double cpuRate = nsResource.getPer5MinCpu() / nsResource.getCpuRequest() * 100;
                nsResource.setCpuRate(ResourceCalculationUtil.roundNumber2TwoDecimalWithCeiling(cpuRate));
            }
            // 计算memory使用率
            if (nsResource.getMemoryRequest() != null && nsResource.getPer5MinMemory() != null) {
                double memoryRate = nsResource.getPer5MinMemory() / nsResource.getMemoryRequest() * 100;
                nsResource.setMemoryRate(ResourceCalculationUtil.roundNumber2TwoDecimalWithCeiling(memoryRate));
            }
            return nsResource.setClusterId(clusterId).setName(ns.getName());
        }).collect(Collectors.toList());
    }

    @Override
    public String getClusterJoinCommand(String clusterName, String apiAddress, String userToken) {
        String clusterJoinUrl = apiAddress + "/api/clusters/quickAdd";
        String curlCommand = "curl -X POST --url %s?name=%s --header Content-Type:multipart/form-data --header userToken:%s -F adminConf=@/etc/kubernetes/admin.conf";
        String res = String.format(curlCommand, clusterJoinUrl, clusterName, userToken);
        return res;
    }

    @Override
    public BaseResult quickAdd(MultipartFile adminConf, String name) {
        String filePath = uploadPath + "/" + adminConf.getName();
        try {
            File file = new File(filePath);
            adminConf.transferTo(file);
        } catch (IOException e) {
            log.error("文件读取失败", e);
        }
        // 获取admin.conf全部内容
        String certificate = YamlUtil.convertToString(filePath);
        String serverAddress = YamlUtil.getServerAddress(filePath);
        // 删除admin.conf文件
        File file = new File(filePath);
        file.deleteOnExit();

        MiddlewareClusterDTO cluster = new MiddlewareClusterDTO();
        ClusterCert clusterCert = new ClusterCert();
        clusterCert.setCertificate(certificate);
        cluster.setCert(clusterCert);
        cluster.setName(name);
        cluster.setNickname(name);
        setClusterAddressInfo(cluster, serverAddress);
        addCluster(cluster);
        return BaseResult.ok();
    }

    public Map<Map<String, String>, List<String>> getResultMap(PrometheusResponse response){
        return response.getData().getResult().stream().collect(Collectors.toMap(PrometheusResult::getMetric, PrometheusResult::getValue));
    }

    public Double getResourceResult(String num){
        return ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(Double.parseDouble(num)), 2, RoundingMode.CEILING);
    }

    public StringBuilder getPodName(MiddlewareCRD mwCrd) {
        StringBuilder pods = new StringBuilder();
        List<MiddlewareInfo> podInfo = mwCrd.getStatus().getInclude().get(PODS);
        if (!CollectionUtils.isEmpty(podInfo)) {
            for (MiddlewareInfo middlewareInfo : podInfo) {
                pods.append(middlewareInfo.getName()).append("|");
            }
        }
        return pods;
    }

    public StringBuilder getPvcs(MiddlewareCRD mwCrd){
        StringBuilder pvcs = new StringBuilder();
        List<MiddlewareInfo> pvcInfo = mwCrd.getStatus().getInclude().get(PERSISTENT_VOLUME_CLAIMS);
        if (!CollectionUtils.isEmpty(pvcInfo)) {
            for (MiddlewareInfo middlewareInfo : pvcInfo) {
                pvcs.append(middlewareInfo.getName()).append("|");
            }
        }
        return pvcs;
    }

    public Map<String, Double> getNodeCpuUsed(String clusterId){
        Map<String, Double> nodeCpu = new HashMap<>();
        Map<String, String> queryMap = new HashMap<>();
        // 查询cpu使用量
        try {
            String nodeCpuQuery = "sum(irate(node_cpu_seconds_total{mode!=\"idle\"}[5m])) by (kubernetes_pod_node_name)";
            queryMap.put("query", nodeCpuQuery);
            PrometheusResponse nodeCpuRequest =
                    prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);
            if (!CollectionUtils.isEmpty(nodeCpuRequest.getData().getResult())) {
                nodeCpuRequest.getData().getResult().forEach(result -> {
                    nodeCpu.put(result.getMetric().get("kubernetes_pod_node_name"), ResourceCalculationUtil.roundNumber(
                        BigDecimal.valueOf(Double.parseDouble(result.getValue().get(1))), 2, RoundingMode.CEILING));
                });
            }
        }catch (Exception e){
            log.error("node列表，查询cpu失败");
        }
        return nodeCpu;
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
        List<Namespace> namespaces = namespaceService.list(clusterDTO.getId(), false, false, false, null);
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
                    "kubectl create -f {0} --server={1} --token={2} --insecure-skip-tls-verify=true",
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

}
