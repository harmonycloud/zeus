package com.harmonycloud.zeus.integration.cluster;

import static com.harmonycloud.caas.common.constants.NameConstant.FOUR_ZERO_FOUR;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.zeus.integration.cluster.bean.BackupList;
import com.harmonycloud.zeus.integration.cluster.bean.MaintenanceList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.integration.cluster.bean.Maintenance;
import com.harmonycloud.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * @author xutianhong
 * @Date 2023/1/10 3:55 下午
 */
@Component
@Slf4j
public class MaintenanceWrapper {

    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(MAINTENANCE_MIDDLEWARE_HC_CN)
            .withVersion(V1_ALPHA1)
            .withScope(NAMESPACED)
            .withPlural(MAINTENANCES)
            .build();

    /**
     * 根据labels查询运维组件
     *
     * @param clusterId
     * @param namespace
     * @param labels
     */
    public List<Maintenance> listByLabels(String clusterId, String namespace, Map<String, String> labels) {
        Map<String, Object> map = null;
        try {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace, labels);
        } catch (Exception e){
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains(FOUR_ZERO_FOUR)) {
                log.error("Maintenance crd未部署");
            } else {
                throw e;
            }
        }
        if (CollectionUtils.isEmpty(map)){
            return null;
        }
        MaintenanceList maintenanceList = JSONObject.parseObject(JSONObject.toJSONString(map), MaintenanceList.class);
        if (CollectionUtils.isEmpty(maintenanceList.getItems())){
            return new ArrayList<>();
        }
        return maintenanceList.getItems();
    }

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

    public List<Maintenance> list(String clusterId, String namespace) {
        try {
            Map<String, Object> map;
            if ("*".equals(namespace)) {
                map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(null);
            } else {
                map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace);
            }
            MaintenanceList maintenanceList = JSONObject.parseObject(JSONObject.toJSONString(map), MaintenanceList.class);
            if (maintenanceList == null || CollectionUtils.isEmpty(maintenanceList.getItems())){
                return new ArrayList<>();
            }
            return maintenanceList.getItems();
        } catch (Exception e){
            log.error("查询Maintenance失败", e);
        }
        return new ArrayList<>();
    }

    public void delete(String clusterId, String namespace, String name) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).delete(namespace, name);
    }

}
