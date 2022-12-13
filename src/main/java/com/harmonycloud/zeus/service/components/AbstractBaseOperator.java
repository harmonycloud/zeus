package com.harmonycloud.zeus.service.components;

import com.middleware.caas.common.enums.ComponentsEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ClusterComponentsDto;
import com.middleware.caas.common.model.middleware.ImageRepositoryDTO;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.bean.BeanClusterComponents;
import com.harmonycloud.zeus.dao.BeanClusterComponentsMapper;
import com.harmonycloud.zeus.integration.cluster.PvcWrapper;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.k8s.PodService;
import com.harmonycloud.zeus.service.middleware.ImageRepositoryService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static com.middleware.caas.common.constants.CommonConstant.*;

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
    @Autowired
    protected PodService podService;
    @Autowired
    protected BeanClusterComponentsMapper beanClusterComponentsMapper;
    @Autowired
    protected PvcWrapper pvcWrapper;
    @Autowired
    private ImageRepositoryService imageRepositoryService;

    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto){
        //获取仓库地址
        String repository = getRepository(cluster);
        //拼接参数
        String setValues = getValues(repository, cluster, clusterComponentsDto);
        //发布组件
        install(setValues,cluster);
        //更新middlewareCluster
        initAddress(clusterComponentsDto, cluster);
    }

    public void updateStatus(MiddlewareClusterDTO cluster, BeanClusterComponents beanClusterComponents) {
        List<PodInfo> podInfoList = getPodInfoList(cluster.getId());
        // 默认正常
        int status = beanClusterComponents.getStatus();
        if (CollectionUtils.isEmpty(podInfoList) && status != NUM_TWO && status != NUM_SIX) {
            // 未安装
            status = NUM_ZERO;
        }
        // 删除中状态的后续变化只能是未安装
        else if (!CollectionUtils.isEmpty(podInfoList) && status != NUM_FIVE) {
            if (beanClusterComponents.getComponent().equals(ComponentsEnum.MIDDLEWARE_CONTROLLER.getName())
                && podInfoList.stream()
                    .anyMatch(pod -> "Running".equals(pod.getStatus()) || "Completed".equals(pod.getStatus()))) {
                // 特殊处理middleware-controller 状态正常
                status = NUM_THREE;
            } else if (podInfoList.stream()
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

    public void setStatus(ClusterComponentsDto clusterComponentsDto){
    }

    /**
     * 获取仓库地址
     */
    protected String getRepository(MiddlewareClusterDTO cluster){
        List<ImageRepositoryDTO> imageRepositoryDTOList = imageRepositoryService.list(cluster.getId());
        if (CollectionUtils.isEmpty(imageRepositoryDTOList)){
            throw new BusinessException(ErrorMessage.CLUSTER_NOT_ADD_REPOSITORY);
        }
        return imageRepositoryDTOList.get(0).getRegistryAddress() + "/" + imageRepositoryDTOList.get(0).getProject();
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
     * @param clusterComponentsDto 集群组件对象
     */
    protected abstract void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster);

    /**
     * 更新集群信息
     *
     * @param clusterId 集群id
     */
    protected abstract List<PodInfo> getPodInfoList(String clusterId);
}
