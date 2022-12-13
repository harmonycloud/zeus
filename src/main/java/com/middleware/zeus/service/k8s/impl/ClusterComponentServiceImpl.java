package com.middleware.zeus.service.k8s.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.ComponentsEnum;
import com.middleware.caas.common.model.ClusterComponentsDto;
import com.middleware.caas.common.model.MultipleComponentsInstallDto;
import com.middleware.caas.common.model.middleware.Middleware;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.util.ThreadPoolExecutorFactory;
import com.middleware.tool.date.DateUtils;
import com.middleware.zeus.bean.BeanClusterComponents;
import com.middleware.zeus.dao.BeanClusterComponentsMapper;
import com.middleware.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.middleware.zeus.service.components.api.LoggingService;
import com.middleware.zeus.service.k8s.NamespaceService;
import com.middleware.zeus.service.middleware.MiddlewareManagerService;
import com.middleware.zeus.service.AbstractBaseService;
import com.middleware.zeus.service.k8s.ClusterComponentService;
import com.middleware.zeus.service.k8s.ClusterService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.middleware.zeus.service.components.BaseComponentsService;
import com.middleware.zeus.service.registry.HelmChartService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.middleware.caas.common.constants.CommonConstant.*;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;

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
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private LoggingService loggingService;

    @Override
    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        // 获取service
        BaseComponentsService service =
            getOperator(BaseComponentsService.class, BaseComponentsService.class, clusterComponentsDto.getComponent());
        checkNamespace(clusterComponentsDto);
        // 部署组件
        service.deploy(cluster, clusterComponentsDto);
        // 记录数据库
        record(cluster.getId(), clusterComponentsDto, 2);
        // 检查是否安装成功
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                Thread.sleep(55000);
                installSuccessCheck(cluster, clusterComponentsDto.getComponent());
            } catch (InterruptedException e) {
                log.error("更新组件安装中状态失败");
            }
        });
    }

    @Override
    public void multipleDeploy(MiddlewareClusterDTO cluster, MultipleComponentsInstallDto multipleComponentsInstallDto)
        throws Exception {
        List<ClusterComponentsDto> componentsDtoList = multipleComponentsInstallDto.getClusterComponentsDtoList();
        // 优先部署内容local-path
        if (componentsDtoList.stream().anyMatch(
            clusterComponentsDto -> clusterComponentsDto.getComponent().equals(ComponentsEnum.LOCAL_PATH.getName()))) {
            deploy(cluster, new ClusterComponentsDto().setComponent(ComponentsEnum.LOCAL_PATH.getName()).setType(""));
            componentsDtoList = componentsDtoList.stream().filter(clusterComponentsDto -> !clusterComponentsDto
                .getComponent().equals(ComponentsEnum.LOCAL_PATH.getName())).collect(Collectors.toList());
        }
        // 优先部署prometheus
        if (componentsDtoList.stream().anyMatch(
            clusterComponentsDto -> clusterComponentsDto.getComponent().equals(ComponentsEnum.PROMETHEUS.getName()))) {
            deploy(cluster, new ClusterComponentsDto().setComponent(ComponentsEnum.PROMETHEUS.getName()).setType(""));
            componentsDtoList = componentsDtoList.stream().filter(clusterComponentsDto -> !clusterComponentsDto
                .getComponent().equals(ComponentsEnum.PROMETHEUS.getName())).collect(Collectors.toList());
        }
        // 部署operator
        final CountDownLatch operatorCount =
            new CountDownLatch(multipleComponentsInstallDto.getMiddlewareInfoDTOList().size());
        multipleComponentsInstallDto.getMiddlewareInfoDTOList()
            .forEach(info -> ThreadPoolExecutorFactory.executor.execute(() -> {
                try {
                    middlewareManagerService.install(cluster.getId(), info.getChartName(), info.getChartVersion(),
                        info.getType());
                } catch (Exception e) {
                    log.error("集群{}  operator{} 安装失败", cluster.getId(), info.getChartName());
                } finally {
                    operatorCount.countDown();
                }
            }));
        operatorCount.await();
        // 部署组件
        final CountDownLatch componentsCount = new CountDownLatch(componentsDtoList.size());
        componentsDtoList.forEach(clusterComponentsDto -> ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                this.deploy(cluster, clusterComponentsDto);
            } catch (Exception e) {
                log.error("集群{}  组件{}  安装失败", cluster.getId(), clusterComponentsDto.getComponent(), e);
            } finally {
                componentsCount.countDown();
            }
        }));
        componentsCount.await();
    }

    @Override
    public void integrate(ClusterComponentsDto clusterComponentsDto, Boolean update) {
        if (clusterComponentsDto.getComponent().equals(ComponentsEnum.LOGGING.getName())){
            // 安装卸载log-pilot
            updateLogPilot(clusterComponentsDto);
        }
        Integer status = update ? null : 1;
        record(clusterComponentsDto.getClusterId(), clusterComponentsDto, status);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, String componentName, Integer status) {
        // 删除集群组件
        BaseComponentsService service =
            getOperator(BaseComponentsService.class, BaseComponentsService.class, componentName);
        service.delete(cluster, status);
        // 更新数据库
        QueryWrapper<BeanClusterComponents> wrapper =
            new QueryWrapper<BeanClusterComponents>().eq("cluster_id", cluster.getId()).eq("component", componentName);
        BeanClusterComponents beanClusterComponents = new BeanClusterComponents();
        beanClusterComponents.setClusterId(cluster.getId());
        beanClusterComponents.setComponent(componentName);
        beanClusterComponents.setStatus(status == 1 ? 0 : 5);
        beanClusterComponentsMapper.update(beanClusterComponents, wrapper);
    }

    @Override
    public List<ClusterComponentsDto> list(String clusterId) throws Exception {
        QueryWrapper<BeanClusterComponents> wrapper =
            new QueryWrapper<BeanClusterComponents>().eq("cluster_id", clusterId);
        List<BeanClusterComponents> clusterComponentsList = beanClusterComponentsMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(clusterComponentsList)) {
            clusterComponentsList = initClusterComponents(clusterId);
        }
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 另起线程更新所有组件状态
        final CountDownLatch count = new CountDownLatch(clusterComponentsList.size());
        clusterComponentsList.forEach(cc -> ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                // 接入组件不更新状态
                if (cc.getStatus() == 1) {
                    return;
                }
                BaseComponentsService service =
                    getOperator(BaseComponentsService.class, BaseComponentsService.class, cc.getComponent());
                service.updateStatus(cluster, cc);
            } finally {
                count.countDown();
            }
        }));
        count.await();
        return clusterComponentsList.stream().map(cm -> {
            ClusterComponentsDto dto = new ClusterComponentsDto();
            BeanUtils.copyProperties(cm, dto);
            if (cm.getStatus() == NUM_TWO) {
                dto.setSeconds(DateUtils.getIntervalDays(new Date(), dto.getCreateTime()));
            }
            if (cm.getStatus() == NUM_ONE || cm.getStatus() == NUM_THREE || cm.getStatus() == NUM_FOUR) {
                getOperator(BaseComponentsService.class, BaseComponentsService.class, dto.getComponent()).setStatus(dto);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public ClusterComponentsDto get(String clusterId, String component) {
        QueryWrapper<BeanClusterComponents> wrapper =
            new QueryWrapper<BeanClusterComponents>().eq("cluster_id", clusterId).eq("component", component);
        BeanClusterComponents beanClusterComponents = beanClusterComponentsMapper.selectOne(wrapper);
        if (beanClusterComponents == null) {
            return null;
        }
        ClusterComponentsDto clusterComponentsDto = new ClusterComponentsDto();
        BeanUtils.copyProperties(beanClusterComponents, clusterComponentsDto);
        return clusterComponentsDto;
    }

    @Override
    public void delete(String clusterId) {
        QueryWrapper<BeanClusterComponents> wrapper =
            new QueryWrapper<BeanClusterComponents>().eq("cluster_id", clusterId);
        beanClusterComponentsMapper.delete(wrapper);
    }

    @Override
    public boolean checkInstalled(String clusterId, String componentName) {
        try {
            List<ClusterComponentsDto> componentsDtos = list(clusterId);
            componentsDtos = componentsDtos.stream()
                .filter(item -> ComponentsEnum.MIDDLEWARE_CONTROLLER.getName().equals(item.getComponent()))
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(componentsDtos)) {
                return false;
            }
            ClusterComponentsDto clusterComponentsDto = componentsDtos.get(0);
            return clusterComponentsDto.getStatus() != 0 && clusterComponentsDto.getStatus() != 5;
        } catch (Exception e) {
            log.error("查询集群组件失败了");
        }
        return false;
    }

    @Override
    public Boolean logCollect(String clusterId) {
        List<HelmListInfo> list = helmChartService.listHelm("logging", null, clusterService.findById(clusterId));
        return list.stream().anyMatch(helmListInfo -> "log".equals(helmListInfo.getName()));
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
    private void record(String clusterId, ClusterComponentsDto componentsDto, Integer status) {
        QueryWrapper<BeanClusterComponents> wrapper = new QueryWrapper<BeanClusterComponents>()
            .eq("cluster_id", clusterId).eq("component", componentsDto.getComponent());
        BeanClusterComponents cm = new BeanClusterComponents();
        cm.setClusterId(clusterId);
        cm.setComponent(componentsDto.getComponent());
        if (status != null){
            cm.setStatus(status);
        }
        BeanUtils.copyProperties(componentsDto, cm, "component", "status");
        cm.setCreateTime(new Date());
        beanClusterComponentsMapper.update(cm, wrapper);
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

    /**
     * 特殊处理grafana，写入protocol
     */
//    public void grafana(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
//        if (StringUtils.isNotEmpty(clusterComponentsDto.getProtocol())
//            && clusterComponentsDto.getComponent().equals(ComponentsEnum.GRAFANA.getName())) {
//            if (cluster.getMonitor() == null) {
//                cluster.setMonitor(new MiddlewareClusterMonitor());
//            }
//            if (cluster.getMonitor().getGrafana() == null) {
//                cluster.getMonitor().setGrafana(new MiddlewareClusterMonitorInfo());
//            }
//            cluster.getMonitor().getGrafana().setProtocol(clusterComponentsDto.getProtocol());
//        }
//    }

    public void installSuccessCheck(MiddlewareClusterDTO cluster, String componentName) {
        QueryWrapper<BeanClusterComponents> wrapper =
            new QueryWrapper<BeanClusterComponents>().eq("cluster_id", cluster.getId()).eq("component", componentName);
        BeanClusterComponents clusterComponents = beanClusterComponentsMapper.selectOne(wrapper);
        if (clusterComponents.getStatus() == 2) {
            clusterComponents.setStatus(6);
            beanClusterComponentsMapper.updateById(clusterComponents);
        }
    }

    /**
     * 创建middleware-operator分区
     */
    public void checkNamespace(ClusterComponentsDto clusterComponentsDto) {
        String name = clusterComponentsDto.getComponent();
        if (ComponentsEnum.LOCAL_PATH.getName().equals(name)
            || ComponentsEnum.MIDDLEWARE_CONTROLLER.getName().equals(name)
            || ComponentsEnum.LVM.getName().equals(name)) {
            namespaceService.createMiddlewareOperator(clusterComponentsDto.getClusterId());
        }
    }

    public void updateLogPilot(ClusterComponentsDto clusterComponentsDto){
        boolean exist = logCollect(clusterComponentsDto.getClusterId());
        if (exist == clusterComponentsDto.getLogCollect()){
            return;
        }
        MiddlewareClusterDTO cluster = clusterService.findById(clusterComponentsDto.getClusterId());
        if (clusterComponentsDto.getLogCollect()){
            loggingService.logPilot(cluster, clusterComponentsDto);
        }else {
            helmChartService.uninstall(cluster, MIDDLEWARE_OPERATOR, "log");
        }
    }

}
