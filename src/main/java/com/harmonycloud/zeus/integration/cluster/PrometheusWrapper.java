package com.harmonycloud.zeus.integration.cluster;

import java.util.Map;

import com.harmonycloud.zeus.integration.cluster.api.PrometheusApi;
import com.harmonycloud.zeus.integration.cluster.client.PrometheusClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.harmonycloud.caas.common.enums.Protocol;
import com.harmonycloud.caas.common.model.PrometheusRulesResponse;
import com.harmonycloud.caas.common.model.PrometheusResponse;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.harmonycloud.zeus.service.k8s.ClusterService;

/**
 * @author xutianhong
 * @Date 2021/3/31 4:45 下午
 */
@Component
public class PrometheusWrapper {

    @Autowired
    private ClusterService clusterService;
    @Value("${k8s.monitoring.prometheus.port:30003}")
    private String prometheusPort;
    @Value("${k8s.monitoring.alertManager.port:31252}")
    private String alertManagerPort;

    public PrometheusResponse getMonitorInfo(String clusterId, String prometheusApiVersion,
                                             Map<String, String> queryMap) throws Exception {

        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        MiddlewareClusterMonitorInfo prometheus = getPrometheusInfo(cluster);
        PrometheusApi prometheusApi = new PrometheusApi(
                new PrometheusClient(prometheus.getProtocol(), prometheus.getHost(), Integer.parseInt(prometheus.getPort()),
                        prometheus.getAddress()
                                .replace(prometheus.getProtocol() + "://" + prometheus.getHost() + ":" + prometheus.getPort(), "")
                                + prometheusApiVersion));
        return prometheusApi.getMonitorInfo("", queryMap);
    }


    public PrometheusRulesResponse getRules(String clusterId, String prometheusApiVersion) throws Exception {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        MiddlewareClusterMonitorInfo prometheus = getPrometheusInfo(cluster);
        PrometheusApi prometheusApi = new PrometheusApi(
                new PrometheusClient(prometheus.getProtocol(), prometheus.getHost(), Integer.parseInt(prometheus.getPort()),
                        prometheus.getAddress()
                                .replace(prometheus.getProtocol() + "://" + prometheus.getHost() + ":" + prometheus.getPort(), "")
                                + prometheusApiVersion));
        return prometheusApi.getRules();
    }

    public void setSilence(String clusterId, String prometheusApiVersion, Map<String, Object> body) throws Exception {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        MiddlewareClusterMonitorInfo alertManager = getAlertManagerInfo(cluster);
        PrometheusApi prometheusApi = new PrometheusApi(new PrometheusClient(alertManager.getProtocol(),
            alertManager.getHost(), Integer.parseInt(alertManager.getPort()),
            alertManager.getAddress()
                .replace(alertManager.getProtocol() + "://" + alertManager.getHost() + ":" + alertManager.getPort(), "")
                + prometheusApiVersion));
        prometheusApi.setSilence(body);
    }

    private MiddlewareClusterMonitorInfo getPrometheusInfo(MiddlewareClusterDTO cluster) {
        MiddlewareClusterMonitorInfo prometheus;
        if (cluster.getMonitor() == null || cluster.getMonitor().getPrometheus() == null) {
            prometheus = new MiddlewareClusterMonitorInfo().setProtocol(Protocol.HTTP.getValue().toLowerCase())
                    .setHost(cluster.getIngress().getAddress()).setPort(prometheusPort);
        } else {
            prometheus = cluster.getMonitor().getPrometheus();
            if (StringUtils.isBlank(cluster.getMonitor().getPrometheus().getProtocol())) {
                prometheus.setProtocol(Protocol.HTTP.getValue().toLowerCase());
            }
            if (StringUtils.isBlank(cluster.getMonitor().getPrometheus().getHost())) {
                prometheus.setHost(cluster.getIngress().getAddress());
            }
            if (StringUtils.isBlank(cluster.getMonitor().getPrometheus().getPort())) {
                prometheus.setPort(prometheusPort);
            }
        }
        if (StringUtils.isEmpty(prometheus.getAddress())) {
            prometheus.setAddress(prometheus.getProtocol() + "://" + prometheus.getHost() + ":" + prometheus.getPort());
        }
        return prometheus;
    }

    private MiddlewareClusterMonitorInfo getAlertManagerInfo(MiddlewareClusterDTO cluster) {
        MiddlewareClusterMonitorInfo alertManager;
        if (cluster.getMonitor() == null || cluster.getMonitor().getAlertManager() == null) {
            alertManager = new MiddlewareClusterMonitorInfo().setProtocol(Protocol.HTTP.getValue().toLowerCase())
                    .setHost(cluster.getIngress().getAddress()).setPort(alertManagerPort);
        } else {
            alertManager = cluster.getMonitor().getAlertManager();
            if (StringUtils.isBlank(cluster.getMonitor().getAlertManager().getProtocol())) {
                alertManager.setProtocol(Protocol.HTTP.getValue().toLowerCase());
            }
            if (StringUtils.isBlank(cluster.getMonitor().getAlertManager().getHost())) {
                alertManager.setHost(cluster.getIngress().getAddress());
            }
            if (StringUtils.isBlank(cluster.getMonitor().getAlertManager().getPort())) {
                alertManager.setPort(alertManagerPort);
            }
        }
        if (StringUtils.isEmpty(alertManager.getAddress())) {
            alertManager
                    .setAddress(alertManager.getProtocol() + "://" + alertManager.getHost() + ":" + alertManager.getPort());
        }
        return alertManager;
    }

}
