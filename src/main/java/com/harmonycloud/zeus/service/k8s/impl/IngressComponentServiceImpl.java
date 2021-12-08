package com.harmonycloud.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterIngress;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.IngressComponentService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    @Value("${k8s.component.components:/usr/local/zeus-pv/components}")
    protected String componentsPath;

    @Override
    public void install(IngressComponentDto ingressComponentDto) {
        MiddlewareClusterDTO cluster = clusterService.findById(ingressComponentDto.getClusterId());
        if (!CollectionUtils.isEmpty(cluster.getIngressList())) {
            if (cluster.getIngressList().stream()
                .anyMatch(ingress -> ingress.getIngressClassName().equals(ingressComponentDto.getIngressClassName()))) {
                throw new BusinessException(ErrorMessage.EXIST);
            }
        }
        String repository = cluster.getRegistry().getRegistryAddress() + "/" + cluster.getRegistry().getChartRepo();
        //setValues
        String setValues = "image.ingressRepository=" + repository +
                ",image.backendRepository=" + repository +
                ",image.keepalivedRepository=" + repository +
                ",httpPort=" + ingressComponentDto.getHttpPort() +
                ",httpsPort=" + ingressComponentDto.getHttpsPort() +
                ",healthzPort=" + ingressComponentDto.getHealthzPort() +
                ",defaultServerPort=" + ingressComponentDto.getDefaultServerPort() +
                ",ingressClass=" + ingressComponentDto.getIngressClassName() +
                ",fullnameOverride=" + ingressComponentDto.getIngressClassName() +
                ",install=true";
        //install
        helmChartService.upgradeInstall(ingressComponentDto.getIngressClassName(), "middleware-operator", setValues,
                componentsPath + File.separator + "ingress-nginx/charts/ingress-nginx", cluster);
        //update cluster
        MiddlewareClusterIngress ingress = new MiddlewareClusterIngress().setAddress(cluster.getHost())
                .setIngressClassName(ingressComponentDto.getIngressClassName());
        MiddlewareClusterIngress.IngressConfig config = new MiddlewareClusterIngress.IngressConfig();
        config.setEnabled(true).setNamespace("middleware-operator")
                .setConfigMapName(ingressComponentDto.getIngressClassName() + "-system-expose-nginx-config-tcp");
        ingress.setTcp(config);
        if (CollectionUtils.isEmpty(cluster.getIngressList())){
            cluster.setIngressList(new ArrayList<>());
        }
        cluster.getIngressList().add(ingress);
        clusterService.update(cluster);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {
        MiddlewareClusterDTO existCluster = clusterService.findById(cluster.getId());
        if (CollectionUtils.isEmpty(existCluster.getIngressList())){
            existCluster.setIngressList(new ArrayList<>());
        }
        if (existCluster.getIngressList().stream()
                .anyMatch(ingress -> ingress.getIngressClassName().equals(cluster.getIngressList().get(0).getIngressClassName()))) {
            throw new BusinessException(ErrorMessage.EXIST);
        }
        cluster.getIngressList().get(0).getTcp().setEnabled(true);
        existCluster.getIngressList().addAll(cluster.getIngressList());
        clusterService.update(existCluster);
    }

    @Override
    public List<IngressComponentDto> list(String clusterId) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        return cluster.getIngressList().stream()
            .map(ingress -> new IngressComponentDto().setAddress(ingress.getAddress())
                .setIngressClassName(ingress.getIngressClassName()).setNamespace(ingress.getTcp().getNamespace())
                .setConfigMapName(ingress.getTcp().getConfigMapName())
                .setClusterId(clusterId))
            .collect(Collectors.toList());
    }

    @Override
    public void delete(String clusterId, String ingressClassName) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        JSONObject values = helmChartService.getInstalledValues(ingressClassName, "middleware-operator", cluster);
        if (values != null && values.containsKey("install") && "true".equals(values.getString("install"))) {
            helmChartService.uninstall(cluster, "middleware-operator", ingressClassName);
        }
        MiddlewareClusterIngress exist = cluster.getIngressList().stream().filter(ingress -> ingress.getIngressClassName().equals(ingressClassName)).collect(Collectors.toList()).get(0);
        cluster.getIngressList().remove(exist);
        clusterService.update(cluster);
    }
}
