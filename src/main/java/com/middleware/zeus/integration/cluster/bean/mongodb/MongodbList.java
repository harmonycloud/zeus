package com.middleware.zeus.integration.cluster.bean.mongodb;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/8/1 13:12
 */
@Accessors(chain = true)
public class MongodbList extends DefaultKubernetesResourceList<Mongodb> {
}
