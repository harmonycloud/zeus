package com.middleware.zeus.service.middleware;

import com.middleware.caas.common.model.middleware.QuestionYaml;

/**
 * @author xutianhong
 * @Date 2021/6/8 3:29 下午
 */
public interface MiddlewareDynamicFormService {

    /**
     * 性能监控
     * @param clusterId 集群id
     * @param chartName chart包名称
     * @param chartVersion chart包版本
     * @return string
     */
    QuestionYaml dynamicForm(String clusterId, String chartName, String chartVersion);

}
