package com.middleware.zeus.service.components.impl;

import static com.middleware.zeus.common.constants.CommonConstant.SIMPLE;

import java.io.File;
import java.util.List;

import com.middleware.zeus.annotation.Operator;
import org.springframework.stereotype.Service;

import com.middleware.zeus.common.enums.ComponentsEnum;
import com.middleware.zeus.common.model.ClusterComponentsDto;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.PodInfo;
import com.middleware.zeus.service.components.AbstractBaseOperator;
import com.middleware.zeus.service.components.api.MiddlewareSchedulerService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/4/7 7:00 下午
 */

@Service
@Slf4j
@Operator(paramTypes4One = String.class)
public class MiddlewareSchedulerServiceImpl extends AbstractBaseOperator implements MiddlewareSchedulerService {

    @Override
    public boolean support(String name) {
        return ComponentsEnum.MIDDLEWARE_SCHEDULER.getName().equals(name);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String setValues = "image.repository=" + repository + "/middleware-scheduler"
            + ",podAntiAffinity=soft,podAntiAffinityTopologKey=kubernetes.io/hostname";
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            setValues = setValues + ",replicaCount=1";
        } else {
            setValues = setValues + ",replicaCount=3";
        }
        return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ComponentsEnum.MIDDLEWARE_SCHEDULER.getName(), "middleware-operator", setValues,
                componentsPath + File.separator + ComponentsEnum.MIDDLEWARE_SCHEDULER.getName(), cluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.MIDDLEWARE_SCHEDULER.getName());
    }

    @Override
    protected void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster) {

    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", ComponentsEnum.MIDDLEWARE_SCHEDULER.getName());
    }

}
