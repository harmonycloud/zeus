package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.common.model.ClusterQuotaQuery;
import com.middleware.zeus.common.model.ResourceQuotaDo;
import com.middleware.zeus.service.k8s.ClusterQuotaService;
import com.middleware.zeus.service.k8s.ClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/28 4:17 下午
 */
@Slf4j
@Service
public class ClusterQuotaServiceImpl implements ClusterQuotaService {
    
    @Autowired
    private ClusterService clusterService;
    
    @Override
    public List<ResourceQuotaDo> list(ClusterQuotaQuery clusterQuotaQuery) {
        List<ResourceQuotaDo> resourceQuotaDoList = new ArrayList<>();
        for (String clusterId : clusterQuotaQuery.getClusterIdList()) {
            resourceQuotaDoList.add(clusterService.getResourceQuotaInfo(clusterId, clusterQuotaQuery.getDetail()));
        }
        return resourceQuotaDoList;
    }
}
