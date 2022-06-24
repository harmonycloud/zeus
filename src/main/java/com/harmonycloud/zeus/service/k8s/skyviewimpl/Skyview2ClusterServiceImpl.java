package com.harmonycloud.zeus.service.k8s.skyviewimpl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.CaasResult;
import com.harmonycloud.caas.common.model.ClusterCert;
import com.harmonycloud.caas.common.model.ClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.middleware.Registry;
import com.harmonycloud.zeus.bean.BeanMiddlewareCluster;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.service.k8s.ClusterServiceImpl;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.skyviewservice.Skyview2ClusterServiceClient;
import com.harmonycloud.zeus.skyviewservice.Skyview2UserServiceClient;
import com.harmonycloud.zeus.util.YamlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/6/17 11:02 上午
 */
@Slf4j
@Service
@ConditionalOnProperty(value="system.usercenter",havingValue = "skyview2")
public class Skyview2ClusterServiceImpl extends ClusterServiceImpl {

    @Value("${system.skyview.username:admin}")
    private String skyviewAdminName;
    @Value("${system.skyview.password:Hc@Cloud01}")
    private String skyviewAdminPassword;

    @Value("${system.registry.protocol:https}")
    private String protocol;
    @Value("${system.registry.address}")
    private String address;
    @Value("${system.registry.port}")
    private int port;
    @Value("${system.registry.username}")
    private String username;
    @Value("${system.registry.password}")
    private String password;
    @Value("${system.registry.type:harbor}")
    private String type;
    @Value("${system.registry.chartRepo:middleware}")
    private String chartRepo;
    @Value("${system.registry.version:v2}")
    private String version;

    @Autowired
    private Skyview2UserServiceClient userServiceClient;
    @Autowired
    private Skyview2ClusterServiceClient clusterServiceClient;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private MiddlewareClusterService middlewareClusterService;

    /**
     * 中间件平台和观云台的clusterid缓存
     * 格式  观云台clusterid:中间件平台clusterid
     */
    private static Map<String, String> clusterIdMap = new HashMap<>();

    /**
     * 将中间件平台集群id转为观云台集群id
     * @param zeusClusterId
     * @return
     */
    public static String convertSkyviewClusterId(String zeusClusterId) {
        StringBuilder skyviewClusterId = new StringBuilder();
        clusterIdMap.forEach((k, v) -> {
            if (zeusClusterId.equals(v)) {
                skyviewClusterId.append(k);
                return;
            }
        });
        return skyviewClusterId.toString();
    }

    @Override
    public List<MiddlewareClusterDTO> listClusters() {
        // 同步集群
        syncCluster();
        return super.listClusters(false, null, null);
    }

    @Override
    public String convertClusterId(String skyviewClusterId) {
        if (clusterIdMap.get(skyviewClusterId) == null) {
            this.syncCluster();
        }
        return clusterIdMap.get(skyviewClusterId);
    }

    @Override
    public List<MiddlewareCR> filterByNamespace(String clusterId, List<MiddlewareCR> mwCrdList) {
        return mwCrdList;
    }

    @Override
    public List<Namespace> getRegisteredNamespaceNum(List<MiddlewareClusterDTO> clusterDTOList) {
        List<Namespace> namespaces = new ArrayList<>();
        clusterDTOList.forEach(clusterDTO -> {
            namespaces.addAll(namespaceService.list(clusterDTO.getId()));
        });
        return namespaces;
    }

    /**
     * 保存项目集群信息
     * @param clusterDTO
     */
    public void saveCluster(ClusterDTO clusterDTO){
        log.info("开始添加集群：{}", clusterDTO.getName());
        MiddlewareClusterDTO cluster = new MiddlewareClusterDTO();
        ClusterCert clusterCert = new ClusterCert();
        clusterCert.setCertificateAuthorityData(clusterDTO.getApiCa());
        clusterCert.setClientKeyData(clusterDTO.getApiKey());
        clusterCert.setClientCertificateData(clusterDTO.getApiCrt());
        String certificateStr  = YamlUtil.generateAdminConf(clusterCert, clusterDTO.getApiServerUrl());
        clusterCert.setCertificate(certificateStr);
        cluster.setCert(clusterCert);
        cluster.setProtocol(clusterDTO.getProtocol());
        cluster.setHost(clusterDTO.getHost());
        cluster.setPort(clusterDTO.getPort());
        cluster.setName(clusterDTO.getName());
        cluster.setNickname(clusterDTO.getAliasName());
        Registry registry = new Registry();
        registry.setProtocol(protocol);
        registry.setAddress(address);
        registry.setPort(port);
        registry.setUser(username);
        registry.setPassword(password);
        registry.setType(type);
        registry.setChartRepo(chartRepo);
        registry.setVersion(version);
        cluster.setRegistry(registry);
        try {
            addCluster(cluster);
        } catch (Exception e) {
            log.error("添加集群出错了", e);
        }
    }

    /**
     * 转换得到集群详细信息
     * @param clusterArray
     * @param token
     * @return
     */
    private List<ClusterDTO> convertCluster(JSONArray clusterArray, String token){
        List<ClusterDTO> clusterDTOS = new ArrayList<>();
        clusterArray.forEach(cluster -> {
            JSONObject jsonCluster = (JSONObject) cluster;
            String clusterId = jsonCluster.getString("id");
            CaasResult<JSONObject> clusterDetail = clusterServiceClient.clusterDetail(token, clusterId);
            ClusterDTO clusterDTO = new ClusterDTO();
            try {
                BeanUtils.copyProperties(clusterDTO, clusterDetail.getData());
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("拷贝集群对象信息出错了", e);
            }
            clusterDTOS.add(clusterDTO);
        });
        return clusterDTOS;
    }

    private void syncCluster(){
        CaasResult<JSONObject> caasResult = userServiceClient.login(skyviewAdminName, skyviewAdminPassword, "ch");
        String caastoken = caasResult.getStringVal("token");
        Map<String, String> skyviewClusterMap;
        // 1、同步集群信息
        CaasResult<JSONArray> clusterResult = clusterServiceClient.clusters(caastoken);
        List<ClusterDTO> clusterList = convertCluster(clusterResult.getData(), caastoken).stream().
                filter(item -> !"top".equals(item.getName())).collect(Collectors.toList());
        skyviewClusterMap = clusterList.stream().collect(Collectors.toMap(ClusterDTO::getHost, ClusterDTO::getId));
        List<MiddlewareClusterDTO> clusterDTOS = new ArrayList<>();
        // 查询平台存储的全部集群
        List<BeanMiddlewareCluster> clusters = middlewareClusterService.listClustersByClusterId(null);
        if (!CollectionUtils.isEmpty(clusters)) {
            clusterDTOS = super.listClusters(false, null, null);
        }
        Set<String> cllusterHostSet = clusterDTOS.stream().map(MiddlewareClusterDTO::getHost).collect(Collectors.toSet());
        clusterList.forEach(clusterDTO -> {
            if (!cllusterHostSet.contains(clusterDTO.getHost())) {
                saveCluster(clusterDTO);
            }
        });
        // 保存集群信息
        Map<String, String> zeusClusterMap = clusterDTOS.stream().collect(Collectors.toMap(MiddlewareClusterDTO::getHost, MiddlewareClusterDTO::getId));
        skyviewClusterMap.forEach((k, v) -> clusterIdMap.put(v, zeusClusterMap.get(k) != null ? zeusClusterMap.get(k) : k));
    }

}
