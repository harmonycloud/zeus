package com.harmonycloud.zeus.service.middleware.impl;

import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.QuestionYaml;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.middleware.MiddlewareDynamicFormService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
