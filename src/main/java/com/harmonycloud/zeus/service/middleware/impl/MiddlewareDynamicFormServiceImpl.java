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
            helmChartService.getHelmChartFromLocal(chartName, chartVersion);
        return helmChartService.getQuestionYaml(helmChartFile);
    }

    public Map<String, List<Middleware>> detailCustomMiddleware(String clusterId, String namesapce){
        //根据数据库获取自定义中间件  并过滤已有的四款中间件
        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoService.list(clusterId);
        mwInfoList = mwInfoList.stream().filter(mw -> {
            String tar = mw.getChartName() + "-" + mw.getChartVersion();
            return filterBasicMiddleware(tar);
        }).collect(Collectors.toList());
        //查询helm list 和 helm get values
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        List<HelmListInfo> infoList = helmChartService.listHelm(namesapce, "", cluster);
        return null;
    }

    
    public boolean filterBasicMiddleware(String tar) {
        return !"mysql-0.1.0".equals(tar) && !"redis-5.0.8".equals(tar) && !"rocketmq-4.1.0".equals(tar)
            && !"elasticsearch-6.8.10-1".equals(tar);
    }
}
