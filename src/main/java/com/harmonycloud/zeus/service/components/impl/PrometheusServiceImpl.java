package com.harmonycloud.zeus.service.components.impl;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterMonitor;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.PrometheusService;
import static com.harmonycloud.caas.common.constants.CommonConstant.SIMPLE;

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
       String setValues = "image.prometheus.repository=" + repository + "/prometheus" +
                ",image.configmapReload.repository=" + repository + "/configmap-reload" +
                ",image.nodeExporter.repository=" + repository + "/node-exporter" +
                ",image.kubeRbacProxy.repository=" + repository + "/kube-rbac-proxy" +
                ",image.prometheusAdapter.repository=" + repository + "/k8s-prometheus-adapter-amd64" +
                ",image.prometheusOperator.repository=" + repository + "/prometheus-operator" +
                ",image.prometheusConfigReloader.repository=" + repository + "/prometheus-config-reloader" +
                ",image.kubeStateMetrics.repository=" + repository + "/kube-state-metrics" +
                ",image.nodeExporter.repository=" + repository + "/node-exporter" +
                ",image.grafana.repository=" + repository + "/grafana" +
                ",image.dashboard.repository=" + repository + "/k8s-sidecar" +
                ",image.busybox.repository=" + repository + "/grafana" +
                ",storage.storageClass=" + "local-path";
       if (SIMPLE.equals(clusterComponentsDto.getType())) {
           setValues = setValues + ",replicas.prometheus=1";
       } else {
           setValues = setValues + ",replicas.prometheus=3";
       }
       return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ComponentsEnum.PROMETHEUS.getName(), "default", setValues,
                componentsPath + File.separator + "prometheus", cluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        if (status != 1){
            // uninstall
            helmChartService.uninstall(cluster, "default", ComponentsEnum.PROMETHEUS.getName());
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
