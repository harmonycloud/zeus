package com.middleware.zeus.service.components.impl;

import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.common.enums.ComponentsEnum;
import com.middleware.zeus.common.model.ClusterComponentsDto;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.PodInfo;
import com.middleware.zeus.service.components.AbstractBaseOperator;
import com.middleware.zeus.service.components.api.FsMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2023/5/31 2:47 下午
 */
@Service
@Operator(paramTypes4One = String.class)
@Slf4j
public class FsMonitorServiceImpl extends AbstractBaseOperator implements FsMonitorService {


    @Override
    public boolean support(String name) {
        return ComponentsEnum.FS_EXPORTER.getName().equals(name);
    }


    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String setValues = "global.repository=" + repository + "/";
        return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ComponentsEnum.FS_EXPORTER.getName(), "middleware-operator", setValues,
                componentsPath + File.separator + "fs-exporter", cluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.FS_EXPORTER.getName());
    }

    @Override
    public void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster){

    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", ComponentsEnum.FS_EXPORTER.getName())
                .stream().filter(pod -> !pod.getPodName().contains("install-crds")).collect(Collectors.toList());
    }


}
