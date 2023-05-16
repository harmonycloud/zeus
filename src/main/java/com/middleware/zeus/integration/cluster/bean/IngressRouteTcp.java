package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

/**
 * @description
 * @author  liyinlong
 * @since 2022/8/26 3:10 下午
 */
@Accessors(chain = true)
@NoArgsConstructor
@Group(TRAEFIC_GROUP)
@Version(V1_ALPHA1)
@Plural(TRAEFIC_PLURAL)
@Kind(TRAEFIC_KIND)
public class IngressRouteTcp extends CustomResource<IngressRouteTcpSpec, Void> implements Namespaced {

    public IngressRouteTcp(String name, String namespace, String entryPoint, String serviceName, Integer servicePort,
                           Map<String, String> labels) {
        setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).withLabels(labels).build());
        setSpec(new IngressRouteTcpSpec(entryPoint, serviceName, servicePort));
    }

}
