package com.harmonycloud.zeus.service.components.impl;

import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.MiddlewareControllerService;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author xutianhong
 * @Date 2021/11/1 5:50 下午
 */
@Service
@Operator(paramTypes4One = String.class)
public class MiddlewareControllerServiceImpl extends AbstractBaseOperator implements MiddlewareControllerService {

    @Override
    public boolean support(String name) {
        return ComponentsEnum.MIDDLEWARE_CONTROLLER.getName().equals(name);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {

    }
    
    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, String type) {
        return "global.repository=" + cluster.getRegistry().getRegistryAddress() + "/"
            + cluster.getRegistry().getChartRepo();
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.upgradeInstall("middleware-controller", "middleware-operator", setValues,
            componentsPath + File.separator + "platform", cluster);
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {

    }
    
}
