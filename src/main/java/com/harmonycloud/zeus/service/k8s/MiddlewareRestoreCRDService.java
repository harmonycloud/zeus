package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareRestoreCRD;

import java.io.IOException;

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

}
