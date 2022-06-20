package com.harmonycloud.zeus.config;

import com.dtflys.forest.callback.AddressSource;
import com.dtflys.forest.http.ForestAddress;
import com.dtflys.forest.http.ForestRequest;
import org.springframework.beans.factory.annotation.Value;

/**
 * @description 实现 AddressSource 接口
 * @author  liyinlong
 * @since 2022/6/20 9:03 上午
 */
public class SkyviewAddressSource implements AddressSource {

    @Value("${system.skyview.ip}")
    private String ip;

    @Override
    public ForestAddress getAddress(ForestRequest request) {
        return new ForestAddress(ip, 80);
    }
}