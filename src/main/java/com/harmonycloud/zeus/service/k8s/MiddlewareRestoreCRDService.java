package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupList;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareRestoreCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareRestoreList;

import java.io.IOException;
import java.util.Map;

/**
 * 中间件恢复
 * @author  liyinlong
 * @since 2021/9/14 10:43 上午
 */
public interface MiddlewareRestoreCRDService {

    /**
     * 创建恢复
     * @param clusterId
     * @param middlewareRestoreCRD
     * @throws IOException
     */
    void create(String clusterId, MiddlewareRestoreCRD middlewareRestoreCRD)  throws IOException;


    /**
     * 删除恢复
     * @param clusterId
     * @param namespace
     * @param name
     * @throws IOException
     */
    void delete(String clusterId, String namespace,String name)  throws IOException;

    /**
     * 查询恢复列表
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    MiddlewareRestoreList list(String clusterId, String namespace, Map<String,String> labels);

}
