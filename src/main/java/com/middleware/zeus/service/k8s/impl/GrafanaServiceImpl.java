package com.middleware.zeus.service.k8s.impl;

import com.middleware.caas.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.middleware.zeus.integration.cluster.GrafanaWrapper;
import com.middleware.zeus.integration.cluster.bean.prometheus.GrafanaApiKey;
import com.middleware.zeus.service.k8s.GrafanaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/05/20
 */
@Slf4j
@Service
public class GrafanaServiceImpl implements GrafanaService {

    @Autowired
    private GrafanaWrapper grafanaWrapper;

    @Value("${system.monitor.grafana.key.name:zeus}")
    private String defaultKeyName;

    @Override
    public void setToken(MiddlewareClusterMonitorInfo grafana) {
        if (StringUtils.isAnyBlank(grafana.getProtocol(), grafana.getHost())) {
            return;
        }
        // 先查一遍确定没有key重复
        List<GrafanaApiKey> grafanaApiKeys = grafanaWrapper.listApiKeys(grafana);
        if (!CollectionUtils.isEmpty(grafanaApiKeys)) {
            grafanaApiKeys.forEach(k -> {
                // 跟默认key名称相同的都删掉
                if (k.getName().equals(defaultKeyName)) {
                    grafanaWrapper.deleteApiKey(k.getId(), grafana);
                }
            });
        }
        // 创建一个key
        String token = grafanaWrapper.createApiKey(defaultKeyName, grafana);
        grafana.setToken(token);
    }

}
