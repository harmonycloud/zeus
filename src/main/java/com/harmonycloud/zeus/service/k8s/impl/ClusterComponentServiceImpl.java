package com.harmonycloud.zeus.service.k8s.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.harmonycloud.caas.common.model.middleware.IngressDTO;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.ServiceDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.Protocol;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterMonitor;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.harmonycloud.zeus.integration.registry.bean.harbor.V1HelmChartVersion;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.GrafanaService;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.service.registry.HelmChartService;

import lombok.extern.slf4j.Slf4j;

import static com.harmonycloud.caas.common.constants.MinioConstant.ACCESS_KEY;
import static com.harmonycloud.caas.common.constants.MinioConstant.ACCESS_KEY_ID;
import static com.harmonycloud.caas.common.constants.MinioConstant.BACKUP;
import static com.harmonycloud.caas.common.constants.MinioConstant.BUCKET_NAME;
import static com.harmonycloud.caas.common.constants.MinioConstant.MINIO;
import static com.harmonycloud.caas.common.constants.MinioConstant.MINIO_SC;
import static com.harmonycloud.caas.common.constants.MinioConstant.SECRET_ACCESS_KEY;
import static com.harmonycloud.caas.common.constants.MinioConstant.SECRET_KEY;
import static com.harmonycloud.caas.common.constants.NameConstant.ENDPOINT;
import static com.harmonycloud.caas.common.constants.NameConstant.NAME;
import static com.harmonycloud.caas.common.constants.NameConstant.STORAGE;
import static com.harmonycloud.caas.common.constants.NameConstant.TYPE;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_EXPOSE_INGRESS;

/**
 * @author dengyulong
 * @date 2021/05/18
 */
@Slf4j
@Service
public class ClusterComponentServiceImpl implements ClusterComponentService {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private GrafanaService grafanaService;
    @Autowired
    private IngressService ingressService;

    @Value("${k8s.component.minio.name:minio}")
    private String minioHelmChartName;
    @Value("${k8s.component.minio.service.port:9000}")
    private String minioServicePort;
    @Value("${k8s.component.monitor.name:prometheus}")
    private String monitorHelmChartName;
    @Value("${k8s.component.monitor.grafana.service.port:3000}")
    private String monitorGrafanaServicePort;
    @Value("${k8s.component.monitor.prometheus.service.port:9090}")
    private String monitorPrometheusServicePort;
    @Value("${k8s.storageclass.default:default}")
    private String defaultStorageClassName;

    @Override
    public void deploy(MiddlewareClusterDTO cluster, String componentName) {
        MiddlewareClusterDTO existCluster = clusterService.findById(cluster.getId());

        // todo 支持自动部署ingress
        // 必须要部署ingress
        if (existCluster.getIngress() == null || StringUtils.isEmpty(existCluster.getIngress().getAddress())
                || existCluster.getIngress().getTcp() == null || !existCluster.getIngress().getTcp().isEnabled()) {
            throw new BusinessException(ErrorMessage.INGRESS_CONTROLLER_FIRST);
        }

        // 部署组件
        deployComponent(existCluster, componentName, cluster);

        // 对接数据
        integrate(existCluster, componentName);
    }

    private void deployComponent(MiddlewareClusterDTO cluster, String componentName, MiddlewareClusterDTO paramCluster) {
        String repository = cluster.getRegistry().getRegistryAddress() + "/" + cluster.getRegistry().getChartRepo();
        switch (componentName) {
            case "storageBackup":
                if (CollectionUtils.isEmpty(paramCluster.getStorage()) || paramCluster.getStorage().get(BACKUP) == null) {
                    throw new IllegalArgumentException("Minio Ingress TCP port is null");
                }
                Map<String, Object> backup = (Map<String, Object>) paramCluster.getStorage().get(BACKUP);
                deployMinio(cluster, paramCluster.getDcId(), repository, backup.get("port").toString());
                break;
            case "monitor":
                if (paramCluster.getMonitor() == null || paramCluster.getMonitor().getGrafana() == null
                    || StringUtils.isBlank(paramCluster.getMonitor().getGrafana().getPort())
                    || paramCluster.getMonitor().getPrometheus() == null
                    || StringUtils.isBlank(paramCluster.getMonitor().getPrometheus().getPort())) {
                    throw new IllegalArgumentException("Grafana port or Prometheus port is null");
                }
                deployMonitor(cluster, paramCluster.getDcId(), repository, paramCluster.getMonitor());
                break;
            default:
                throw new BusinessException(ErrorMessage.CLUSTER_COMPONENT_UNSUPPORTED);
        }
    }

    /**
     * 部署minio
     */
    private void deployMinio(MiddlewareClusterDTO cluster, String namespace, String repository, String port) {
        // 校验端口
        List<ServiceDTO> serviceList = Collections.singletonList(new ServiceDTO().setExposePort(port));
        ingressService.checkIngressTcpPort(cluster, namespace, serviceList);

        // 获取chart最新版本
        V1HelmChartVersion version = getHelmChartLatestVersion(cluster, minioHelmChartName);

        // 拼接更新参数
        String bucketName = minioHelmChartName + "-" + namespace;
        String setValues = "image.repository=" + repository 
                + ",minio.storageClassName=" + defaultStorageClassName 
                + ",minioArgs.bucketName=" + bucketName + ",";
        // 发布
        helmChartService.upgradeInstall(minioHelmChartName, namespace, setValues, minioHelmChartName,
            version.getVersion(), cluster);
        JSONObject values = helmChartService.getInstalledValues(minioHelmChartName, namespace, cluster);

        // 添加Ingress TCP配置
        try {
            String servicePort = values.getJSONObject("service") != null
                && StringUtils.isNotBlank(values.getJSONObject("service").getString("port"))
                    ? values.getJSONObject("service").getString("port") : minioServicePort;
            serviceList.get(0).setServiceName(minioHelmChartName + "-svc").setServicePort(servicePort);
            ingressService.createIngressTcp(cluster, namespace, serviceList, false);
        } catch (Exception e) {
            // 回滚Ingress TCP配置
            rollbackHelmRelease(cluster, minioHelmChartName, namespace);
            throw e;
        }

        // 回填数据
        JSONObject storage = new JSONObject();
        storage.put(ACCESS_KEY_ID, values.getString(ACCESS_KEY));
        storage.put(SECRET_ACCESS_KEY, values.getString(SECRET_KEY));
        storage.put(BUCKET_NAME, bucketName);
        storage.put(NAME, MINIO_SC);
        storage.put(ENDPOINT,
            Protocol.HTTP.getValue().toLowerCase() + "://" + cluster.getIngress().getAddress() + ":" + port);

        JSONObject backup = new JSONObject();
        backup.put(TYPE, MINIO);
        backup.put(STORAGE, storage);
        if (cluster.getStorage() == null) {
            cluster.setStorage(new HashMap<>(5));
        }
        cluster.getStorage().put(BACKUP, backup);
    }

    /**
     * 部署监控
     */
    private void deployMonitor(MiddlewareClusterDTO cluster, String namespace, String repository, 
                               MiddlewareClusterMonitor monitor) {
        String prometheusIngressPort = monitor.getPrometheus().getPort();
        String grafanaIngressPort = monitor.getGrafana().getPort();
        
        // 获取chart最新版本
        V1HelmChartVersion version = getHelmChartLatestVersion(cluster, monitorHelmChartName);

        // 校验端口
        List<ServiceDTO> serviceList = new ArrayList<>(2);
        serviceList.add(new ServiceDTO().setExposePort(grafanaIngressPort));
        serviceList.add(new ServiceDTO().setExposePort(prometheusIngressPort));
        ingressService.checkIngressTcpPort(cluster, namespace, serviceList);
        
        // 拼接更新参数
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
                ",storage.storageClass=" + defaultStorageClassName;
        // 发布
        helmChartService.upgradeInstall(monitorHelmChartName, namespace, setValues, monitorHelmChartName,
            version.getVersion(), cluster);

        try {
            // todo values.yaml里没有配置service信息，因此直接使用默认配置
            serviceList.get(0).setServiceName(monitorHelmChartName + "-grafana")
                .setServicePort(monitorGrafanaServicePort);
            serviceList.get(1).setServiceName(monitorHelmChartName + "-svc")
                .setServicePort(monitorPrometheusServicePort);
            ingressService.createIngressTcp(cluster, namespace, serviceList, false);
        } catch (Exception e) {
            // 回滚helm release
            rollbackHelmRelease(cluster, monitorHelmChartName, namespace);
            throw e;
        }

        // 回填数据
        MiddlewareClusterMonitorInfo prometheus =
            new MiddlewareClusterMonitorInfo().setProtocol(Protocol.HTTP.getValue().toLowerCase())
                .setHost(cluster.getIngress().getAddress()).setPort(prometheusIngressPort);
        MiddlewareClusterMonitorInfo grafana =
            new MiddlewareClusterMonitorInfo().setProtocol(Protocol.HTTP.getValue().toLowerCase())
                .setHost(cluster.getIngress().getAddress()).setPort(grafanaIngressPort);
        try {
            // 生成grafana的api token
            grafanaService.setToken(grafana);
        } catch (Exception e) {
            // 回滚Ingress TCP配置
            rollbackIngress(cluster, namespace, serviceList);
            // 回滚helm release
            rollbackHelmRelease(cluster, monitorHelmChartName, namespace);
            throw e;
        }
        cluster.setMonitor(new MiddlewareClusterMonitor().setPrometheus(prometheus).setGrafana(grafana));
    }

    /**
     * 回滚Ingress TCP配置
     */
    private void rollbackIngress(MiddlewareClusterDTO cluster, String namespace, List<ServiceDTO> serviceList) {
        IngressDTO ingress = new IngressDTO().setExposeType(MIDDLEWARE_EXPOSE_INGRESS)
            .setProtocol(Protocol.TCP.getValue()).setServiceList(serviceList);
        ingressService.delete(cluster.getId(), namespace, null, null, ingress);
    }

    /**
     * 回滚helm release
     */
    private void rollbackHelmRelease(MiddlewareClusterDTO cluster, String releaseName, String namespace) {
        Middleware middleware = new Middleware().setName(releaseName).setNamespace(namespace);
        helmChartService.uninstall(middleware, cluster);
    }

    private V1HelmChartVersion getHelmChartLatestVersion(MiddlewareClusterDTO cluster, String chartName) {
        List<V1HelmChartVersion> versions = helmChartService.listHelmChartVersions(cluster.getRegistry(), chartName);
        if (CollectionUtils.isEmpty(versions)) {
            throw new BusinessException(DictEnum.CHART, chartName, ErrorMessage.NOT_EXIST);
        }
        return versions.get(0);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster, String componentName) {
        MiddlewareClusterDTO existCluster = clusterService.findById(cluster.getId());
        switch (componentName) {
            case "ingress":
                if (cluster.getIngress() == null) {
                    throw new IllegalArgumentException("ingress info is null");
                }
                existCluster.setIngress(cluster.getIngress());
                break;
            case "storageBackup":
                if (cluster.getStorage() == null || cluster.getStorage().get("backup") == null) {
                    throw new IllegalArgumentException("storage backup info is null");
                }
                existCluster.getStorage().put("backup", cluster.getStorage().get("backup"));
                break;
            case "monitor":
                if (cluster.getMonitor() == null || cluster.getMonitor().getPrometheus() == null
                    || cluster.getMonitor().getGrafana() == null) {
                    throw new IllegalArgumentException("monitor info is null");
                }
                existCluster.setMonitor(cluster.getMonitor());
                break;
            default:
        }
        // 更新集群
        clusterService.update(existCluster);
    }

}
