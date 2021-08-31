package com.harmonycloud.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.integration.cluster.bean.ScheduleBackupCRD;
import com.harmonycloud.zeus.integration.cluster.bean.ScheduleBackupList;
import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author xutianhong
 * @Date 2021/4/2 3:14 下午
 */
@Component
public class ScheduleBackupWrapper {

    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(MIDDLEWARE_MYSQL_GROUP)
            .withVersion(MIDDLEWARE_INCLUDE_VERSION)
            .withScope(NAMESPACED)
            .withPlural(MYSQL_BACKUP_SCHEDULE)
            .build();

    /**
     * 获取定时备份列表
     */
    public List<ScheduleBackupCRD> list(String clusterId, String namespace){
        Map<String, Object> map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace);
        ScheduleBackupList scheduleBackupList = JSONObject.parseObject(JSONObject.toJSONString(map), ScheduleBackupList.class);
        if (scheduleBackupList == null || CollectionUtils.isEmpty(scheduleBackupList.getItems())){
            return null;
        }
        return scheduleBackupList.getItems();
    }


    /**
     * 创建定时备份
     */
    public void create(String clusterId, ScheduleBackupCRD scheduleBackupCRD) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(
            scheduleBackupCRD.getMetadata().getNamespace(),
            JSONObject.parseObject(JSONObject.toJSONString(scheduleBackupCRD)));
    }


    /**
     * 删除定时备份
     */
    public void delete(String clusterId, String namespace, String name) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).delete(namespace, name);
    }

}
