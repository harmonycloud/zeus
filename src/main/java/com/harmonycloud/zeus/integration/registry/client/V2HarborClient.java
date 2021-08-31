package com.harmonycloud.zeus.integration.registry.client;

import static com.harmonycloud.caas.common.constants.NameConstant.ADMIN;

import com.harmonycloud.caas.common.model.middleware.Registry;
import com.harmonycloud.tool.api.client.BaseClient;

/**
 * @author dengyulong
 * @date 2021/05/24
 */
public class V2HarborClient extends BaseClient {

    public V2HarborClient() {
        super("/api/v2.0");
        setVerifyingSsl(false);
        // 注册当前实例到工厂
        RegistryClientFactory.register(RegistryClientFactory.generateVersion("harbor", "v2"), this);
    }

    public V2HarborClient(String protocol, String host, int port) {
        super(protocol, host, port, "/api/v2.0");
        setVerifyingSsl(false);
        // 注册当前实例到工厂
        RegistryClientFactory.register(RegistryClientFactory.generateVersion("harbor", "v2"), this);
    }

    public V2HarborClient(Registry registry) {
        super("/api/v2.0");
        setVerifyingSsl(false);
        this.resetAddr(registry.getProtocol(), registry.getAddress(), registry.getPort()).addHttpBasicAuth(ADMIN,
            registry.getUser(), registry.getPassword());
        // 注册当前实例到工厂
        RegistryClientFactory.register(RegistryClientFactory.generateVersion("harbor", "v2"), this);
    }

}
