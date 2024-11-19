package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.ClusterComponentsDto;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.middleware.zeus.integration.cluster.api.AlertManagerApi;
import com.middleware.zeus.integration.cluster.client.AlertManagerClient;
import com.middleware.zeus.service.k8s.ClusterComponentService;
import com.middleware.zeus.service.k8s.ClusterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;

import static com.middleware.zeus.common.constants.NameConstant.ADMIN;
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
    @Autowired
    private ClusterComponentService clusterComponentService;

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
        ClusterComponentsDto clusterComponentsDto = clusterComponentService.get(cluster.getId(), "alertmanager");
        if (clusterComponentsDto != null
                && StringUtils.isNotEmpty(clusterComponentsDto.getUsername())
                && StringUtils.isNotEmpty(clusterComponentsDto.getPassword())) {
            client.addHttpBasicAuth(ADMIN, clusterComponentsDto.getUsername(),
                    clusterComponentsDto.getPassword());
        }
        return client;
    }

    private MiddlewareClusterMonitorInfo getAlertManagerInfo(MiddlewareClusterDTO cluster) {
        MiddlewareClusterMonitorInfo alertManager = new MiddlewareClusterMonitorInfo();
        ClusterComponentsDto clusterComponentsDto = clusterComponentService.get(cluster.getId(), "alertmanager");
        if (clusterComponentsDto == null) {
            throw new BusinessException(ErrorMessage.ALERT_MANAGER_NOT_INSTALLED);
        }
        alertManager.setProtocol(clusterComponentsDto.getProtocol());
        alertManager.setHost(clusterComponentsDto.getHost());
        alertManager.setPort(clusterComponentsDto.getPort());

        if (StringUtils.isEmpty(alertManager.getAddress())) {
            alertManager
                    .setAddress(alertManager.getProtocol() + "://" + alertManager.getHost() + ":" + alertManager.getPort());
        }
        return alertManager;
    }

}
