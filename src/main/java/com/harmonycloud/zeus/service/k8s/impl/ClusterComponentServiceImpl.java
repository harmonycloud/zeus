package com.harmonycloud.zeus.service.k8s.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareInfoDTO;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.zeus.bean.BeanClusterComponents;
import com.harmonycloud.zeus.dao.BeanClusterComponentsMapper;
import com.harmonycloud.zeus.service.middleware.MiddlewareManagerService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.service.AbstractBaseService;
import com.harmonycloud.zeus.service.components.BaseComponentsService;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.registry.HelmChartService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dengyulong
 * @date 2021/05/18
 */
@Slf4j
@Service
public class ClusterComponentServiceImpl extends AbstractBaseService implements ClusterComponentService {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private BeanClusterComponentsMapper beanClusterComponentsMapper;
    @Autowired
    private MiddlewareManagerService middlewareManagerService;

    @Override
    public void deploy(MiddlewareClusterDTO cluster, String componentName, String type) {
        BaseComponentsService service = getOperator(BaseComponentsService.class, BaseComponentsService.class, componentName);
        service.deploy(cluster, type);
        record(cluster.getId(), componentName, 2);
    }

    @Override
    public void multipleDeploy(MiddlewareClusterDTO cluster, List<ClusterComponentsDto> componentsDtoList,
        List<MiddlewareInfoDTO> middlewareInfoDTOList) {
        // 优先部署内容local-path
        if (componentsDtoList.stream().anyMatch(
            clusterComponentsDto -> clusterComponentsDto.getComponent().equals(ComponentsEnum.LOCAL_PATH.getName()))) {
            deploy(cluster, ComponentsEnum.LOCAL_PATH.getName(), "");
            componentsDtoList = componentsDtoList.stream().filter(
                clusterComponentsDto -> clusterComponentsDto.getComponent().equals(ComponentsEnum.LOCAL_PATH.getName()))
                .collect(Collectors.toList());
        }
        // 部署组件
        componentsDtoList.forEach(clusterComponentsDto -> this.deploy(cluster, clusterComponentsDto.getComponent(),
            clusterComponentsDto.getType()));

        //部署operator
        middlewareInfoDTOList.forEach(info -> ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                middlewareManagerService.install(cluster.getId(), info.getChartName(), info.getChartVersion());
            } catch (Exception e) {
                log.error("集群{}  operator{} 安装失败", cluster.getId(), info.getChartName());
            }
        }));
    }


    @Override
    public void integrate(MiddlewareClusterDTO cluster, String componentName) {
        BaseComponentsService service = getOperator(BaseComponentsService.class, BaseComponentsService.class, componentName);
        service.integrate(cluster);
        record(cluster.getId(), componentName, 1);
    }

    @Override
    public List<ClusterComponentsDto> list(String clusterId) {
        QueryWrapper<BeanClusterComponents> wrapper =
            new QueryWrapper<BeanClusterComponents>().eq("cluster_id", clusterId);
        List<BeanClusterComponents> clusterComponentsList = beanClusterComponentsMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(clusterComponentsList)) {
            clusterComponentsList = initClusterComponents(clusterId);
        }
        //另起线程更新所有组件状态
        clusterComponentsList.forEach(cc -> ThreadPoolExecutorFactory.executor.execute(() -> {
            if (cc.getStatus() == 1){
                return;
            }
            BaseComponentsService service = getOperator(BaseComponentsService.class, BaseComponentsService.class, cc.getComponent());
            service.updateStatus(clusterService.findById(clusterId), cc.getComponent());
        }));
        return clusterComponentsList.stream().map(cm -> {
            ClusterComponentsDto dto = new ClusterComponentsDto();
            BeanUtils.copyProperties(cm, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 回滚helm release
     */
    private void rollbackHelmRelease(MiddlewareClusterDTO cluster, String releaseName, String namespace) {
        Middleware middleware = new Middleware().setName(releaseName).setNamespace(namespace);
        helmChartService.uninstall(middleware, cluster);
    }

    /**
     * 记入数据库
     */
    private void record(String clusterId, String name, Integer status){
        BeanClusterComponents cm = new BeanClusterComponents();
        cm.setClusterId(clusterId);
        cm.setComponent(name);
        cm.setStatus(status);
        beanClusterComponentsMapper.insert(cm);
    }

    /**
     * 初始化集群组件映照表
     */
    public List<BeanClusterComponents> initClusterComponents(String clusterId) {
        List<BeanClusterComponents> list = new ArrayList<>();
        for (ComponentsEnum e : ComponentsEnum.values()) {
            BeanClusterComponents cc = new BeanClusterComponents();
            cc.setClusterId(clusterId);
            cc.setStatus(0);
            cc.setComponent(e.getName());
            list.add(cc);
            beanClusterComponentsMapper.insert(cc);
        }
        return list;
    }

}
