package com.harmonycloud.zeus.integration.registry.client;

import static com.harmonycloud.caas.common.constants.NameConstant.ADMIN;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.harmonycloud.caas.common.enums.registry.RegistryType;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.middleware.Registry;
import com.harmonycloud.tool.api.client.BaseClient;

/**
 * @author dengyulong
 * @date 2021/05/24 制品服务客户端 工厂类
 */
public class RegistryClientFactory {

    private static final Map<String, BaseClient> REGISTRY_CLIENT_MAP = new HashMap<>();
    private static final String DEFAULT_VERSION = RegistryType.HARBOR.getType() + "-v1";

    /**
     * 注册客户端
     */
    public static void register(String version, BaseClient baseClient) {
        REGISTRY_CLIENT_MAP.put(version, baseClient);
    }

    /**
     * 获取客户端
     */
    public static BaseClient getClient(Registry registry) {
        String version;
        if (StringUtils.isBlank(registry.getVersion())) {
            version = DEFAULT_VERSION;
            registry.setVersion("v1");
        } else {
            version = generateVersion(registry.getType(), registry.getVersion().contains(".")
                ? registry.getVersion().substring(0, registry.getVersion().indexOf(".")) : registry.getVersion());
        }

        BaseClient baseClient = REGISTRY_CLIENT_MAP.get(version);
        if (baseClient == null) {
            if (RegistryType.HARBOR.getType().equals(registry.getType())) {
                if (registry.getVersion().startsWith("v2")) {
                    baseClient = new V2HarborClient();
                } else {
                    baseClient = new V1HarborClient();
                }
            }
        }
        if (baseClient == null) {
            throw new CaasRuntimeException("Unsupported registry type : " + version);
        }
        return baseClient.resetAddr(registry.getProtocol(), registry.getAddress(), registry.getPort())
            .addHttpBasicAuth(ADMIN, registry.getUser(), registry.getPassword());
    }

    public static String generateVersion(String type, String shortVersion) {
        return type + "-" + shortVersion;
    }

}
