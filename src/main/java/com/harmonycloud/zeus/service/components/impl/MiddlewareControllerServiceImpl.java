package com.harmonycloud.zeus.service.components.impl;

import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.MiddlewareControllerService;
import org.springframework.stereotype.Service;
import static com.harmonycloud.caas.common.constants.CommonConstant.SIMPLE;

import java.io.File;
import java.util.List;

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
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String setValues = "global.repository=" + cluster.getRegistry().getRegistryAddress() + "/"
            + cluster.getRegistry().getChartRepo();
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            setValues = setValues + ",global.middleware_controller.replicas=1";
        } else {
            setValues = setValues + ",global.middleware_controller.replicas=3";
        }
        return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ComponentsEnum.MIDDLEWARE_CONTROLLER.getName(), "middleware-operator", setValues,
            componentsPath + File.separator + "platform", cluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.MIDDLEWARE_CONTROLLER.getName());
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {

    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", ComponentsEnum.MIDDLEWARE_CONTROLLER.getName());
    }
    
}
