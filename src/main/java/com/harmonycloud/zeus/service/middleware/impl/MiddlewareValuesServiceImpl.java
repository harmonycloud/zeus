package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareValues;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.middleware.MiddlewareValuesService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author xutianhong
 * @Date 2021/12/16 4:15 下午
 */
@Service
@Slf4j
public class MiddlewareValuesServiceImpl implements MiddlewareValuesService {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private HelmChartService helmChartService;

    @Override
    public JSONObject get(String clusterId, String namespace, String name) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        return helmChartService.getInstalledValues(name, namespace, cluster);
    }

    @Override
    public void update(MiddlewareValues middlewareValues) {
        Middleware middleware = new Middleware();
        BeanUtils.copyProperties(middlewareValues, middleware);
        middleware.setChartName(middlewareValues.getType());
        JSONObject oldValues =
            this.get(middlewareValues.getClusterId(), middlewareValues.getNamespace(), middlewareValues.getName());
        helmChartService.upgrade(middleware, oldValues, middlewareValues.getValues(),
            clusterService.findById(middlewareValues.getClusterId()));
    }
}
