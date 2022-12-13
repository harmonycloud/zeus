package com.harmonycloud.zeus.integration.cluster;

import java.util.Map;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.zeus.integration.cluster.api.PrometheusApi;
import com.harmonycloud.zeus.integration.cluster.client.PrometheusClient;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.middleware.caas.common.enums.Protocol;
import com.middleware.caas.common.model.PrometheusRulesResponse;
import com.middleware.caas.common.model.PrometheusResponse;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import org.springframework.util.CollectionUtils;

import static com.middleware.caas.common.constants.NameConstant.ADMIN;

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
    @Autowired
    private ClusterComponentService clusterComponentService;

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
        ClusterComponentsDto componentsDto = clusterComponentService.get(clusterId, "prometheus");
        if (componentsDto != null
            && StringUtils.isNotEmpty(componentsDto.getUsername())
            && StringUtils.isNotEmpty(componentsDto.getPassword())) {
            client.addHttpBasicAuth(ADMIN, componentsDto.getUsername(),
                    componentsDto.getPassword());
        }
        return client;
    }

    private MiddlewareClusterMonitorInfo getPrometheusInfo(MiddlewareClusterDTO cluster) {
        ClusterComponentsDto clusterComponentsDto = clusterComponentService.get(cluster.getId(), "prometheus");
        if (clusterComponentsDto==null){
            throw new BusinessException(ErrorMessage.PROMETHEUS_NOT_INSTALLED);
        }
        MiddlewareClusterMonitorInfo prometheus = new MiddlewareClusterMonitorInfo();
        BeanUtils.copyProperties(clusterComponentsDto,prometheus);
        if (StringUtils.isBlank(prometheus.getProtocol())){
            prometheus.setProtocol(Protocol.HTTP.getValue().toLowerCase());
        }
        if (StringUtils.isBlank(prometheus.getPort())){
            prometheus.setPort(prometheusPort);
        }
        if (StringUtils.isEmpty(prometheus.getAddress())) {
            prometheus.setAddress(prometheus.getProtocol() + "://" + prometheus.getHost() + ":" + prometheus.getPort());
        }
        return prometheus;
    }

}
