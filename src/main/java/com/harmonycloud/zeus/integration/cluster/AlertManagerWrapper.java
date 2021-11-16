package com.harmonycloud.zeus.integration.cluster;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.harmonycloud.caas.common.enums.Protocol;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.harmonycloud.zeus.integration.cluster.api.AlertManagerApi;
import com.harmonycloud.zeus.integration.cluster.client.AlertManagerClient;
import com.harmonycloud.zeus.service.k8s.ClusterService;

/**
 * @author xutianhong
 * @Date 2021/11/16 7:13 下午
 */
@Component
public class AlertManagerWrapper {

    @Autowired
    private ClusterService clusterService;
    @Value("${k8s.monitoring.alertManager.port:31252}")
    private String alertManagerPort;

    public void setSilence(String clusterId, String alertManagerApiVersion, Map<String, Object> body) throws Exception {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        AlertManagerApi alertManagerApi = createApi(clusterId, alertManagerApiVersion);
        alertManagerApi.setSilence(body);
    }

    public AlertManagerApi createApi(String clusterId, String alertManagerApiVersion) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        MiddlewareClusterMonitorInfo alertManager = getAlertManagerInfo(cluster);
        return new AlertManagerApi(
                new AlertManagerClient(alertManager.getProtocol(), alertManager.getHost(), Integer.parseInt(alertManager.getPort()),
                        alertManager.getAddress()
                                .replace(alertManager.getProtocol() + "://" + alertManager.getHost() + ":" + alertManager.getPort(), "")
                                + alertManagerApiVersion));
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
