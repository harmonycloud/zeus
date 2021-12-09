package com.harmonycloud.zeus.service.components.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.middleware.StorageClassProvisionerEnum;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.LoggingService;
import com.harmonycloud.zeus.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xutianhong
 * @Date 2021/12/8 2:10 下午
 */
@Slf4j
@Service
@Operator(paramTypes4One = String.class)
public class LVMServiceImpl extends AbstractBaseOperator implements LoggingService {

    private static final Map<String, String> size = new ConcurrentHashMap<>();

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
        cluster.getStorage().remove(StorageClassProvisionerEnum.CSI_LVM.getType());
        clusterService.update(cluster);
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.upgradeInstall(ComponentsEnum.LVM.getName(), "middleware-operator", setValues,
                componentsPath + File.separator + "lvm-csi-plugin", cluster);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String url = getMinioUrl(cluster);
        AssertUtil.notBlank(clusterComponentsDto.getVgName(), DictEnum.ACCESS_KEY_ID);
        AssertUtil.notBlank(clusterComponentsDto.getVgName(), DictEnum.SIZE);
        size.put("size", clusterComponentsDto.getSize());
        return "image.repository=" + repository +
                ",storage.vgName=" + clusterComponentsDto.getVgName() +
                ",storage.size=" + clusterComponentsDto.getSize() +
                ",volumeSnapshotClass.bucket=velero" +
                ",volumeSnapshotClass.url=" + url;
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {
        cluster.getStorage().put(StorageClassProvisionerEnum.CSI_LVM.getType(), size.get("lvm"));
        clusterService.update(cluster);
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", ComponentsEnum.LVM.getName());
    }

    public String getMinioUrl(MiddlewareClusterDTO cluster) {
        JSONObject storage = JSONObject.parseObject(JSONObject.toJSONString(cluster.getStorage()));
        if (storage.containsKey("backup") && storage.getJSONObject("backup").containsKey("storage")) {
            return storage.getJSONObject("backup").getJSONObject("storage").getString("endpoint");
        } else {
            log.error("未安装minio，使用默认minio地址");
            return "http://" + cluster.getHost() + ":31909";
        }
    }

}
