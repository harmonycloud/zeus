package com.harmonycloud.zeus.service.components.impl;

import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.MinioService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
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
    public void deploy(MiddlewareClusterDTO cluster, String type){
        //创建minio分区
        namespaceService.save(cluster.getId(), "minio", null);
        //发布minio
        super.deploy(cluster, type);
    }
    
    @Override
    public void integrate(MiddlewareClusterDTO cluster) {
        MiddlewareClusterDTO existCluster = clusterService.findById(cluster.getId());
        existCluster.getStorage().put("backup", cluster.getStorage().get("backup"));
        clusterService.updateCluster(existCluster);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, String type) {
        return "image.repository=" + repository +
                ",persistence.storageClass=local-path" +
                ",minioArgs.bucketName=velero" +
                ",service.nodePort=31909";
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.upgradeInstall("minio", "minio", setValues,
                componentsPath + File.separator + "minio/charts/minio", cluster);
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {
        Map<String, String> storage = new HashMap<>();
        storage.put("name", "minio");
        storage.put("bucketName", "velero");
        storage.put("accessKeyId", "minio");
        storage.put("secretAccessKey", "minio123");
        storage.put("endpoint", "http://" + cluster.getHost() + ":31909");
        Map<String, Object> backup = new HashMap<>();
        backup.put("type", "minio");
        backup.put("storage", storage);
        cluster.getStorage().put("backup", backup);
        clusterService.updateCluster(cluster);
    }
}