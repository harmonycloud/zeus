package com.harmonycloud.zeus.integration.cluster;

import java.util.Map;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
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
import org.springframework.util.CollectionUtils;

import static com.harmonycloud.caas.common.constants.NameConstant.ADMIN;

/**
 * @author xutianhong
 * @Date 2021/3/31 4:45 下午
 */
@Component
public class PrometheusWrapper {

    @Autowired
    private ClusterService clusterService;
    @Value("${k8s.monitoring.prometheus.port:31900}")
    private String prometheusPort;
    @Value("${k8s.monitoring.alertManager.port:31902}")
    private String alertManagerPort;

    public PrometheusResponse get(String clusterId, String prometheusApiVersion, Map<String, String> queryMap)
        throws Exception {
        String authName = null;
        PrometheusClient client = createApi(clusterId, prometheusApiVersion);
        if (!CollectionUtils.isEmpty(client.getAuthentications())){
            authName = ADMIN;
        }
        return new PrometheusApi(client).get("", queryMap, authName);
    }

    public PrometheusRulesResponse getRules(String clusterId, String prometheusApiVersion) throws Exception {
        String authName = null;
        PrometheusClient client = createApi(clusterId, prometheusApiVersion);
        if (!CollectionUtils.isEmpty(client.getAuthentications())){
            authName = ADMIN;
        }
        return new PrometheusApi(client).getRules(authName);
    }

    public PrometheusClient createApi(String clusterId, String prometheusApiVersion) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        MiddlewareClusterMonitorInfo prometheus = getPrometheusInfo(cluster);
        PrometheusClient client =
                new PrometheusClient(prometheus.getProtocol(), prometheus.getHost(), Integer.parseInt(prometheus.getPort()),
                        prometheus.getAddress()
                                .replace(prometheus.getProtocol() + "://" + prometheus.getHost() + ":" + prometheus.getPort(), "")
                                + prometheusApiVersion);
        if (cluster.getMonitor() != null && cluster.getMonitor().getPrometheus() != null
            && StringUtils.isNotEmpty(cluster.getMonitor().getPrometheus().getUsername())
            && StringUtils.isNotEmpty(cluster.getMonitor().getPrometheus().getPassword())) {
            client.addHttpBasicAuth(ADMIN, cluster.getMonitor().getPrometheus().getUsername(),
                cluster.getMonitor().getPrometheus().getPassword());
        }
        return client;
    }

    private MiddlewareClusterMonitorInfo getPrometheusInfo(MiddlewareClusterDTO cluster) {
        MiddlewareClusterMonitorInfo prometheus;
        if (cluster.getMonitor() == null || cluster.getMonitor().getPrometheus() == null) {
            throw new BusinessException(ErrorMessage.PROMETHEUS_NOT_INSTALLED);
        } else {
            prometheus = cluster.getMonitor().getPrometheus();
            if (StringUtils.isBlank(cluster.getMonitor().getPrometheus().getProtocol())) {
                prometheus.setProtocol(Protocol.HTTP.getValue().toLowerCase());
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

}
