package com.harmonycloud.zeus.integration.registry.client;

import static com.harmonycloud.caas.common.constants.NameConstant.ADMIN;

import com.harmonycloud.caas.common.model.middleware.Registry;
import com.harmonycloud.tool.api.client.BaseClient;

/**
 * @author dengyulong
 * @date 2021/03/29
 */
public class V1HarborClient extends BaseClient {

    public V1HarborClient() {
        super("/api");
        setVerifyingSsl(false);
        // 注册当前实例到工厂
        RegistryClientFactory.register(RegistryClientFactory.generateVersion("harbor", "v1"), this);
    }

    public V1HarborClient(String protocol, String host, int port) {
        super(protocol, host, port, "/api");
        setVerifyingSsl(false);
        // 注册当前实例到工厂
        RegistryClientFactory.register(RegistryClientFactory.generateVersion("harbor", "v1"), this);
    }

    public V1HarborClient(Registry registry) {
        super("/api");
        setVerifyingSsl(false);
        this.resetAddr(registry.getProtocol(), registry.getAddress(), registry.getPort()).addHttpBasicAuth(ADMIN,
            registry.getUser(), registry.getPassword());
        // 注册当前实例到工厂
        RegistryClientFactory.register(RegistryClientFactory.generateVersion("harbor", "v1"), this);
    }

}