package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liyinlong
 * @since 2022/8/26 3:23 下午
 */
@Data
public class IngressRouteTCPSpecRoute {

    private String match;

    private List<Object> middlewares;

    private Integer priority;

    private List<IngressRouteTCPSpecRouteService> services;

    public IngressRouteTCPSpecRoute(String serviceName, Integer servicePort) {
        this.match = "HostSNI(`*`)";
        services = new ArrayList<>();
        services.add(new IngressRouteTCPSpecRouteService(serviceName, servicePort));
    }

}
