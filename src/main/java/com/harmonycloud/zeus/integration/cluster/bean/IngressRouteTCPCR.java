package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;

import java.util.Map;

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

    public IngressRouteTCPCR() {
    }

    public IngressRouteTCPCR(String apiVersion, String kind, ObjectMeta metadata, IngressRouteTCPSpec spec) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.metadata = metadata;
        this.spec = spec;
    }

    public IngressRouteTCPCR(String name, String namespace, String entryPoint, String serviceName, Integer servicePort) {
        this.metadata = new ObjectMeta();
        this.metadata.setName(name);
        this.metadata.setNamespace(namespace);
        this.spec = new IngressRouteTCPSpec(entryPoint, serviceName, servicePort);
    }

    public IngressRouteTCPCR(String name, String namespace, String entryPoint, String serviceName, Integer servicePort, Map<String, String> labels) {
        this.metadata = new ObjectMeta();
        this.metadata.setName(name);
        this.metadata.setNamespace(namespace);
        this.metadata.setLabels(labels);
        this.spec = new IngressRouteTCPSpec(entryPoint, serviceName, servicePort);
    }
}
