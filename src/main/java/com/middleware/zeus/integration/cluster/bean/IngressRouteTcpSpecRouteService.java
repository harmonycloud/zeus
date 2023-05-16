package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2022/8/26 3:23 下午
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngressRouteTcpSpecRouteService {

    private String name;

    private String match;

    private String namespace;

    private Integer port;

    private Object proxyProtocol;

    private Integer terminationDelay;

    private Integer weight;

    public IngressRouteTcpSpecRouteService() {
    }

    public IngressRouteTcpSpecRouteService(String name, Integer port) {
        this.name = name;
        this.port = port;
    }

}
