package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.Protocol;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.ClusterComponentsDto;
import com.middleware.zeus.common.model.PrometheusResponse;
import com.middleware.zeus.common.model.PrometheusRulesResponse;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.middleware.zeus.integration.cluster.api.PrometheusApi;
import com.middleware.zeus.integration.cluster.client.PrometheusClient;
import com.middleware.zeus.service.k8s.ClusterComponentService;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.util.api.client.BaseClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

import static com.middleware.zeus.common.constants.NameConstant.ADMIN;

/**
 * @author xutianhong
 * @Date 2021/3/31 4:45 下午
 */
@Component
public class PrometheusWrapper {

    private static final Map<String, PrometheusClient> PROMETHEUS_CLIENT_MAP = new HashMap<>();

    @Autowired
    private ClusterService clusterService;
    @Value("${k8s.monitoring.prometheus.port:31900}")
    private String prometheusPort;
    @Autowired
    private ClusterComponentService clusterComponentService;

    // 清理缓存
    public void clearCache(String clusterId) {
        PROMETHEUS_CLIENT_MAP.remove(clusterId);
    }

    public PrometheusResponse get(String clusterId, String prometheusApiVersion, Map<String, String> queryMap)
        throws Exception {
        String authName = null;
        PrometheusClient client = createApi(clusterId, prometheusApiVersion);
        if (!CollectionUtils.isEmpty(client.getAuthentications())){
            authName = ADMIN;
        }
        PrometheusResponse prometheusResponse;
        try {
            prometheusResponse = new PrometheusApi(client).get("", queryMap, authName);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorMessage.THE_CONNECTION_ADDRESS_FOR_PROMETHEUS_IS_INCORRECT);
        }
        return prometheusResponse;
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
        if (StringUtils.isAnyEmpty(prometheus.getProtocol(), prometheus.getHost())) {
            throw new BusinessException(ErrorMessage.PROMETHEUS_NOT_INSTALLED);
        }
        if (PROMETHEUS_CLIENT_MAP.containsKey(clusterId)) {
            return PROMETHEUS_CLIENT_MAP.get(clusterId);
        }
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
        PROMETHEUS_CLIENT_MAP.put(clusterId, client);
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
