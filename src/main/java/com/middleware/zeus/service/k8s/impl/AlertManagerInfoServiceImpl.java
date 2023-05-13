package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.common.enums.ComponentsEnum;
import com.middleware.zeus.common.model.ClusterComponentsDto;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.service.k8s.AlertManagerInfoService;
import com.middleware.zeus.service.k8s.ClusterComponentService;
import com.middleware.zeus.service.k8s.ClusterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2023/3/29 11:19 上午
 */
@Service
@Slf4j
public class AlertManagerInfoServiceImpl implements AlertManagerInfoService {

    @Autowired
    private ClusterComponentService clusterComponentService;
    @Autowired
    private ClusterService clusterService;

    @Override
    public List<ClusterComponentsDto> list(String clusterId) {
        List<ClusterComponentsDto> componentsDtoList = new ArrayList<>();

        List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
        for (MiddlewareClusterDTO cluster : clusterList) {
            ClusterComponentsDto clusterComponentsDto =
                clusterComponentService.get(cluster.getId(), ComponentsEnum.ALERTMANAGER.getName());
            clusterComponentsDto.setClusterAliasName(cluster.getNickname());
            componentsDtoList.add(clusterComponentsDto);
        }

        if (StringUtils.isNotEmpty(clusterId)) {
            componentsDtoList = componentsDtoList.stream()
                .filter(clusterComponentsDto -> clusterComponentsDto.getClusterId().equals(clusterId))
                .collect(Collectors.toList());
        }

        return componentsDtoList;
    }
}
