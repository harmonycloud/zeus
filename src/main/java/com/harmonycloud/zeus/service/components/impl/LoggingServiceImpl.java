package com.harmonycloud.zeus.service.components.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.bean.BeanClusterComponents;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.LoggingService;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.EsService;
import com.harmonycloud.zeus.service.middleware.MiddlewareManagerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import static com.harmonycloud.caas.common.constants.CommonConstant.SIMPLE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2021/10/29 4:16 下午
 */
@Service
@Slf4j
@Operator(paramTypes4One = String.class)
public class LoggingServiceImpl extends AbstractBaseOperator implements LoggingService {

    private static final String ES_NAME = "kubernetes-logging";
    @Autowired
    private EsService esService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ClusterMiddlewareInfoService clusterMiddlewareInfoService;
    @Autowired
    private MiddlewareManagerService middlewareManagerService;

    @Override
    public boolean support(String name) {
        return ComponentsEnum.LOGGING.getName().equals(name);
    }

    @Override
    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        //创建分区
        namespaceService.save(cluster.getId(), "logging", null, null);
        // 校验是否已发布elasticsearch
        List<BeanClusterMiddlewareInfo> beanClusterMiddlewareInfoList =
            clusterMiddlewareInfoService.list(cluster.getId());
        beanClusterMiddlewareInfoList =
            beanClusterMiddlewareInfoList.stream().filter(beanClusterMiddlewareInfo -> beanClusterMiddlewareInfo
                .getChartName().equals(MiddlewareTypeEnum.ELASTIC_SEARCH.getType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(beanClusterMiddlewareInfoList) || beanClusterMiddlewareInfoList.get(0).getStatus() == 2) {
            middlewareManagerService.install(cluster.getId(), MiddlewareTypeEnum.ELASTIC_SEARCH.getType(), null,
                "simple");
        }
        //发布elasticsearch
        super.deploy(cluster, clusterComponentsDto);
        //为es创建nodePort
        try {
            createNodePort(cluster);
        } catch (Exception e){
            log.error("es组建创建nodePort失败", e);
        }
        //初始化es索引模板,安装log-pilot
        Executors.newSingleThreadExecutor().execute(() -> {
            tryCreateEsTemplate(cluster, clusterComponentsDto);
        });
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String setValues = "image.repository=" + repository +
                ",aliasName=kubernetes-logging" +
                ",nameOverride=kubernetes-logging" +
                ",elasticsearch-operator.enabled=false" +
                ",elasticPassword=Hc@Cloud01" +
                ",storage.masterClass=local-path" +
                ",storage.masterSize=5Gi" +
                ",logging.collection.filelog.enable=false" +
                ",logging.collection.stdout.enable=false" +
                ",resources.master.limits.cpu=1" +
                ",resources.master.limits.memory=4Gi" +
                ",esJavaOpts.xmx=1024m" +
                ",esJavaOpts.xms=1024m";
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            setValues = setValues + ",cluster.masterReplacesCount=1,resources.master.requests.cpu=700m,resources.master.requests.memory=1Gi";
        } else {
            setValues = setValues + ",cluster.masterReplacesCount=3,resources.master.requests.cpu=1,resources.master.requests.memory=4Gi";
        }
        return setValues;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.installComponents(ES_NAME, "logging", setValues,
                componentsPath + File.separator + "elasticsearch", cluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        if (status != 1){
            helmChartService.uninstall(cluster, "logging", ES_NAME);
            helmChartService.uninstall(cluster, "logging", "log");
        }
        if (cluster.getLogging().getElasticSearch() != null){
            cluster.getLogging().setElasticSearch(null);
        }
        clusterService.update(cluster);
    }

    @Override
    public void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster){
        if (StringUtils.isEmpty(clusterComponentsDto.getProtocol())){
            clusterComponentsDto.setProtocol("http");
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getHost())){
            clusterComponentsDto.setHost(cluster.getHost());
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getPort())){
            clusterComponentsDto.setPort("30092");
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getUsername())){
            clusterComponentsDto.setUsername("elastic");
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getPassword())){
            clusterComponentsDto.setPassword("Hc@Cloud01");
        }
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "logging", ES_NAME);
    }

    public void createNodePort(MiddlewareClusterDTO cluster){
        IngressDTO ingressDTO = new IngressDTO();
        ingressDTO.setExposeType("NodePort");
        ingressDTO.setMiddlewareType("elasticsearch");
        ingressDTO.setProtocol("TCP");
        List<ServiceDTO> serviceList = new ArrayList<>();
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setExposePort("30092");
        serviceDTO.setServiceName("kubernetes-logging-master");
        serviceDTO.setServicePort("9200");
        serviceDTO.setTargetPort("9200");
        serviceList.add(serviceDTO);
        ingressDTO.setServiceList(serviceList);
        ingressService.create(cluster.getId(), "logging", "kubernetes-logging", ingressDTO);
    }

    @Override
    public void logPilot(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String repository = getRepository(cluster);
        String setValues = "image.logpilotRepository=" + repository + "/log-pilot" +
                ",image.logstashRepository=" + repository + "/logstash";
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            setValues = setValues + ",logstashReplacesCount=1";
        } else {
            setValues = setValues + ",logstashReplacesCount=2";
        }
        helmChartService.installComponents("log", "logging", setValues,
                componentsPath + File.separator + "logging", cluster);
    }

    public void tryCreateEsTemplate(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        QueryWrapper<BeanClusterComponents> wrapper =
            new QueryWrapper<BeanClusterComponents>().eq("cluster_id", cluster.getId()).eq("component", "logging");
        for (int i = 0; i < 60 * 60; i++) {
            try {
                BeanClusterComponents logging = beanClusterComponentsMapper.selectOne(wrapper);
                if (logging.getStatus() == 3) {
                    boolean res = createEsTemplate(cluster);
                    if (res) {
                        // 发布logPilot
                        log.info("es发布成功,开始安装logPilot");
                        logPilot(cluster, clusterComponentsDto);
                        return;
                    }
                }
            } catch (Exception e) {
                log.error("查询集群组件失败", e);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("尝试初始化es索引模板失败", e);
            }
        }
    }

    public boolean createEsTemplate(MiddlewareClusterDTO cluster) {
        return esService.initEsIndexTemplate(cluster.getId());
    }

    @Override
    public void setStatus(ClusterComponentsDto clusterComponentsDto) {
        if (StringUtils.isAnyEmpty(clusterComponentsDto.getProtocol(), clusterComponentsDto.getHost(),
            clusterComponentsDto.getPort())) {
            clusterComponentsDto.setStatus(7);
        }
    }
}
