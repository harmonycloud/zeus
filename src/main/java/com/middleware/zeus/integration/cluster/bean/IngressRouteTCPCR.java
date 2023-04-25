package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.Data;

import java.util.Map;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @description
 * @author  liyinlong
 * @since 2022/8/26 3:10 下午
 */
@Group(TRAEFIC_GROUP)
@Version(V1_ALPHA1)
@Plural(TRAEFIC_PLURAL)
public class IngressRouteTCPCR extends CustomResource<IngressRouteTCPSpec, Void> implements Namespaced {

    public IngressRouteTCPCR(String name, String namespace, String entryPoint, String serviceName, Integer servicePort,
        Map<String, String> labels) {
        setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).withLabels(labels).build());
        setSpec(new IngressRouteTCPSpec(entryPoint, serviceName, servicePort));
    }

}
