package com.harmonycloud.zeus.integration.cluster;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.integration.cluster.bean.Maintenance;
import com.harmonycloud.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * @author xutianhong
 * @Date 2023/1/10 3:55 下午
 */
@Component
public class MaintenanceWrapper {

    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(MAINTENANCE_MIDDLEWARE_HC_CN)
            .withVersion(V1_ALPHA1)
            .withScope(NAMESPACED)
            .withPlural(MAINTENANCES)
            .build();

    /**
     * 创建运维组件
     * 
     * @param clusterId
     * @param maintenance
     * @throws IOException
     */
    public void create(String clusterId, Maintenance maintenance) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(maintenance.getMetadata().getNamespace(),
            JSONObject.parseObject(JSONObject.toJSONString(maintenance)));
    }

}
