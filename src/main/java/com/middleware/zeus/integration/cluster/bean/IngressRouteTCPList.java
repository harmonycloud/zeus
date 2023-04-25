package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @description
 * @author  liyinlong
 * @since 2022/8/26 3:44 下午
 */
@Accessors(chain = true)
public class IngressRouteTCPList extends DefaultKubernetesResourceList<IngressRouteTCPCR> {

}
