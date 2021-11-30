package com.harmonycloud.zeus.service.k8s.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterIngress;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.bean.BeanIngressComponents;
import com.harmonycloud.zeus.dao.BeanIngressComponentsMapper;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.IngressComponentService;
import com.harmonycloud.zeus.service.k8s.PodService;
import com.harmonycloud.zeus.service.registry.HelmChartService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/11/22 5:54 下午
 */
@Service
@Slf4j
public class IngressComponentServiceImpl implements IngressComponentService {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private BeanIngressComponentsMapper beanIngressComponentsMapper;
    @Autowired
    private PodService podService;
    @Value("${k8s.component.components:/usr/local/zeus-pv/components}")
    private String componentsPath;

    @Override
    public void install(IngressComponentDto ingressComponentDto) {
        MiddlewareClusterDTO cluster = clusterService.findById(ingressComponentDto.getClusterId());
        if (!CollectionUtils.isEmpty(cluster.getIngressList())) {
            if (cluster.getIngressList().stream()
                .anyMatch(ingress -> ingress.getIngressClassName().equals(ingressComponentDto.getIngressClassName()))) {
                throw new BusinessException(ErrorMessage.INGRESS_CLASS_EXISTED);
            }
        }
        String repository = cluster.getRegistry().getRegistryAddress() + "/" + cluster.getRegistry().getChartRepo();
        // setValues
        String setValues = "image.ingressRepository=" + repository + ",image.backendRepository=" + repository
            + ",image.keepalivedRepository=" + repository + ",httpPort=" + ingressComponentDto.getHttpPort()
            + ",httpsPort=" + ingressComponentDto.getHttpsPort() + ",healthzPort="
            + ingressComponentDto.getHealthzPort() + ",defaultServerPort=" + ingressComponentDto.getDefaultServerPort()
            + ",ingressClass=" + ingressComponentDto.getIngressClassName() + ",fullnameOverride="
            + ingressComponentDto.getIngressClassName() + ",install=true";
        // install
        helmChartService.upgradeInstall(ingressComponentDto.getIngressClassName(), "middleware-operator", setValues,
            componentsPath + File.separator + "ingress-nginx/charts/ingress-nginx", cluster);
        // update cluster
        MiddlewareClusterIngress ingress = new MiddlewareClusterIngress().setAddress(cluster.getHost())
            .setIngressClassName(ingressComponentDto.getIngressClassName());
        MiddlewareClusterIngress.IngressConfig config = new MiddlewareClusterIngress.IngressConfig();
        config.setEnabled(true).setNamespace("middleware-operator")
            .setConfigMapName(ingressComponentDto.getIngressClassName() + "-system-expose-nginx-config-tcp");
        ingress.setTcp(config);
        if (CollectionUtils.isEmpty(cluster.getIngressList())) {
            cluster.setIngressList(new ArrayList<>());
        }
        cluster.getIngressList().add(ingress);
        clusterService.update(cluster);
        // save to mysql
        insert(cluster.getId(), ingressComponentDto.getIngressClassName(), 2);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {
        MiddlewareClusterDTO existCluster = clusterService.findById(cluster.getId());
        if (CollectionUtils.isEmpty(existCluster.getIngressList())) {
            existCluster.setIngressList(new ArrayList<>());
        }
        existCluster.getIngressList().addAll(cluster.getIngressList());
        clusterService.update(existCluster);
        // save to mysql
        insert(cluster.getId(), cluster.getIngressList().get(0).getIngressClassName(), 1);
    }

    @Override
    public List<IngressComponentDto> list(String clusterId) {
        // 获取数据库状态记录
        QueryWrapper<BeanIngressComponents> wrapper =
            new QueryWrapper<BeanIngressComponents>().eq("cluster_id", clusterId);
        List<BeanIngressComponents> ingressComponentsList = beanIngressComponentsMapper.selectList(wrapper);
        updateStatus(clusterId, ingressComponentsList);
        Map<String, Integer> ingressStatusMap = ingressComponentsList.stream()
            .collect(Collectors.toMap(BeanIngressComponents::getName, BeanIngressComponents::getStatus));

        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        //数据同步
        if (cluster.getIngressList().size() > ingressComponentsList.size()){
            synchronization(cluster, ingressComponentsList);
        }
        return cluster.getIngressList().stream()
            .map(ingress -> new IngressComponentDto().setAddress(ingress.getAddress())
                .setIngressClassName(ingress.getIngressClassName()).setNamespace(ingress.getTcp().getNamespace())
                .setConfigMapName(ingress.getTcp().getConfigMapName()).setClusterId(clusterId)
                .setStatus(ingressStatusMap.get(ingress.getIngressClassName())))
            .collect(Collectors.toList());
    }

    @Override
    public void delete(String clusterId, String ingressClassName) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        QueryWrapper<BeanIngressComponents> wrapper =
            new QueryWrapper<BeanIngressComponents>().eq("cluster_id", clusterId).eq("name", ingressClassName);
        BeanIngressComponents existIngress = beanIngressComponentsMapper.selectOne(wrapper);
        if (existIngress.getStatus() != 1) {
            helmChartService.uninstall(cluster, "middleware-operator", ingressClassName);
        }
        // update cluster
        MiddlewareClusterIngress exist =
            cluster.getIngressList().stream().filter(ingress -> ingress.getIngressClassName().equals(ingressClassName))
                .collect(Collectors.toList()).get(0);
        cluster.getIngressList().remove(exist);
        clusterService.update(cluster);
        // remove from mysql
        beanIngressComponentsMapper.deleteById(existIngress.getId());
    }

    public void insert(String clusterId, String name, Integer status) {
        BeanIngressComponents beanIngressComponents = new BeanIngressComponents();
        beanIngressComponents.setClusterId(clusterId);
        beanIngressComponents.setName(name);
        beanIngressComponents.setStatus(status);
        beanIngressComponentsMapper.insert(beanIngressComponents);
    }

    public void updateStatus(String clusterId, List<BeanIngressComponents> ingressComponentsList) {
        Map<String, String> labels = new HashMap<>(1);
        labels.put("app.kubernetes.io/name", "ingress-nginx");
        List<PodInfo> podInfoList = podService.list(clusterId, "middleware-operator", labels);
        ingressComponentsList.forEach(ingress -> {
            if (ingress.getStatus() == 1) {
                return;
            }
            List<PodInfo> pods = podInfoList.stream().filter(podInfo -> {
                String name = podInfo.getPodName();
                return name.substring(0, name.lastIndexOf("-")).equals(ingress.getName());
            }).collect(Collectors.toList());
            // 默认正常
            int status = ingress.getStatus();
            if (CollectionUtils.isEmpty(pods)) {
                // 未安装
                beanIngressComponentsMapper.deleteById(ingress.getId());
                return;
            } else if (pods.stream()
                .allMatch(pod -> "Running".equals(pod.getStatus()) || "Completed".equals(pod.getStatus()))) {
                status = 3;
            } else if (ingress.getStatus() != 5 && ingress.getStatus() != 2) {
                // 非卸载或安装中 则为异常
                status = 4;
            }
            ingress.setStatus(status);
            beanIngressComponentsMapper.updateById(ingress);
        });
    }

    public void synchronization(MiddlewareClusterDTO cluster, List<BeanIngressComponents> ingressComponentsList){
        //过滤获取未在数据库记录的ingress
        List<
            MiddlewareClusterIngress> ingressList =
                cluster.getIngressList().stream()
                    .filter(ingress -> ingressComponentsList.stream()
                        .noneMatch(ic -> ic.getName().equals(ingress.getIngressClassName())))
                    .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(ingressList)){
            ingressList.forEach(ingress -> {
                BeanIngressComponents bean = new BeanIngressComponents();
                bean.setClusterId(cluster.getId());
                bean.setName(ingress.getIngressClassName());
                bean.setStatus(1);
                beanIngressComponentsMapper.insert(bean);
                ingressComponentsList.add(bean);
            });
        }
    }

}
