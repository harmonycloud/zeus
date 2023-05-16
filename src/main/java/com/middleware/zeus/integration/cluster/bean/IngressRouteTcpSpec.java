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
 * @since 2021/9/15 5:07 下午
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngressRouteTcpSpec {

    private List<String> entryPoints;

    private List<IngressRouteTcpSpecRoute> routes;

    private Object tls;

    public IngressRouteTcpSpec(List<String> entryPoints, List<IngressRouteTcpSpecRoute> routes, Object tls) {
        this.entryPoints = entryPoints;
        this.routes = routes;
        this.tls = tls;
    }

    public IngressRouteTcpSpec(String entryPoint, String serviceName, Integer servicePort) {
        List<String> entryPointList = new ArrayList<>();
        entryPointList.add(entryPoint);
        this.entryPoints = entryPointList;
        this.routes = new ArrayList<>();
        this.routes.add(new IngressRouteTcpSpecRoute(serviceName, servicePort));
    }

}
