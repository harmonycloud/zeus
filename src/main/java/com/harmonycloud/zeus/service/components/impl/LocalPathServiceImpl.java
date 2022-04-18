package com.harmonycloud.zeus.service.components.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.StorageClassProvisionerEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterStorageSupport;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.LocalPathService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.NameConstant.SUPPORT;

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
    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        try {
            super.deploy(cluster, clusterComponentsDto);
        } catch (Exception e){
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains("already exists")) {
                throw new BusinessException(ErrorMessage.LOCAL_PATH_ALREADY_EXIST);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {
        MiddlewareClusterDTO existCluster = clusterService.findById(cluster.getId());
        if (existCluster.getStorage().getSupport() == null) {
            existCluster.getStorage().setSupport(new ArrayList<>());
        }
        List<MiddlewareClusterStorageSupport> existSupport = existCluster.getStorage().getSupport();
        List<MiddlewareClusterStorageSupport> support = cluster.getStorage().getSupport();
        existSupport = existSupport.stream().filter(st -> !ComponentsEnum.LOCAL_PATH.getName().equals(st.getType()))
            .collect(Collectors.toList());
        existSupport.addAll(support);
        existCluster.getStorage().setSupport(existSupport);
        clusterService.update(existCluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.LOCAL_PATH.getName());
        List<MiddlewareClusterStorageSupport> support = cluster.getStorage().getSupport();
        support = support.stream().filter(st -> !ComponentsEnum.LOCAL_PATH.getName().equals(st.getType()))
            .collect(Collectors.toList());
        cluster.getStorage().setSupport(support);
        clusterService.update(cluster);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        return "image.repository=" + repository + "/local-path-provisioner" +
                ",storage.storageClassName=" + "local-path" +
                ",helperImage.repository=" + repository + "/busybox" +
                ",localPath.path=" + "/opt/local-path-provisioner";
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ComponentsEnum.LOCAL_PATH.getName(), "middleware-operator", setValues,
            componentsPath + File.separator + "local-path-provisioner", cluster);
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {
        if (CollectionUtils.isEmpty(cluster.getStorage().getSupport())) {
            cluster.getStorage().setSupport(new ArrayList<>());
        }
        MiddlewareClusterStorageSupport support = new MiddlewareClusterStorageSupport();
        support.setName(ComponentsEnum.LOCAL_PATH.getName());
        support.setType(ComponentsEnum.LOCAL_PATH.getName());
        support.setNamespace("middleware-operator");
        List<MiddlewareClusterStorageSupport> list = new ArrayList<>();
        list.add(support);
        cluster.getStorage().setSupport(list);
        clusterService.update(cluster);
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", ComponentsEnum.LOCAL_PATH.getName());
    }
}
