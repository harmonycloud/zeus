package com.middleware.zeus.skyview.v2.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.model.ClusterCert;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.skyview.v2.client.V2ClusterServiceClient;
import com.middleware.zeus.skyview.v2.service.V2ClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2023/3/26 4:32 下午
 */
@Slf4j
@Service
public class V2ClusterServiceImpl implements V2ClusterService {

    @Autowired
    private V2ClusterServiceClient v2ClusterServiceClient;



    @Override
    public List<MiddlewareClusterDTO> list() {
        CaasResult<JSONArray> res = v2ClusterServiceClient.list(false, false, false);
        return res.getData().stream()
            .map(cluster -> convertClusterDto(JSONObject.parseObject(JSONObject.toJSONString(cluster))))
            .collect(Collectors.toList());
    }

    @Override
    public MiddlewareClusterDTO get(String clusterId) {
        CaasResult<JSONObject> res = v2ClusterServiceClient.get(clusterId);
        return convertClusterDto(res.getData());
    }

    public MiddlewareClusterDTO convertClusterDto(JSONObject cluster){
        MiddlewareClusterDTO clusterDTO = new MiddlewareClusterDTO();

        clusterDTO.setId(cluster.getString("id"));
        clusterDTO.setName(cluster.getString("name"));
        clusterDTO.setNickname(cluster.getString("aliasName"));

        ClusterCert cert = new ClusterCert();
        if (cluster.containsKey("apiCa")){
            cert.setClientCertificateData(cluster.getString("apiCa"));
        }
        if (cluster.containsKey("apiCrt")){
            cert.setClientCertificateData(cluster.getString("apiCrt"));
        }
        if (cluster.containsKey("apiKey")){
            cert.setCertificateAuthorityData(cluster.getString("apiKey"));
        }
        if (cluster.containsKey("certificate")){
            cert.setCertificate(cluster.getString("certificate"));
        }
        clusterDTO.setCert(cert);

        clusterDTO.setProtocol(cluster.getString("protocol"));
        clusterDTO.setHost(cluster.getString("compAddress"));
        clusterDTO.setPort(cluster.getInteger("port"));



        return clusterDTO;
    }
}
