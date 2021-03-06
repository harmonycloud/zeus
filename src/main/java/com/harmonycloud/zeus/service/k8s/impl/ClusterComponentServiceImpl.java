package com.harmonycloud.zeus.service.k8s.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.MultipleComponentsInstallDto;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.bean.BeanClusterComponents;
import com.harmonycloud.zeus.dao.BeanClusterComponentsMapper;
import com.harmonycloud.zeus.service.middleware.MiddlewareManagerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harmonycloud.zeus.service.AbstractBaseService;
import com.harmonycloud.zeus.service.components.BaseComponentsService;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.registry.HelmChartService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.CommonConstant.NUM_FIVE;
import static com.harmonycloud.caas.common.constants.CommonConstant.NUM_TWO;

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
    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        //????????????grafana
        grafana(cluster, clusterComponentsDto);
        // ??????service
        BaseComponentsService service =
                getOperator(BaseComponentsService.class, BaseComponentsService.class, clusterComponentsDto.getComponent());
        // ????????????
        service.deploy(cluster, clusterComponentsDto);
        //???????????????
        record(cluster.getId(), clusterComponentsDto.getComponent(), 2);
        // ????????????????????????
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                Thread.sleep(55000);
                installSuccessCheck(cluster, clusterComponentsDto.getComponent());
            } catch (InterruptedException e) {
                log.error("?????????????????????????????????");
            }
        });
    }

    @Override
    public void multipleDeploy(MiddlewareClusterDTO cluster, MultipleComponentsInstallDto multipleComponentsInstallDto)
        throws Exception {
        List<ClusterComponentsDto> componentsDtoList = multipleComponentsInstallDto.getClusterComponentsDtoList();
        // ??????????????????local-path
        if (componentsDtoList.stream().anyMatch(
            clusterComponentsDto -> clusterComponentsDto.getComponent().equals(ComponentsEnum.LOCAL_PATH.getName()))) {
            deploy(cluster, new ClusterComponentsDto().setComponent(ComponentsEnum.LOCAL_PATH.getName()).setType(""));
            componentsDtoList = componentsDtoList.stream().filter(clusterComponentsDto -> !clusterComponentsDto
                .getComponent().equals(ComponentsEnum.LOCAL_PATH.getName())).collect(Collectors.toList());
        }
        // ????????????prometheus
        if (componentsDtoList.stream().anyMatch(
                clusterComponentsDto -> clusterComponentsDto.getComponent().equals(ComponentsEnum.PROMETHEUS.getName()))) {
            deploy(cluster, new ClusterComponentsDto().setComponent(ComponentsEnum.PROMETHEUS.getName()).setType(""));
            componentsDtoList = componentsDtoList.stream().filter(clusterComponentsDto -> !clusterComponentsDto
                    .getComponent().equals(ComponentsEnum.PROMETHEUS.getName())).collect(Collectors.toList());
        }
        // ??????operator
        final CountDownLatch operatorCount =
            new CountDownLatch(multipleComponentsInstallDto.getMiddlewareInfoDTOList().size());
        multipleComponentsInstallDto.getMiddlewareInfoDTOList()
            .forEach(info -> ThreadPoolExecutorFactory.executor.execute(() -> {
                try {
                    middlewareManagerService.install(cluster.getId(), info.getChartName(), info.getChartVersion(),
                        info.getType());
                } catch (Exception e) {
                    log.error("??????{}  operator{} ????????????", cluster.getId(), info.getChartName());
                } finally {
                    operatorCount.countDown();
                }
            }));
        operatorCount.await();
        // ????????????
        final CountDownLatch componentsCount = new CountDownLatch(componentsDtoList.size());
        componentsDtoList.forEach(clusterComponentsDto -> ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                this.deploy(cluster, clusterComponentsDto);
            } catch (Exception e) {
                log.error("??????{}  ??????{}  ????????????", cluster.getId(), clusterComponentsDto.getComponent(), e);
            } finally {
                componentsCount.countDown();
            }
        }));
        componentsCount.await();
    }


    @Override
    public void integrate(MiddlewareClusterDTO cluster, String componentName, Boolean update) {
        BaseComponentsService service = getOperator(BaseComponentsService.class, BaseComponentsService.class, componentName);
        service.integrate(cluster);
        if (!update){
            record(cluster.getId(), componentName, 1);
        }
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, String componentName, Integer status) {
        // ??????????????????
        BaseComponentsService service =
            getOperator(BaseComponentsService.class, BaseComponentsService.class, componentName);
        service.delete(cluster, status);
        // ???????????????
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
        //????????????????????????????????????
        final CountDownLatch count = new CountDownLatch(clusterComponentsList.size());
        clusterComponentsList.forEach(cc -> ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                // ???????????????????????????
                if (cc.getStatus() == 1){
                    return;
                }
                BaseComponentsService service = getOperator(BaseComponentsService.class, BaseComponentsService.class, cc.getComponent());
                service.updateStatus(cluster, cc);
            } finally {
                count.countDown();
            }
        }));
        count.await();
        return clusterComponentsList.stream().map(cm -> {
            ClusterComponentsDto dto = new ClusterComponentsDto();
            BeanUtils.copyProperties(cm, dto);
            if (cm.getStatus() == NUM_TWO){
                dto.setSeconds(DateUtils.getIntervalDays(new Date(), dto.getCreateTime()));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void delete(String clusterId) {
        QueryWrapper<BeanClusterComponents> wrapper = new QueryWrapper<BeanClusterComponents>().eq("cluster_id", clusterId);
        beanClusterComponentsMapper.delete(wrapper);
    }

    /**
     * ??????helm release
     */
    private void rollbackHelmRelease(MiddlewareClusterDTO cluster, String releaseName, String namespace) {
        Middleware middleware = new Middleware().setName(releaseName).setNamespace(namespace);
        helmChartService.uninstall(middleware, cluster);
    }

    /**
     * ???????????????
     */
    private void record(String clusterId, String name, Integer status){
        QueryWrapper<BeanClusterComponents> wrapper = new QueryWrapper<BeanClusterComponents>().eq("cluster_id", clusterId).eq("component", name);
        BeanClusterComponents cm = new BeanClusterComponents();
        cm.setClusterId(clusterId);
        cm.setComponent(name);
        cm.setStatus(status);
        cm.setCreateTime(new Date());
        beanClusterComponentsMapper.update(cm, wrapper);
    }

    /**
     * ??????????????????????????????
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
     * ????????????grafana?????????protocol
     */
    public void grafana(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        if (StringUtils.isNotEmpty(clusterComponentsDto.getProtocol())
            && clusterComponentsDto.getComponent().equals(ComponentsEnum.GRAFANA.getName())) {
            if (cluster.getMonitor() == null) {
                cluster.setMonitor(new MiddlewareClusterMonitor());
            }
            if (cluster.getMonitor().getGrafana() == null) {
                cluster.getMonitor().setGrafana(new MiddlewareClusterMonitorInfo());
            }
            cluster.getMonitor().getGrafana().setProtocol(clusterComponentsDto.getProtocol());
        }
    }

    public void installSuccessCheck(MiddlewareClusterDTO cluster, String componentName){
        QueryWrapper<BeanClusterComponents> wrapper = new QueryWrapper<BeanClusterComponents>().eq("cluster_id", cluster.getId()).eq("component", componentName);
        BeanClusterComponents clusterComponents = beanClusterComponentsMapper.selectOne(wrapper);
        if (clusterComponents.getStatus() == 2){
            clusterComponents.setStatus(6);
            beanClusterComponentsMapper.updateById(clusterComponents);
        }
    }

}
