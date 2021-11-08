package com.harmonycloud.zeus.service.components;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author xutianhong
 * @Date 2021/10/29 2:49 下午
 */
public abstract class AbstractBaseOperator {

    @Value("${k8s.component.components:/usr/local/zeus-pv/components}")
    protected String componentsPath;

    @Autowired
    protected HelmChartService helmChartService;
    @Autowired
    protected NamespaceService namespaceService;
    @Autowired
    protected ClusterService clusterService;
    @Autowired
    protected IngressService ingressService;

    public void deploy(MiddlewareClusterDTO cluster){
        //获取仓库地址
        String repository = getRepository(cluster);
        //拼接参数
        String setValues = getValues(repository, cluster);
        //发布组件
        install(setValues,cluster);
        //更新middlewareCluster
        updateCluster(cluster);
    }

    public void uninstall(MiddlewareClusterDTO cluster, String type) {
        //获取分区地址
        //helmChartService.uninstall(cluster, getNamespace(), type);
    }

    /**
     * 获取仓库地址
     */
    protected String getRepository(MiddlewareClusterDTO cluster){
        return cluster.getRegistry().getRegistryAddress() + "/" + cluster.getRegistry().getChartRepo();
    }

    /**
     * 拼接参数
     *
     * @param repository 仓库地址
     * @param cluster 集群对象
     * @return String
     */
    protected abstract String getValues(String repository, MiddlewareClusterDTO cluster);

    /**
     * 发布组件
     *
     * @param setValues 参数
     * @param cluster 集群对象
     */
    protected abstract void install(String setValues, MiddlewareClusterDTO cluster);

    /**
     * 更新集群信息
     *
     * @param cluster 集群对象
     */
    protected abstract void updateCluster(MiddlewareClusterDTO cluster);
}
