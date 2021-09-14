package com.harmonycloud.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackup;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MysqlReplicateCRD;
import com.harmonycloud.zeus.service.k8s.MiddlewareBackupCRDService;
import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @description 中间件备份
 * @author  liyinlong
 * @since 2021/9/14 10:52 上午
 */
@Slf4j
@Component
public class MiddlewareBackupWrapper {

    /**
     * MiddlewareBackup的context
     */
    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(HARMONY_CLOUD_CN)
            .withVersion(V1)
            .withScope(NAMESPACED)
            .withPlural(MIDDLEWAREBACKUPS)
            .build();

    /**
     * 创建MiddlewareBackup
     */
    public void create(String clusterId, MiddlewareBackupCRD middlewareBackupCRD) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).create(middlewareBackupCRD.getMetadata().getNamespace(),
                JSONObject.parseObject(JSONObject.toJSONString(middlewareBackupCRD)));
    }

    /**
     * 更新MiddlewareBackup
     */
    public void update(String clusterId, MiddlewareBackupCRD middlewareBackupCRD) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(middlewareBackupCRD.getMetadata().getNamespace(),
                JSONObject.parseObject(JSONObject.toJSONString(middlewareBackupCRD)));
    }

    /**
     * 删除MiddlewareBackup复制
     */
    public void delete(String clusterId, String namespace, String name) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).delete(namespace, name);
    }

    /**
     * 查询MiddlewareBackup
     */
    public MiddlewareBackupCRD get(String clusterId, String namespace, String name){
        Map<String, Object> map = null;
        try {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, name);
        } catch (Exception e) {
            log.error("查询MiddlewareBackup出错了");
            return null;
        }
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareBackupCRD.class);
    }

}
