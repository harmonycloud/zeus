package com.harmonycloud.zeus.service.k8s.impl;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.integration.cluster.PodWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.service.k8s.*;
import io.fabric8.kubernetes.api.model.Pod;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ActiveAreaEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.ActiveAreaDto;
import com.harmonycloud.caas.common.model.ActivePoolDto;
import com.harmonycloud.caas.common.model.ClusterNodeResourceDto;
import com.harmonycloud.caas.common.model.Node;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;
import com.harmonycloud.zeus.bean.BeanActiveArea;
import com.harmonycloud.zeus.dao.BeanActiveAreaMapper;
import com.harmonycloud.zeus.integration.cluster.NodeWrapper;

import io.fabric8.kubernetes.api.model.Taint;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2022/5/10 2:45 下午
 */
@Service
@Slf4j
public class ActiveAreaServiceImpl implements ActiveAreaService {
    
    private static final String taintType = "harm.cn/type";
    private static final String taintValue = "active-active";

    @Value("${active-active.label.key:topology.kubernetes.io/zone}")
    private String zoneKey;

    @Autowired
    private NodeWrapper nodeWrapper;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private BeanActiveAreaMapper beanActiveAreaMapper;
    @Autowired
    private PodWrapper podWrapper;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private ClusterComponentService clusterComponentService;

    @Override
    public void dividePool(String clusterId, ActivePoolDto activePoolDto) {
        List<io.fabric8.kubernetes.api.model.Node> nodes = nodeWrapper.list(clusterId);
        nodes =
            nodes.stream()
                .filter(node -> activePoolDto.getNodeList().stream()
                    .anyMatch(node1 -> node1.getName().equals(node.getMetadata().getName())))
                .collect(Collectors.toList());
        for (io.fabric8.kubernetes.api.model.Node node : nodes) {
            // 设置污点
            List<Taint> taintList = node.getSpec().getTaints();
            if (CollectionUtils.isEmpty(taintList)) {
                taintList = new ArrayList<>();
            }
            Taint taint = new Taint();
            taint.setKey("harm.cn/type");
            taint.setValue("active-active");
            taint.setEffect("NoSchedule");
            taintList.add(taint);
            node.getSpec().setTaints(taintList);

            // 添加标签
            Map<String, String> labels = node.getMetadata().getLabels();
            if (CollectionUtils.isEmpty(labels)) {
                labels = new HashMap<>();
            }
            labels.put("type", "active-active");
            node.getMetadata().setLabels(labels);
            // 更新节点
            nodeWrapper.update(node, clusterId);
        }
    }

    @Override
    public ActivePoolDto getPoolNode(String clusterId) {
        List<com.harmonycloud.caas.common.model.Node> nodeList = nodeService.list(clusterId);
        nodeList = nodeList.stream()
            .filter(
                node -> node.getLabels().containsKey("type") && "active-active".equals(node.getLabels().get("type")))
            .collect(Collectors.toList());
        return new ActivePoolDto().setClusterId(clusterId).setName("active-pool").setNodeList(nodeList);
    }

    @Override
    public void divideActiveArea(String clusterId, ActiveAreaDto activeAreaDto) {
        // 获取将被添加的下节点
        List<io.fabric8.kubernetes.api.model.Node> nodes = nodeWrapper.list(clusterId);
        List<io.fabric8.kubernetes.api.model.Node> filteredNodeList =
            nodes.stream()
                .filter(node -> activeAreaDto.getNodeList().stream()
                    .anyMatch(node1 -> node1.getName().equals(node.getMetadata().getName())))
                .collect(Collectors.toList());
        // 设置标签污点
        for (io.fabric8.kubernetes.api.model.Node node : filteredNodeList) {
            // 设置污点
            List<Taint> taintList = node.getSpec().getTaints();
            if (CollectionUtils.isEmpty(taintList)) {
                taintList = new ArrayList<>();
            }
            if (taintList.stream().noneMatch(
                taint -> taintType.equals(taint.getKey()) && taintValue.equals(taint.getValue()))) {
                Taint taint = new Taint();
                taint.setKey("harm.cn/type");
                taint.setValue("active-active");
                taint.setEffect("NoSchedule");
                taintList.add(taint);
                node.getSpec().setTaints(taintList);
            }

            // 添加标签
            Map<String, String> labels = node.getMetadata().getLabels();
            if (CollectionUtils.isEmpty(labels)) {
                labels = new HashMap<>();
            }
            labels.put(zoneKey, activeAreaDto.getName());
            node.getMetadata().setLabels(labels);
            // 更新节点
            nodeWrapper.update(node, clusterId);
        }
    }

    @Override
    public void removeActiveAreaNode(String clusterId, String areaName, String nodeName) {
        io.fabric8.kubernetes.api.model.Node node = nodeWrapper.get(clusterId, nodeName);
        if (node == null) {
            throw new BusinessException(ErrorMessage.NODE_NOT_FOUND);
        }
        // 验证节点是否存在中间件服务
        Map<String, String> fields = new HashMap<>();
        fields.put("spec.nodeName", nodeName);
        List<Pod> podList = podWrapper.listByFields(clusterId, null, fields);

        if (clusterComponentService.checkInstalled(clusterId, ComponentsEnum.MIDDLEWARE_CONTROLLER.getName())) {
            List<MiddlewareCR> middlewareCRList = middlewareCRService.listCR(clusterId, null, null);
            podList =
                    podList.stream()
                            .filter(pod -> middlewareCRList.stream().anyMatch(middlewareCR -> pod.getMetadata().getName()
                                    .substring(0, pod.getMetadata().getName().length() - 2).equals(middlewareCR.getSpec().getName())))
                            .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(podList)) {
                throw new BusinessException(ErrorMessage.NODE_CONTAINS_MIDDLEWARE_PODS);
            }
        }

        // 移除labels
        Map<String, String> labels = node.getMetadata().getLabels();
        if (!CollectionUtils.isEmpty(labels) && labels.containsKey(zoneKey) && areaName.equals(labels.get(zoneKey))) {
            labels.remove(zoneKey, areaName);
            node.getMetadata().setLabels(labels);
        }
        // 移除污点
        if (node.getSpec().getTaints().stream()
            .anyMatch(taint -> taintType.equals(taint.getKey()) && taintValue.equals(taint.getValue()))) {
            node.getSpec().getTaints()
                .removeIf(taint -> taintType.equals(taint.getKey()) && taintValue.equals(taint.getValue()));
        }
        nodeWrapper.update(node, clusterId);
    }

    @Override
    public List<ActiveAreaDto> list(String clusterId) {
        QueryWrapper<BeanActiveArea> wrapper = new QueryWrapper<BeanActiveArea>().eq("cluster_id", clusterId);
        List<BeanActiveArea> beanActiveAreaList;
        synchronized (this) {
            beanActiveAreaList = beanActiveAreaMapper.selectList(wrapper);
            if (CollectionUtils.isEmpty(beanActiveAreaList)) {
                beanActiveAreaList = initBeanActiveArea(clusterId);
            }
        }
        Map<String, BeanActiveArea> map =
            beanActiveAreaList.stream().collect(Collectors.toMap(BeanActiveArea::getAreaName, a -> a));
        List<ActiveAreaDto> activeAreaDtoList = new ArrayList<>();
        for (ActiveAreaEnum activeAreaEnum : ActiveAreaEnum.values()) {
            ActiveAreaDto activeAreaDto = new ActiveAreaDto();
            activeAreaDto.setName(activeAreaEnum.getName());
            // 获取中文别名
            activeAreaDto.setAliasName(map.get(activeAreaEnum.getName()).getAliasName());
            // 更新可用区是否已经初始化
            activeAreaDto.setInit(map.get(activeAreaEnum.getName()).getInit());
            if (!activeAreaDto.getInit()) {
                Map<String, String> labels = new HashMap<>();
                labels.put(zoneKey, activeAreaEnum.getName());
                List<Node> nodes = nodeService.list(clusterId, labels);
                if (!CollectionUtils.isEmpty(nodes)) {
                    beanActiveAreaMapper.updateById(map.get(activeAreaEnum.getName()).setInit(true));
                    activeAreaDto.setInit(true);
                }
            }
            activeAreaDtoList.add(activeAreaDto);
        }
        return activeAreaDtoList;
    }

    @Override
    public void update(String clusterId, String areaName, String aliasName) {
        QueryWrapper<BeanActiveArea> wrapper =
            new QueryWrapper<BeanActiveArea>().eq("cluster_id", clusterId).eq("area_name", areaName);
        BeanActiveArea beanActiveArea = new BeanActiveArea();
        beanActiveArea.setClusterId(clusterId);
        beanActiveArea.setAreaName(areaName);
        beanActiveArea.setAliasName(aliasName);
        beanActiveArea.setInit(true);
        BeanActiveArea exist = beanActiveAreaMapper.selectOne(wrapper);
        if (exist == null) {
            beanActiveAreaMapper.insert(beanActiveArea);
        } else {
            beanActiveAreaMapper.update(beanActiveArea, wrapper);
        }
    }

    @Override
    public ActiveAreaDto getAreaResource(String clusterId, String areaName) {
        Map<String, String> labels = new HashMap<>();
        labels.put(zoneKey, areaName);
        // 获取指定可用区节点
        List<Node> nodes = nodeService.list(clusterId, labels);
        // 计算可用节点数量
        int runningNode = 0;
        int errorNode = 0;
        for (Node node : nodes) {
            if ("True".equals(node.getStatus())) {
                runningNode = runningNode + 1;
            } else {
                errorNode = errorNode + 1;
            }
        }
        // 初始化对象
        ActiveAreaDto activeAreaDto = new ActiveAreaDto().setClusterId(clusterId).setName(areaName);
        // 查询节点资源
        ClusterNodeResourceDto clusterNodeResourceDto = nodeService.getSumNodeResource(clusterId, nodes, false);
        // 封装数据
        activeAreaDto.setRunningNodeCount(runningNode);
        activeAreaDto.setErrorNodeCount(errorNode);
        activeAreaDto.setCpuUsed(clusterNodeResourceDto.getCpuUsed());
        activeAreaDto.setCpuTotal(clusterNodeResourceDto.getCpuTotal());
        activeAreaDto.setMemoryUsed(clusterNodeResourceDto.getMemoryUsed());
        activeAreaDto.setMemoryTotal(clusterNodeResourceDto.getMemoryTotal());

        // 计算cpu使用率
        if (activeAreaDto.getCpuUsed() != null && activeAreaDto.getCpuTotal() != null
            && activeAreaDto.getCpuTotal() != 0) {
            activeAreaDto.setCpuRate(ResourceCalculationUtil.roundNumber(
                BigDecimal.valueOf(activeAreaDto.getCpuUsed() / activeAreaDto.getCpuTotal() * 100), 2,
                RoundingMode.CEILING));
        }
        // 计算memory使用率
        if (activeAreaDto.getMemoryUsed() != null && activeAreaDto.getMemoryTotal() != null
            && activeAreaDto.getMemoryTotal() != 0) {
            activeAreaDto.setMemoryRate(ResourceCalculationUtil.roundNumber(
                BigDecimal.valueOf(activeAreaDto.getMemoryUsed() / activeAreaDto.getMemoryTotal() * 100), 2,
                RoundingMode.CEILING));
        }
        // 查询中文别名
        QueryWrapper<BeanActiveArea> wrapper =
            new QueryWrapper<BeanActiveArea>().eq("cluster_id", clusterId).eq("area_name", areaName);
        BeanActiveArea beanActiveArea = beanActiveAreaMapper.selectOne(wrapper);
        if (beanActiveArea == null) {
            activeAreaDto.setAliasName(ActiveAreaEnum.getByName(areaName));
        } else {
            activeAreaDto.setAliasName(beanActiveArea.getAliasName());
            activeAreaDto.setInit(beanActiveArea.getInit() != null && beanActiveArea.getInit());
        }
        return activeAreaDto;
    }

    @Override
    public List<ClusterNodeResourceDto> getAreaNode(String clusterId, String areaName) {
        Map<String, String> labels = new HashMap<>();
        labels.put(zoneKey, areaName);
        // 获取指定可用区节点
        List<Node> nodes = nodeService.list(clusterId, labels);
        return nodeService.getNodeResource(clusterId, nodes, false);
    }

    @Override
    public List<BeanActiveArea> initBeanActiveArea(String clusterId) {
        List<BeanActiveArea> beanActiveAreaList = new ArrayList<>();
        for (ActiveAreaEnum activeAreaEnum : ActiveAreaEnum.values()) {
            BeanActiveArea beanActiveArea = new BeanActiveArea();
            beanActiveArea.setClusterId(clusterId).setAreaName(activeAreaEnum.getName())
                .setAliasName(activeAreaEnum.getAliasName()).setInit(false);
            beanActiveAreaList.add(beanActiveArea);
            beanActiveAreaMapper.insert(beanActiveArea);
        }
        return beanActiveAreaList;
    }



    @Override
    public BeanActiveArea get(String clusterId, String areaName) {
        QueryWrapper<BeanActiveArea> queryWrapper  = new QueryWrapper<>();
        queryWrapper.eq("cluster_id", clusterId);
        queryWrapper.eq("area_name", areaName);
        return beanActiveAreaMapper.selectOne(queryWrapper);
    }

    @Override
    public void delete(String clusterId) {
        QueryWrapper<BeanActiveArea> queryWrapper  = new QueryWrapper<>();
        queryWrapper.eq("cluster_id", clusterId);
        beanActiveAreaMapper.delete(queryWrapper);
    }

}
