package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import lombok.experimental.Accessors;

/**
 * @description
 * @author  liyinlong
 * @since 2022/8/26 3:44 下午
 */
@Accessors(chain = true)
public class IngressRouteTcpList extends DefaultKubernetesResourceList<IngressRouteTcp> {

}
