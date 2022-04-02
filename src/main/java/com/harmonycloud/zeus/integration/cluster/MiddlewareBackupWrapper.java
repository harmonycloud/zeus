package com.harmonycloud.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupList;
import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @description 中间件备份记录
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
            .withPlural(MIDDLEWAREBACKUP)
            .build();

    /**
     * 创建备份(立即备份)
     * @param clusterId
     * @param middlewareBackupCR
     * @throws IOException
     */
    public void create(String clusterId, MiddlewareBackupCR middlewareBackupCR) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).create(middlewareBackupCR.getMetadata().getNamespace(),
                JSONObject.parseObject(JSONObject.toJSONString(middlewareBackupCR)));
    }

    /**
     * 删除
     * @param clusterId
     * @param namespace
     * @param name
     * @throws IOException
     */
    public void delete(String clusterId, String namespace, String name) {
        try {
            K8sClient.getClient(clusterId).customResource(CONTEXT).delete(namespace, name);
        } catch (IOException e) {
            throw new BusinessException(ErrorMessage.BACKUP_RECORD_MAY_NOT_EXIST);
        }
    }

    /**
     * 查询备份记录列表
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    public MiddlewareBackupList list(String clusterId, String namespace, Map<String,String> labels){
        Map<String, Object> map = null;
        try {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace, labels);
        } catch (Exception e) {
            log.error("查询MiddlewareBackupList出错了", e);
            return null;
        }
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareBackupList.class);
    }

    public MiddlewareBackupCR get(String clusterId, String namespace, String name) throws IOException {
        Map<String, Object> map = null;
        try {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, name);
        } catch (Exception e) {
            log.error("查询middlewareBackup出错了", e);
        }
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareBackupCR.class);
    }

}
