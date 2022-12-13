package com.harmonycloud.zeus.service.middleware.impl;

import com.middleware.caas.common.model.middleware.QuestionYaml;
import com.middleware.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.middleware.MiddlewareDynamicFormService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author xutianhong
 * @Date 2021/6/8 3:29 下午
 */
@Service
@Slf4j
public class MiddlewareDynamicFormServiceImpl implements MiddlewareDynamicFormService {

    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    protected MiddlewareInfoService middlewareInfoService;

    @Override
    public QuestionYaml dynamicForm(String clusterId, String chartName, String chartVersion) {
        HelmChartFile helmChartFile =
            helmChartService.getHelmChartFromMysql(chartName, chartVersion);
        return helmChartService.getQuestionYaml(helmChartFile);
    }
}
