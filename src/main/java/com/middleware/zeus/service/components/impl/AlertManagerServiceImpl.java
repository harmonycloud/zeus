package com.middleware.zeus.service.components.impl;

import com.middleware.caas.common.enums.ComponentsEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ClusterComponentsDto;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.PodInfo;
import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.service.components.AbstractBaseOperator;
import com.middleware.zeus.service.components.api.AlertManagerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static com.middleware.caas.common.constants.CommonConstant.SIMPLE;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/10/29 2:47 下午
 */
@Service
@Operator(paramTypes4One = String.class)
@Slf4j
public class AlertManagerServiceImpl extends AbstractBaseOperator implements AlertManagerService {

    @Override
    public boolean support(String name) {
        return ComponentsEnum.ALERTMANAGER.getName().equals(name);
    }

    @Override
    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        if (namespaceService.list(cluster.getId()).stream().noneMatch(ns -> "monitoring".equals(ns.getName()))){
            //创建分区
            namespaceService.save(cluster.getId(), "monitoring", null, null);
        }
        //发布alertManager
        try {
            super.deploy(cluster, clusterComponentsDto);
        } catch (Exception e){
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains("no matches for kind")) {
                log.error("识别部分资源类型失败", e);
                throw new BusinessException(ErrorMessage.CRD_NOT_EXISTED);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        //uninstall
        if (status != 1){
            helmChartService.uninstall(cluster, "monitoring", ComponentsEnum.ALERTMANAGER.getName());
        }
    }

    @Override
    public String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto){
        String setValues = "image.alertmanager.repository=" + repository + "/alertmanager" +
                ",clusterHost=" + cluster.getHost();
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            setValues = setValues + ",replicas=1";
        }else {
            setValues = setValues + ",replicas=3";
        }
        return setValues;

    }

    @Override
    public void install(String setValues, MiddlewareClusterDTO cluster){
        helmChartService.installComponents(ComponentsEnum.ALERTMANAGER.getName(), "monitoring", setValues,
                componentsPath + File.separator + "alertmanager", cluster);
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
            clusterComponentsDto.setPort("31902");
        }
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", ComponentsEnum.ALERTMANAGER.getName());
        return podService.list(clusterId, "monitoring", labels);
    }

    @Override
    public void setStatus(ClusterComponentsDto clusterComponentsDto) {
        if (StringUtils.isAnyEmpty(clusterComponentsDto.getProtocol(), clusterComponentsDto.getHost(),
            clusterComponentsDto.getPort())) {
            clusterComponentsDto.setStatus(7);
        }
    }

}
