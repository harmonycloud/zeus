package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;

/**
 * @author liyinlong
 * @since 2022/8/26 3:23 下午
 */
@Data
public class IngressRouteTCPSpecRouteService {

    private String name;

    private String match;

    private String namespace;

    private String port;

    private Object proxyProtocol;

    private Integer terminationDelay;

    private Integer weight;

    public IngressRouteTCPSpecRouteService(String name, String port) {
        this.name = name;
        this.port = port;
    }

}
