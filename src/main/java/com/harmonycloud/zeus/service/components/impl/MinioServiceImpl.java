package com.harmonycloud.zeus.service.components.impl;

import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.MinioService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import static com.harmonycloud.caas.common.constants.CommonConstant.SIMPLE;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/10/29 4:20 下午
 */
@Service
@Operator(paramTypes4One = String.class)
public class MinioServiceImpl extends AbstractBaseOperator implements MinioService {
    @Override
    public boolean support(String name) {
        return ComponentsEnum.MINIO.getName().equals(name);
    }

    @Override
    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto){
        //创建minio分区
        namespaceService.save(cluster.getId(), "minio", null, null);
        //发布minio
        super.deploy(cluster, clusterComponentsDto);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        if (status != 1){
            // uninstall
            helmChartService.uninstall(cluster, "minio", ComponentsEnum.MINIO.getName());
        }
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String setValues = "image.repository=" + repository +
                ",persistence.storageClass=local-path" +
                ",minioArgs.bucketName=velero" +
                ",service.nodePort=31909";
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            setValues = setValues + ",replicas=1,drivesPerNode=4";
        } else {
            setValues = setValues + ",replicas=3,drivesPerNode=2";
        }
        return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ComponentsEnum.MINIO.getName(), "minio", setValues,
                componentsPath + File.separator + "minio/charts/minio", cluster);
    }

    @Override
    public void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster){
        if (StringUtils.isEmpty(clusterComponentsDto.getProtocol())){
            clusterComponentsDto.setProtocol("http");
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getHost())){
            clusterComponentsDto.setHost(cluster.getHost());
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getPort())){
            clusterComponentsDto.setPort("31909");
        }
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "minio", ComponentsEnum.MINIO.getName());
    }
}
