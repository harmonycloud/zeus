package com.harmonycloud.zeus.service.k8s;

import com.middleware.caas.common.model.middleware.MiddlewareClusterMonitorInfo;

/**
 * @author dengyulong
 * @date 2021/05/20
 */
public interface GrafanaService {

    /**
     * 获取token
     *
     * @param grafana grafana信息
     */
    void setToken(MiddlewareClusterMonitorInfo grafana);

}
