package com.harmonycloud.zeus.service.components;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.bean.BeanClusterComponents;
import com.harmonycloud.zeus.dao.BeanClusterComponentsMapper;
import com.harmonycloud.zeus.integration.cluster.PvcWrapper;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.k8s.PodService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static com.harmonycloud.caas.common.constants.CommonConstant.*;

/**
 * @author xutianhong
 * @Date 2021/10/29 2:49 下午
 */
public abstract class AbstractBaseOperator {

    @Value("${k8s.component.components:/usr/local/zeus-pv/components}")
    protected String componentsPath;
    /**
     * 默认存储限额
     */
    protected static final String DEFAULT_STORAGE_LIMIT = "100Gi";

    @Autowired
    protected HelmChartService helmChartService;
    @Autowired
    protected NamespaceService namespaceService;
    @Autowired
    protected ClusterService clusterService;
    @Autowired
    protected IngressService ingressService;
    @Autowired
    protected PodService podService;
    @Autowired
    protected BeanClusterComponentsMapper beanClusterComponentsMapper;
    @Autowired
    protected PvcWrapper pvcWrapper;

    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto){
        //获取仓库地址
        String repository = getRepository(cluster);
        //拼接参数
        String setValues = getValues(repository, cluster, clusterComponentsDto);
        //发布组件
        install(setValues,cluster);
        //更新middlewareCluster
        updateCluster(cluster);
    }

    public void updateStatus(MiddlewareClusterDTO cluster, BeanClusterComponents beanClusterComponents) {
        List<PodInfo> podInfoList = getPodInfoList(cluster.getId());
        // 默认正常
        int status = beanClusterComponents.getStatus();
        if (CollectionUtils.isEmpty(podInfoList) && status != NUM_TWO) {
            // 未安装
            status = NUM_ZERO;
        }
        // 删除中状态的后续变化只能是未安装
        else if (!CollectionUtils.isEmpty(podInfoList) && status != NUM_FIVE) {
            if (podInfoList.stream()
                .allMatch(pod -> "Running".equals(pod.getStatus()) || "Completed".equals(pod.getStatus()))) {
                // 正常
                status = NUM_THREE;
            } else if (status != NUM_TWO && status != NUM_SIX) {
                // 异常
                status = NUM_FOUR;
            }
        }
        beanClusterComponents.setStatus(status);
        beanClusterComponentsMapper.updateById(beanClusterComponents);
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
    protected abstract String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto);

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

    /**
     * 更新集群信息
     *
     * @param clusterId 集群id
     */
    protected abstract List<PodInfo> getPodInfoList(String clusterId);
}
