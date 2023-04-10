package com.middleware.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.integration.cluster.bean.MysqlScheduleBackupCR;
import com.middleware.zeus.integration.cluster.bean.ScheduleBackupList;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.*;
/**
 * @author xutianhong
 * @Date 2021/4/2 3:14 下午
 */
@Slf4j
@Component
public class MysqlScheduleBackupWrapper {

    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(MIDDLEWARE_MYSQL_GROUP)
            .withVersion(MIDDLEWARE_INCLUDE_VERSION)
            .withScope(NAMESPACED)
            .withPlural(MYSQL_BACKUP_SCHEDULE)
            .build();

    /**
     * 获取定时备份列表
     */
    public List<MysqlScheduleBackupCR> list(String clusterId, String namespace){
        Map<String, Object> map = null;
        try {
            if ("*".equals(namespace)) {
                map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(null);
            } else {
                map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace);
            }
        } catch (Exception e) {
            return null;
        }
        ScheduleBackupList scheduleBackupList = JSONObject.parseObject(JSONObject.toJSONString(map), ScheduleBackupList.class);
        if (scheduleBackupList == null || CollectionUtils.isEmpty(scheduleBackupList.getItems())){
            return null;
        }
        return scheduleBackupList.getItems();
    }


    /**
     * 创建定时备份
     */
    public void create(String clusterId, MysqlScheduleBackupCR mysqlScheduleBackupCR) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(
            mysqlScheduleBackupCR.getMetadata().getNamespace(),
            JSONObject.parseObject(JSONObject.toJSONString(mysqlScheduleBackupCR)));
    }


    /**
     * 删除定时备份
     */
    public void delete(String clusterId, String namespace, String name) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).delete(namespace, name);
    }

    /**
     * 更新定时备份
     * @param clusterId
     * @param mysqlScheduleBackupCR
     * @throws IOException
     */
    public void update(String clusterId, MysqlScheduleBackupCR mysqlScheduleBackupCR) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(
                mysqlScheduleBackupCR.getMetadata().getNamespace(),
                JSONObject.parseObject(JSONObject.toJSONString(mysqlScheduleBackupCR)));
    }

    /**
     * 查询备份规则
     * @param clusterId
     * @param namespace
     * @param backupScheduleName
     * @return
     */
    public MysqlScheduleBackupCR get(String clusterId, String namespace, String backupScheduleName){
        Map<String, Object> map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, backupScheduleName);
        MysqlScheduleBackupCR mysqlScheduleBackupCR = JSONObject.parseObject(JSONObject.toJSONString(map), MysqlScheduleBackupCR.class);
        return mysqlScheduleBackupCR;
    }

}
