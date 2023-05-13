package com.middleware.zeus.integration.cluster.client;

import com.middleware.zeus.util.api.client.BaseClient;

/**
 * @author xutianhong
 * @Date 2021/3/31 4:47 下午
 */
public class PrometheusClient extends BaseClient {


    public PrometheusClient(String basePath) {
        super(basePath);
        setVerifyingSsl(false);
    }

    public PrometheusClient(String protocol, String host, int port, String basePath) {
        super(protocol, host, port, basePath);
        setVerifyingSsl(false);
    }
}
