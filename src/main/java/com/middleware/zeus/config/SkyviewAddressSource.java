package com.middleware.zeus.config;

import com.dtflys.forest.callback.AddressSource;
import com.dtflys.forest.http.ForestAddress;
import com.dtflys.forest.http.ForestRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @description 实现 AddressSource 接口
 * @author  liyinlong
 * @since 2022/6/20 9:03 上午
 */
@Slf4j
@Configuration
public class SkyviewAddressSource implements AddressSource {

    @Value("${system.skyview.host:caas-gateway-svc.caas-system}")
    private String host;
    @Value("${system.skyview.port:8081}")
    private int port;

    @Override
    public ForestAddress getAddress(ForestRequest request) {
        return new ForestAddress(host, port);
    }
}