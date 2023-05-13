package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2021/10/22 12:03 上午
 */
@Accessors(chain = true)
public class MiddlewareRestoreList extends DefaultKubernetesResourceList<MiddlewareRestoreCR> {

}
