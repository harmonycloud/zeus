package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.caas.common.model.ActiveAreaClusterDto;
import com.harmonycloud.zeus.service.k8s.ActiveAreaClusterService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2022/8/13 1:44 下午
 */
@Service
@Slf4j
public class ActiveAreaClusterServiceImpl implements ActiveAreaClusterService {

    @Autowired
    private ClusterService clusterService;

    @Override
    public List<ActiveAreaClusterDto> list() {
        return clusterService.listClusters().stream().map(cluster -> {
            ActiveAreaClusterDto activeAreaClusterDto = new ActiveAreaClusterDto()
                    .setClusterId(cluster.getId())
                    .setClusterAliasName(cluster.getNickname())
                    .setActiveActive(cluster.getActiveActive())
                    .setActiveAreaNum(cluster.getActiveAreaNum())
                    .setStatusCode(cluster.getStatusCode());
            return activeAreaClusterDto;
        }).collect(Collectors.toList());
    }

    @Override
    public void update(String clusterId, Boolean active) {

    }
}
