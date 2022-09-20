package com.harmonycloud.zeus.service.k8s.skyviewimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import com.harmonycloud.zeus.skyviewservice.client.Skyview2ClusterServiceClient;
import com.harmonycloud.zeus.skyviewservice.client.Skyview2UserServiceClient;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.CaasResult;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.ClusterCert;
import com.harmonycloud.caas.common.model.ClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.middleware.Registry;
import com.harmonycloud.zeus.bean.BeanMiddlewareCluster;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.k8s.impl.ClusterServiceImpl;
import com.harmonycloud.zeus.util.CryptoUtils;
import com.harmonycloud.zeus.util.YamlUtil;

import lombok.extern.slf4j.Slf4j;

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

    @Autowired
    private Skyview2UserServiceClient userServiceClient;
    @Autowired
    private Skyview2ClusterServiceClient clusterServiceClient;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private MiddlewareClusterService middlewareClusterService;

    @Value("${system.skyview.encryptPassword:false}")
    private boolean encryptPassword;
    /**
     * 中间件平台和观云台的clusterid缓存
     * 格式  观云台clusterid:中间件平台clusterid
     */
    private static Map<String, String> clusterIdMap = new HashMap<>();

    /**
     * 观云台集群别名缓存
     * 格式 观云台clusterid:观云台集群
     */
    private static Map<String, ClusterDTO> skyviewClustersCache = new HashMap<>();

    @Override
    public List<MiddlewareClusterDTO> listClusters() {
        // 同步集群
        log.info("开始同步集群信息");
        syncCluster();
        return super.listClusters(false, null, null);
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

    @Override
    public String convertToZeusClusterId(String skyviewClusterId) {
        if (clusterIdMap.get(skyviewClusterId) == null) {
            this.syncCluster();
        }
        return clusterIdMap.get(skyviewClusterId);
    }

    @Override
    public ClusterDTO findBySkyviewClusterId(String skyviewClusterId) {
        if (!skyviewClustersCache.containsKey(skyviewClusterId)) {
            this.syncCluster();
        }
        return skyviewClustersCache.get(skyviewClusterId);
    }

    @Override
    public String convertToSkyviewClusterId(String zeusClusterId) {
        StringBuilder skyviewClusterId = new StringBuilder();
        if (!clusterIdMap.containsValue(zeusClusterId)) {
            this.syncCluster();
        }
        clusterIdMap.forEach((k, v) -> {
            if (zeusClusterId.equals(v)) {
                skyviewClusterId.append(k);
                return;
            }
        });
        return skyviewClusterId.toString();
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

    private synchronized void syncCluster(){
        String tempPassword =  skyviewAdminPassword;
        if (encryptPassword) {
            tempPassword = CryptoUtils.encrypt(skyviewAdminPassword);
        }
        CaasResult<JSONObject> caasResult = userServiceClient.login(skyviewAdminName, tempPassword, "ch");
        if (!caasResult.getSuccess()){
            log.error("集群同步时，用户{} 登录观云台失败，原因：{}", skyviewAdminName, caasResult.getData());
            throw new BusinessException(ErrorMessage.LOGIN_CAAS_API_FAILED);
        }
        String caasToken = caasResult.getStringVal("token");

        // 1、同步集群信息
        CaasResult<JSONArray> clusterResult = clusterServiceClient.clusters(caasToken);
        List<ClusterDTO> clusterList = convertCluster(clusterResult.getData(), caasToken);
        // 如果top集群被用作业务集群，则过滤掉top集群
        clusterList = filterTopCluster(clusterList);
        Map<String, String> skyviewClusterMap = clusterList.stream().collect(Collectors.toMap(ClusterDTO::getHost, ClusterDTO::getId));
        skyviewClustersCache = clusterList.stream().collect(Collectors.toMap(ClusterDTO::getId, clusterDTO -> clusterDTO));

        List<MiddlewareClusterDTO> clusterDTOS = new ArrayList<>();
        // 查询平台存储的全部集群
        List<BeanMiddlewareCluster> clusters = middlewareClusterService.listClustersByClusterId(null);
        if (!CollectionUtils.isEmpty(clusters)) {
            clusterDTOS = super.listClusters(false, null, null);
        }
        Set<String> clusterHostSet = clusterDTOS.stream().map(MiddlewareClusterDTO::getHost).collect(Collectors.toSet());
        // 保存集群信息
        clusterList.forEach(clusterDTO -> {
            if (clusterDTO.getIsEnable() != null && clusterDTO.getIsEnable() && !clusterHostSet.contains(clusterDTO.getHost())) {
                saveCluster(clusterDTO);
            }
        });
        // 映射相同host下观云台和中间件平台的不同的clusterId
        Map<String, String> zeusClusterMap = clusterDTOS.stream().collect(Collectors.toMap(MiddlewareClusterDTO::getHost, MiddlewareClusterDTO::getId));
        skyviewClusterMap.forEach((k, v) -> clusterIdMap.put(v, zeusClusterMap.get(k) != null ? zeusClusterMap.get(k) : k));
    }

    private void saveComponent(String clusterId, ClusterDTO clusterDTO) {
        MiddlewareClusterDTO middlewareClusterDTO = super.findById(clusterId);
        MiddlewareClusterDTO prometheus = new MiddlewareClusterDTO();
        if (middlewareClusterDTO.getMonitor() == null || (middlewareClusterDTO.getMonitor() != null && middlewareClusterDTO.getMonitor().getPrometheus() == null)) {

        }
        //clusterComponentService.integrate();
    }

    /**
     * 如果top集群被用作业务集群，则过滤掉top集群
     * @param clusterList
     */
    private List<ClusterDTO> filterTopCluster(List<ClusterDTO> clusterList) {
        String topClusterApiServerHost = "";
        for (ClusterDTO clusterDTO : clusterList) {
            if ("top".equals(clusterDTO.getName())) {
                topClusterApiServerHost = clusterDTO.getHost();
                break;
            }
        }
        if (StringUtils.isEmpty(topClusterApiServerHost)) {
            return clusterList;
        }
        int num = 0;
        for (ClusterDTO clusterDTO : clusterList) {
            if (topClusterApiServerHost.equals(clusterDTO.getHost())) {
                num++;
            }
        }
        if (num != 1) {
            return clusterList.stream().filter(clusterDTO -> !"top".equals(clusterDTO.getName())).collect(Collectors.toList());
        }
        return clusterList;
    }

}
