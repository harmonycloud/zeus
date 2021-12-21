package com.harmonycloud.zeus.service.middleware;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.middleware.MiddlewareValues;

/**
 * @author xutianhong
 * @Date 2021/12/16 4:15 下午
 */
public interface MiddlewareValuesService {

    /**
     * 获取values.yaml
     *  @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @return JSONObject
     */
    JSONObject get(String clusterId, String namespace, String name);

    /**
     * 更新values.yaml
     * @param middlewareValues
     */
    void update(MiddlewareValues middlewareValues);

}
