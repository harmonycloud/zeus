package com.harmonycloud.zeus.service.components.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.StorageClassProvisionerEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.LVMService;
import com.harmonycloud.zeus.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.harmonycloud.caas.common.constants.CommonConstant.ALREADY_EXISTED;
import static com.harmonycloud.caas.common.constants.NameConstant.SUPPORT;

/**
 * @author xutianhong
 * @Date 2021/12/8 2:10 下午
 */
@Slf4j
@Service
@Operator(paramTypes4One = String.class)
public class LVMServiceImpl extends AbstractBaseOperator implements LVMService {

    private static final Map<String, String> size = new ConcurrentHashMap<>();

    @Override
    public boolean support(String name) {
        return ComponentsEnum.LVM.getName().equals(name);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {
        MiddlewareClusterDTO existCluster = clusterService.findById(cluster.getId());
        if (!existCluster.getStorage().containsKey(SUPPORT)){
            existCluster.getStorage().put(SUPPORT, new ArrayList<JSONObject>());
        }
        List<JSONObject> existSupport = (List<JSONObject>)existCluster.getStorage().get(SUPPORT);
        List<JSONObject> support = (List<JSONObject>)cluster.getStorage().get(SUPPORT);
        existSupport.addAll(support);
        existCluster.getStorage().put(SUPPORT, existSupport);
        clusterService.update(existCluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.LVM.getName());
        cluster.getStorage().remove(StorageClassProvisionerEnum.CSI_LVM.getType());
        clusterService.update(cluster);
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        try {
            helmChartService.installComponents(ComponentsEnum.LVM.getName(), "middleware-operator", setValues,
                    componentsPath + File.separator + "lvm-csi-plugin", cluster);
        } catch (Exception e){
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains(ALREADY_EXISTED)) {
                throw new BusinessException(ErrorMessage.LVM_ALREADY_EXISTED);
            } else {
                throw e;
            }
        }
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
        if (!cluster.getStorage().containsKey(SUPPORT)){
            cluster.getStorage().put(SUPPORT, new ArrayList<JSONObject>());
        }
        JSONObject object = new JSONObject();
        object.put("name", "middleware-lvm");
        object.put("type", "lvm");
        object.put("namespace", "middleware-operator");
        List<JSONObject> support = (List<JSONObject>)cluster.getStorage().get(SUPPORT);
        support.add(object);
        cluster.getStorage().put(SUPPORT, support);
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
