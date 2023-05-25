package com.middleware.zeus.service.components.impl;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.middleware.zeus.common.model.ClusterComponentsDto;
import com.middleware.zeus.common.model.middleware.PodInfo;
import com.middleware.zeus.service.k8s.ClusterComponentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.middleware.zeus.common.enums.ComponentsEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.middleware.zeus.service.components.AbstractBaseOperator;
import com.middleware.zeus.service.components.api.PrometheusService;
import static com.middleware.zeus.common.constants.CommonConstant.SIMPLE;

/**
 * @author xutianhong
 * @Date 2021/10/29 2:42 下午
 */
@Service
@Operator(paramTypes4One = String.class)
public class PrometheusServiceImpl extends AbstractBaseOperator implements PrometheusService {

    @Autowired
    private ClusterComponentService clusterComponentService;

    @Override
    public boolean support(String name) {
        return ComponentsEnum.PROMETHEUS.getName().equals(name);
    }

    @Override
    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        if (namespaceService.list(cluster.getId()).stream().noneMatch(ns -> "monitoring".equals(ns.getName()))){
            //创建分区
            namespaceService.save(cluster.getId(), "monitoring", null, null);
        }
        //发布prometheus
        super.deploy(cluster, clusterComponentsDto);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
       String setValues = "prometheus.prometheusSpec.image.repository=" + repository + "/prometheus" +
                ",kube-state-metrics.image.repository=" + repository + "/kube-state-metrics" +
                ",prometheus-node-exporter.image.repository=" + repository + "/node-exporter" +
                ",prometheusOperator.image.repository=" + repository + "/prometheus-operator" +
                ",prometheusOperator.prometheusConfigReloader.image.repository=" + repository + "/prometheus-config-reloader" +
                ",prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.storageClassName=" + "local-path" +
                ",prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.resources.requests.storage=" + "10Gi";
       if (SIMPLE.equals(clusterComponentsDto.getType())) {
           setValues = setValues + ",prometheus.prometheusSpec.replicas=1";
       } else {
           setValues = setValues + ",prometheus.prometheusSpec.replicas=3";
       }
       return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ComponentsEnum.PROMETHEUS.getName(), "monitoring", setValues,
                componentsPath + File.separator + "prometheus", cluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        if (status != 1){
            // uninstall
            helmChartService.uninstall(cluster, "monitoring", ComponentsEnum.PROMETHEUS.getName());
        }
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
            clusterComponentsDto.setPort("31901");
        }
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", ComponentsEnum.PROMETHEUS.getName());
        return podService.list(clusterId, "monitoring", labels);
    }

    public void checkExist(MiddlewareClusterDTO cluster) {
        List<HelmListInfo> helmListInfos = helmChartService.listHelm("", "", cluster);
        ClusterComponentsDto clusterComponentsDto = clusterComponentService.get(cluster.getId(), "prometheus");
        if (clusterComponentsDto != null || helmListInfos.stream().anyMatch(helm -> "prometheus".equals(helm.getName()))) {
            throw new BusinessException(ErrorMessage.EXIST);
        }
    }

    @Override
    public void setStatus(ClusterComponentsDto clusterComponentsDto){
        if (StringUtils.isAnyEmpty(clusterComponentsDto.getProtocol(), clusterComponentsDto.getHost(),
                clusterComponentsDto.getPort())) {
            clusterComponentsDto.setStatus(7);
        }
    }
}
