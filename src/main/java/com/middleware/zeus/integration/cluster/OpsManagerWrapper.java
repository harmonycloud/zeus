package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareList;
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
 * @Date 2025/8/1 11:57
 */
@Component
@Slf4j
public class OpsManagerWrapper {

    public OpsManager get(String clusterId, String namespace, String name) {
        try {
            // init client
            NonNamespaceOperation<OpsManager, OpsManagerList, Resource<OpsManager>> opsManagerClient =
                    K8sClient.getClient(clusterId).resources(OpsManager.class, OpsManagerList.class);
            // 条件判断
            if (StringUtils.isNotEmpty(namespace)) {
                opsManagerClient =
                        ((MixedOperation<OpsManager, OpsManagerList, Resource<OpsManager>>)opsManagerClient)
                                .inNamespace(namespace);
            }
            return opsManagerClient.withName(name).get();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) {
                throw new BusinessException(DictEnum.MIDDLEWARE, name, ErrorMessage.NOT_EXIST);
            }
            throw e;
        }
    }


    public void patch(String clusterId, OpsManager opsManager) {
        // init client
        NonNamespaceOperation<OpsManager, OpsManagerList, Resource<OpsManager>> opsManagerClient =
                K8sClient.getClient(clusterId).resources(OpsManager.class, OpsManagerList.class);

        // patch
        opsManagerClient.resource(opsManager).patch();
    }

}
