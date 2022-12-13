package com.harmonycloud.zeus.integration.cluster.client;

import com.middleware.caas.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.middleware.tool.api.client.BaseClient;

/**
 * @author dengyulong
 * @date 2021/05/20
 */
public class GrafanaClient extends BaseClient {

    public GrafanaClient(String basePath) {
        super(basePath);
        setVerifyingSsl(false);
    }

    public GrafanaClient(MiddlewareClusterMonitorInfo grafana) {
        super(grafana.getProtocol(), grafana.getHost(), Integer.parseInt(grafana.getPort()), "");
        setVerifyingSsl(false);
    }

}
