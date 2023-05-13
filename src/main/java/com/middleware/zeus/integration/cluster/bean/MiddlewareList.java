package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import lombok.experimental.Accessors;

/**
 * @author tangtx
 * @date 2021/03/26 11:00 AM
 */
@Accessors(chain = true)
public class MiddlewareList extends DefaultKubernetesResourceList<MiddlewareCR> {
}
