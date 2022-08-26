package com.harmonycloud.zeus.service.ingress;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.bean.BeanClusterComponents;
import com.harmonycloud.zeus.bean.BeanIngressComponents;
import com.harmonycloud.zeus.dao.BeanClusterComponentsMapper;
import com.harmonycloud.zeus.dao.BeanIngressComponentsMapper;
import com.harmonycloud.zeus.integration.cluster.PvcWrapper;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

import static com.harmonycloud.caas.common.constants.CommonConstant.*;

/**
 * @description
 * @author  liyinlong
 * @since 2022/8/22 5:06 下午
 */
public abstract class AbstractBaseOperator {

    @Value("${system.upload.path:/usr/local/zeus-pv/upload}")
    protected String uploadPath;
    @Value("${k8s.component.components:/usr/local/zeus-pv/components}")
    protected String componentsPath;
    protected static final String SUB_DIR = "/helmcharts/";

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
    protected BeanIngressComponentsMapper beanIngressComponentsMapper;
    @Autowired
    protected ClusterCertService clusterCertService;

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
        if (CollectionUtils.isEmpty(podInfoList) && status != NUM_TWO && status != NUM_SIX) {
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
     * 保存到数据库
     */
    public void insert(String clusterId, IngressComponentDto ingressComponentDto, Integer status) {
        BeanIngressComponents beanIngressComponents = new BeanIngressComponents();
        beanIngressComponents.setClusterId(clusterId);
        BeanUtils.copyProperties(ingressComponentDto, beanIngressComponents);
        beanIngressComponents.setName(ingressComponentDto.getIngressClassName());
        beanIngressComponents.setStatus(status);
        beanIngressComponents.setCreateTime(new Date());
        beanIngressComponentsMapper.insert(beanIngressComponents);
    }

    public void installSuccessCheck(MiddlewareClusterDTO cluster, IngressComponentDto ingressComponentDto) {
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>()
                .eq("cluster_id", cluster.getId()).eq("ingress_class_name", ingressComponentDto.getIngressClassName());
        BeanIngressComponents ingressComponents = beanIngressComponentsMapper.selectOne(wrapper);
        if (ingressComponents.getStatus() == 2) {
            ingressComponents.setStatus(6);
            beanIngressComponentsMapper.updateById(ingressComponents);
        }
    }

    /**
     * 检查是否已存在同名ingress
     * @param ingressComponentDto
     */
    public void checkIfExists(IngressComponentDto ingressComponentDto) {
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>().eq("ingress_class_name",
                ingressComponentDto.getIngressClassName());
        BeanIngressComponents beanIngressComponents = beanIngressComponentsMapper.selectOne(wrapper);
        if (beanIngressComponents != null) {
            throw new BusinessException(ErrorMessage.INGRESS_CLASS_EXISTED);
        }
    }

    private BeanIngressComponents getAndCheckExists(String clusterId, String ingressClassName) {
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>().eq("ingress_class_name",
                ingressClassName);
        wrapper.eq("cluster_id", clusterId);
        BeanIngressComponents beanIngressComponents = beanIngressComponentsMapper.selectOne(wrapper);
        if (beanIngressComponents == null) {
            throw new BusinessException(ErrorMessage.INGRESS_CLASS_NOT_EXISTED);
        }
        return beanIngressComponents;
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
