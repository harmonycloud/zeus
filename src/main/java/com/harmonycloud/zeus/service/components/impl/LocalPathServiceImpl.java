package com.harmonycloud.zeus.service.components.impl;

import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.LocalPathService;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author xutianhong
 * @Date 2021/10/29 4:26 下午
 */
@Service
@Operator(paramTypes4One = String.class)
public class LocalPathServiceImpl extends AbstractBaseOperator implements LocalPathService {
    @Override
    public boolean support(String name) {
        return ComponentsEnum.LOCAL_PATH.getName().equals(name);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {

    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster) {
        return "image.repository=" + repository + "/local-path-provisioner" +
                ",storage.storageClassName=" + "local-path" +
                ",helperImage.repository=" + repository + "/busybox" +
                ",localPath.path=" + "/opt/local-path-provisioner";
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.upgradeInstall("local-path", "middleware-operator", setValues,
            componentsPath + File.separator + "local-path-provisioner", cluster);
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {

    }
}
