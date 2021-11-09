package com.harmonycloud.zeus.service.k8s.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.service.AbstractBaseService;
import com.harmonycloud.zeus.service.components.BaseComponentsService;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.registry.HelmChartService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/05/18
 */
@Slf4j
@Service
public class ClusterComponentServiceImpl extends AbstractBaseService implements ClusterComponentService {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private HelmChartService helmChartService;

    @Override
    public void deploy(MiddlewareClusterDTO cluster, String componentName, String type) {
        BaseComponentsService service = getOperator(BaseComponentsService.class, BaseComponentsService.class, componentName);
        service.deploy(cluster, type);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster, String componentName) {
        BaseComponentsService service = getOperator(BaseComponentsService.class, BaseComponentsService.class, componentName);
        service.integrate(cluster);
    }

    @Override
    public void list(String clusterId, String componentName) {
        //接入对应解绑
    }

    /**
     * 回滚helm release
     */
    private void rollbackHelmRelease(MiddlewareClusterDTO cluster, String releaseName, String namespace) {
        Middleware middleware = new Middleware().setName(releaseName).setNamespace(namespace);
        helmChartService.uninstall(middleware, cluster);
    }

}
