package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.integration.cluster.bean.mongodb.Mongodb;
import com.middleware.zeus.integration.cluster.bean.mongodb.MongodbList;
import com.middleware.zeus.integration.cluster.bean.mongodb.OpsManager;
import com.middleware.zeus.integration.cluster.bean.mongodb.OpsManagerList;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author xutianhong
 * @Date 2025/8/1 13:11
 */
@Component
@Slf4j
public class MongodbWrapper {

    public Mongodb get(String clusterId, String namespace, String name) {
        try {
            // init client
            NonNamespaceOperation<Mongodb, MongodbList, Resource<Mongodb>> mongodbClient =
                    K8sClient.getClient(clusterId).resources(Mongodb.class, MongodbList.class);
            // 条件判断
            if (StringUtils.isNotEmpty(namespace)) {
                mongodbClient =
                        ((MixedOperation<Mongodb, MongodbList, Resource<Mongodb>>)mongodbClient)
                                .inNamespace(namespace);
            }
            return mongodbClient.withName(name).get();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) {
                throw new BusinessException(DictEnum.MIDDLEWARE, name, ErrorMessage.NOT_EXIST);
            }
            throw e;
        }
    }

    public void patch(String clusterId, Mongodb mongodb) {
        // init client
        NonNamespaceOperation<Mongodb, MongodbList, Resource<Mongodb>> mongodbClient =
                K8sClient.getClient(clusterId).resources(Mongodb.class, MongodbList.class);

        mongodbClient.resource(mongodb).patch();
    }

}
