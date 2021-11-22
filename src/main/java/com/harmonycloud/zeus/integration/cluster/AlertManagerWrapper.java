package com.harmonycloud.zeus.integration.cluster;

import java.util.Map;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
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
import org.springframework.util.CollectionUtils;

import static com.harmonycloud.caas.common.constants.NameConstant.ADMIN;
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
        String authName = null;
        AlertManagerClient client = createApi(cluster, alertManagerApiVersion);
        if (!CollectionUtils.isEmpty(client.getAuthentications())){
            authName = ADMIN;
        }
        new AlertManagerApi(client).setSilence(body, authName);
    }

    public AlertManagerClient createApi(MiddlewareClusterDTO cluster, String alertManagerApiVersion) {
        MiddlewareClusterMonitorInfo alertManager = getAlertManagerInfo(cluster);
        AlertManagerClient client = new AlertManagerClient(alertManager.getProtocol(), alertManager.getHost(),
            Integer.parseInt(alertManager.getPort()),
            alertManager.getAddress()
                .replace(alertManager.getProtocol() + "://" + alertManager.getHost() + ":" + alertManager.getPort(), "")
                + alertManagerApiVersion);
        if (cluster.getMonitor() != null && cluster.getMonitor().getAlertManager() != null
            && StringUtils.isNotEmpty(cluster.getMonitor().getAlertManager().getUsername())
            && StringUtils.isNotEmpty(cluster.getMonitor().getAlertManager().getPassword())) {
            client.addHttpBasicAuth(ADMIN, cluster.getMonitor().getAlertManager().getUsername(),
                cluster.getMonitor().getAlertManager().getPassword());
        }
        return client;
    }

    private MiddlewareClusterMonitorInfo getAlertManagerInfo(MiddlewareClusterDTO cluster) {
        MiddlewareClusterMonitorInfo alertManager;
        if (cluster.getMonitor() == null || cluster.getMonitor().getAlertManager() == null) {
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        } else {
            alertManager = cluster.getMonitor().getAlertManager();
            if (StringUtils.isBlank(cluster.getMonitor().getAlertManager().getProtocol())) {
                alertManager.setProtocol(Protocol.HTTP.getValue().toLowerCase());
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
