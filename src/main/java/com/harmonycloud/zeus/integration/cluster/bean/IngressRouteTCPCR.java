package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;

/**
 * @description
 * @author  liyinlong
 * @since 2022/8/26 3:10 下午
 */
@Data
public class IngressRouteTCPCR {

    private String apiVersion = "traefik.containo.us/v1alpha1";

    private String kind = "IngressRouteTCP";

    private ObjectMeta metadata;

    private IngressRouteTCPSpec spec;

    public IngressRouteTCPCR(String name, String namespace, String entryPoint, String serviceName, String servicePort) {
        this.metadata.setName(name);
        this.metadata.setNamespace(namespace);
        this.spec = new IngressRouteTCPSpec(entryPoint, serviceName, servicePort);
    }

}
