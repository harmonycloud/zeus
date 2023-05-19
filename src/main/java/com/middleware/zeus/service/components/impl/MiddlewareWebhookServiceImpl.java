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
import com.middleware.zeus.service.components.api.MiddlewareWebhookService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/5/17 9:37 下午
 */
@Slf4j
@Service
@Operator(paramTypes4One = String.class)
public class MiddlewareWebhookServiceImpl extends AbstractBaseOperator implements MiddlewareWebhookService {

    @Override
    public boolean support(String name) {
        return ComponentsEnum.MIDDLEWARE_WEBHOOK.getName().equals(name);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String setValues = "image.repository=" + repository;
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            setValues = setValues + ",replicaCount=1";
        } else {
            setValues = setValues + ",replicaCount=3";
        }
        return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ComponentsEnum.MIDDLEWARE_WEBHOOK.getName(), "middleware-operator", setValues,
                componentsPath + File.separator + "middleware-admission-webhook", cluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.MIDDLEWARE_WEBHOOK.getName());
    }

    @Override
    public void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster){

    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", ComponentsEnum.MIDDLEWARE_WEBHOOK.getName());
    }

}
