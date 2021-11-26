package com.harmonycloud.zeus.service.components.impl;

import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterIngress;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.IngressService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2021/10/29 4:14 下午
 */
@Service
@Operator(paramTypes4One = String.class)
public class IngressComponentsServiceImpl extends AbstractBaseOperator implements IngressService {

    @Override
    public boolean support(String name) {
        return ComponentsEnum.INGRESS.getName().equals(name);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {
        MiddlewareClusterDTO existCluster = clusterService.findById(cluster.getId());
        existCluster.getIngressList().addAll(cluster.getIngressList());
        clusterService.update(existCluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        if (status != 1) {
            helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.INGRESS.getName());
        }
        // todo
        MiddlewareClusterIngress exist = cluster.getIngressList().stream()
            .filter(ingress -> ingress.getIngressClassName().equals(ComponentsEnum.INGRESS.getName()))
            .collect(Collectors.toList()).get(0);
        cluster.getIngressList().remove(exist);
        clusterService.update(cluster);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, String type) {
        return "image.ingressRepository=" + repository +
                ",image.backendRepository=" + repository +
                ",image.keepalivedRepository=" + repository;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.upgradeInstall(ComponentsEnum.INGRESS.getName(), "middleware-operator", setValues,
                componentsPath + File.separator + "ingress-nginx/charts/ingress-nginx", cluster);
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {
        MiddlewareClusterIngress ingress = new MiddlewareClusterIngress().setAddress(cluster.getHost())
                .setIngressClassName("ingress-ingress-nginx-controller");
        MiddlewareClusterIngress.IngressConfig config = new MiddlewareClusterIngress.IngressConfig();
        config.setEnabled(true).setNamespace("middleware-operator")
                .setConfigMapName("ingress-ingress-nginx-system-expose-nginx-config-tcp");
        ingress.setTcp(config);
        if (CollectionUtils.isEmpty(cluster.getIngressList())){
            cluster.setIngressList(new ArrayList<>());
        }
        cluster.getIngressList().add(ingress);
        clusterService.update(cluster);
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", "ingress-nginx-controller");
    }
}
