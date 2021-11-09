package com.harmonycloud.zeus.service.components.impl;

import java.io.File;
import java.util.List;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterMonitor;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterMonitorInfo;
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

    @Override
    public boolean support(String name) {
        return ComponentsEnum.PROMETHEUS.getName().equals(name);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {
        MiddlewareClusterDTO existCluster = clusterService.findById(cluster.getId());
        if (existCluster.getMonitor() == null){
            existCluster.setMonitor(new MiddlewareClusterMonitor());
        }
        existCluster.getMonitor().setPrometheus(cluster.getMonitor().getPrometheus());
        clusterService.updateCluster(existCluster);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, String type) {
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
       if (SIMPLE.equals(type)) {
           setValues = setValues + ",replicas.prometheus=1";
       } else {
           setValues = setValues + ",replicas.prometheus=3";
       }
       return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.upgradeInstall("prometheus", "default", setValues,
                componentsPath + File.separator + "prometheus", cluster);
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {
        MiddlewareClusterMonitorInfo prometheus = new MiddlewareClusterMonitorInfo();
        prometheus.setProtocol("http").setPort("31901").setHost(cluster.getHost());
        cluster.getMonitor().setPrometheus(prometheus);
        clusterService.updateCluster(cluster);
    }

    public void checkExist(MiddlewareClusterDTO cluster) {
        List<HelmListInfo> helmListInfos = helmChartService.listHelm("", "", cluster);
        if (cluster.getMonitor().getPrometheus() != null
            || helmListInfos.stream().anyMatch(helm -> "prometheus".equals(helm.getName()))) {
            throw new BusinessException(ErrorMessage.EXIST);
        }
    }
}
