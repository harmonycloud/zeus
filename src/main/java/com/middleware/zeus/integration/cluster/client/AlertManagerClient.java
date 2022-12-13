package com.middleware.zeus.integration.cluster.client;

import com.middleware.tool.api.client.BaseClient;

/**
 * @author xutianhong
 * @Date 2021/11/16 7:21 下午
 */
public class AlertManagerClient extends BaseClient {

    public AlertManagerClient(String basePath) {
        super(basePath);
        setVerifyingSsl(false);
    }

    public AlertManagerClient(String protocol, String host, int port, String basePath) {
        super(protocol, host, port, basePath);
        setVerifyingSsl(false);
    }

}
