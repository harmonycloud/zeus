package com.middleware.zeus.service.components.impl;

import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.common.enums.ComponentsEnum;
import com.middleware.zeus.common.model.ClusterComponentsDto;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.PodInfo;
import com.middleware.zeus.service.components.AbstractBaseOperator;
import com.middleware.zeus.service.components.api.MiddlewareBackupControllerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.CommonConstant.SIMPLE;

/**
 * @author xutianhong
 * @Date 2023/5/17 9:12 下午
 */
@Service
@Slf4j
@Operator(paramTypes4One = String.class)
public class MiddlewareBackupControllerServiceImpl extends AbstractBaseOperator implements MiddlewareBackupControllerService {

    @Override
    public boolean support(String name) {
        return ComponentsEnum.MIDDLEWAREBACKUP_CONTROLLER.getName().equals(name);
    }


    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String setValues = "global.repository=" + repository;
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            setValues = setValues + ",global.middlewarebackup_controller.replicas=1";
        } else {
            setValues = setValues + ",global.middlewarebackup_controller.replicas=3";
        }
        return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ComponentsEnum.MIDDLEWAREBACKUP_CONTROLLER.getName(), "middleware-operator", setValues,
                componentsPath + File.separator + "middleware-backup", cluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.MIDDLEWAREBACKUP_CONTROLLER.getName());
    }

    @Override
    public void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster){

    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", ComponentsEnum.MIDDLEWAREBACKUP_CONTROLLER.getName())
                .stream().filter(pod -> !pod.getPodName().contains("install-crds")).collect(Collectors.toList());
    }


}
