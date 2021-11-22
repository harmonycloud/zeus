package com.harmonycloud.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.integration.cluster.bean.MysqlScheduleBackupCRD;
import com.harmonycloud.zeus.integration.cluster.bean.ScheduleBackupList;
import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.Alias;
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
    public List<MysqlScheduleBackupCRD> list(String clusterId, String namespace){
        Map<String, Object> map = null;
        try {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace);
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
    public void create(String clusterId, MysqlScheduleBackupCRD mysqlScheduleBackupCRD) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(
            mysqlScheduleBackupCRD.getMetadata().getNamespace(),
            JSONObject.parseObject(JSONObject.toJSONString(mysqlScheduleBackupCRD)));
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
     * @param mysqlScheduleBackupCRD
     * @throws IOException
     */
    public void update(String clusterId, MysqlScheduleBackupCRD mysqlScheduleBackupCRD) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(
                mysqlScheduleBackupCRD.getMetadata().getNamespace(),
                JSONObject.parseObject(JSONObject.toJSONString(mysqlScheduleBackupCRD)));
    }

    /**
     * 查询备份规则
     * @param clusterId
     * @param namespace
     * @param backupScheduleName
     * @return
     */
    public MysqlScheduleBackupCRD get(String clusterId, String namespace, String backupScheduleName){
        Map<String, Object> map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, backupScheduleName);
        MysqlScheduleBackupCRD mysqlScheduleBackupCRD = JSONObject.parseObject(JSONObject.toJSONString(map), MysqlScheduleBackupCRD.class);
        return mysqlScheduleBackupCRD;
    }

}
