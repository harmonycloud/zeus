package com.harmonycloud.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.integration.cluster.bean.BackupCR;
import com.harmonycloud.zeus.integration.cluster.bean.BackupList;
import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author xutianhong
 * @Date 2021/4/6 4:41 下午
 */
@Component
public class BackupWrapper {

    private static final CustomResourceDefinitionContext CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withGroup(MIDDLEWARE_MYSQL_GROUP)
            .withVersion(MIDDLEWARE_INCLUDE_VERSION)
            .withScope(NAMESPACED)
            .withPlural(MYSQL_BACKUP)
            .build();
    
    /**
     * 获取备份
     */
    public List<BackupCR> list(String clusterId, String namespace){
        Map<String, Object> map =null;
        if ("*".equals(namespace)) {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(null);
        } else {
            map = K8sClient.getClient(clusterId).customResource(CONTEXT).list(namespace);
        }
        BackupList backupList = JSONObject.parseObject(JSONObject.toJSONString(map), BackupList.class);
        if (backupList == null || CollectionUtils.isEmpty(backupList.getItems())){
            return new ArrayList<>();
        }
        return backupList.getItems();
    }


    /**
     * 创建备份
     */
    public void create(String clusterId, BackupCR backupCR) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(backupCR.getMetadata().getNamespace(),
            JSONObject.parseObject(JSONObject.toJSONString(backupCR)));
    }

    /**
     * 删除备份
     */
    public void delete(String clusterId, String namespace, String name) throws Exception {
        K8sClient.getClient(clusterId).customResource(CONTEXT).delete(namespace, name);
    }


}
