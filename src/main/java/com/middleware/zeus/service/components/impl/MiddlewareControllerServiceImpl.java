package com.middleware.zeus.service.components.impl;

import com.middleware.caas.common.enums.ComponentsEnum;
import com.middleware.caas.common.model.ClusterComponentsDto;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.PodInfo;
import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.service.components.AbstractBaseOperator;
import com.middleware.zeus.service.components.api.MiddlewareControllerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import static com.middleware.caas.common.constants.CommonConstant.SIMPLE;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2021/11/1 5:50 下午
 */
@Service
@Operator(paramTypes4One = String.class)
public class MiddlewareControllerServiceImpl extends AbstractBaseOperator implements MiddlewareControllerService {

    @Override
    public boolean support(String name) {
        return ComponentsEnum.MIDDLEWARE_CONTROLLER.getName().equals(name);
    }
    
    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String setValues = "global.repository=" + repository;
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            setValues = setValues + ",global.middleware_controller.replicas=1";
        } else {
            setValues = setValues + ",global.middleware_controller.replicas=3";
        }
        return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ComponentsEnum.MIDDLEWARE_CONTROLLER.getName(), "middleware-operator", setValues,
            componentsPath + File.separator + "platform", cluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.MIDDLEWARE_CONTROLLER.getName());
    }

    @Override
    public void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster){
        if (StringUtils.isEmpty(clusterComponentsDto.getProtocol())){
            clusterComponentsDto.setProtocol("http");
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getHost())){
            clusterComponentsDto.setHost(cluster.getHost());
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getPort())){
            clusterComponentsDto.setPort("31808");
        }
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", ComponentsEnum.MIDDLEWARE_CONTROLLER.getName())
            .stream().filter(pod -> !pod.getPodName().contains("install-crds")).collect(Collectors.toList());
    }
    
}
