package com.harmonycloud.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupScheduleCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupScheduleList;
import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @description 中间件备份
 * @author  liyinlong
 * @since 2021/9/14 10:52 上午
 */
@Slf4j
@Component
public class MiddlewareBackupScheduleWrapper {

    /**
     * MiddlewareBackupSchedule的context
     */
    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(HARMONY_CLOUD_CN)
            .withVersion(V1)
            .withScope(NAMESPACED)
            .withPlural(MIDDLEWAREBACKUPSCHEDULES)
            .build();

    /**
     * 创建备份
     * @param clusterId
     * @param middlewareBackupScheduleCR
     * @throws IOException
     */
    public void create(String clusterId, MiddlewareBackupScheduleCR middlewareBackupScheduleCR) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(
            middlewareBackupScheduleCR.getMetadata().getNamespace(),
            JSONObject.parseObject(JSONObject.toJSONString(middlewareBackupScheduleCR)));
    }

    /**
     * 更新
     * @param clusterId
     * @param middlewareBackupScheduleCR
     * @throws IOException
     */
    public void update(String clusterId, MiddlewareBackupScheduleCR middlewareBackupScheduleCR) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(middlewareBackupScheduleCR.getMetadata().getNamespace(),
                JSONObject.parseObject(JSONObject.toJSONString(middlewareBackupScheduleCR)));
    }

    /**
     * 删除
     * @param clusterId
     * @param namespace
     * @param name
     * @throws IOException
     */
    public void delete(String clusterId, String namespace, String name) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).delete(namespace, name);
    }

    /**
     * 获取备份
     * @param clusterId
     * @param namespace
     * @param name
     * @return
     */
    public MiddlewareBackupScheduleCR get(String clusterId, String namespace, String name){
        Map<String, Object> map = null;
        try {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, name);
        } catch (Exception e) {
            log.error("查询MiddlewareBackupSchedule出错了", e);
            return null;
        }
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareBackupScheduleCR.class);
    }

    public MiddlewareBackupScheduleList list(String clusterId, String namespace, Map<String,String> labels){
        Map<String, Object> map = null;
        try {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace, labels);
        } catch (Exception e) {
            log.error("查询MiddlewareBackupScheduleList出错了");
            return null;
        }
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareBackupScheduleList.class);
    }

    public MiddlewareBackupScheduleList list(String clusterId, String namespace){
        Map<String, Object> map = null;
        try {
            if ("*".equals(namespace)) {
                map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(null);
            } else {
                map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace);
            }
        } catch (Exception e) {
            log.error("查询MiddlewareBackupScheduleList出错了");
            return null;
        }
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(map), MiddlewareBackupScheduleList.class);
    }
}
