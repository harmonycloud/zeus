package com.harmonycloud.zeus.service.components.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.LoggingService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/12/8 2:10 下午
 */
@Service
@Operator(paramTypes4One = String.class)
public class LVMServiceImpl extends AbstractBaseOperator implements LoggingService {

    @Override
    public boolean support(String name) {
        return ComponentsEnum.LVM.getName().equals(name);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {

    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.LVM.getName());
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.upgradeInstall(ComponentsEnum.LVM.getName(), "middleware-operator", setValues,
                componentsPath + File.separator + "lvm-csi-plugin", cluster);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, String type) {
        String setValues = "image.repository=" + repository;
        if (cluster.getStorage().containsKey("lvm")){
            JSONObject lvm = JSONObject.parseObject(JSONObject.toJSONString(cluster.getStorage().get("lvm")));
            setValues = setValues + ",storage.vgName=" + lvm.getOrDefault("vgName", "vg_middleware") +
                    "storage.size=" +  lvm.getOrDefault("vgName", "vg_middleware");
        }
        return setValues;
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {

    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", ComponentsEnum.LVM.getName());
    }


}
