package com.middleware.zeus.service.aspect;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.model.middleware.Middleware;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.QuestionYaml;

import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/11/12 11:40 上午
 */
public interface AspectService {

    QuestionYaml dynamic();

    void operation(MiddlewareClusterDTO cluster, Middleware middleware, Map<String, String> dynamicValues, JSONObject values);

}
