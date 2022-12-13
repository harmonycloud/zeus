package com.middleware.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liyinlong
 * @since 2021/9/15 5:07 下午
 */
@Data
@Accessors(chain = true)
public class IngressRouteTCPSpec {

    private List<String> entryPoints;

    private List<IngressRouteTCPSpecRoute> routes;

    private Object tls;

    public IngressRouteTCPSpec() {
    }

    public IngressRouteTCPSpec(List<String> entryPoints, List<IngressRouteTCPSpecRoute> routes, Object tls) {
        this.entryPoints = entryPoints;
        this.routes = routes;
        this.tls = tls;
    }

    public IngressRouteTCPSpec(String entryPoint, String serviceName, Integer servicePort) {
        List<String> entryPointList = new ArrayList<>();
        entryPointList.add(entryPoint);
        this.entryPoints = entryPointList;
        this.routes = new ArrayList<>();
        this.routes.add(new IngressRouteTCPSpecRoute(serviceName, servicePort));
    }

}
