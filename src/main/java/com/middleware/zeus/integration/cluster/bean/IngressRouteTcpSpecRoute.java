package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liyinlong
 * @since 2022/8/26 3:23 下午
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class IngressRouteTcpSpecRoute {

    private String match;

    private List<Object> middlewares;

    private Integer priority;

    private List<IngressRouteTcpSpecRouteService> services;


    public IngressRouteTcpSpecRoute(String serviceName, Integer servicePort) {
        this.match = "HostSNI(`*`)";
        services = new ArrayList<>();
        services.add(new IngressRouteTcpSpecRouteService(serviceName, servicePort));
    }

}
