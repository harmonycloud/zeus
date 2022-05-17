package com.harmonycloud.zeus.service.k8s.impl;

import static com.harmonycloud.caas.common.constants.CommonConstant.ZONE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.harmonycloud.zeus.service.k8s.ActiveAreaService;
import com.harmonycloud.zeus.service.k8s.NodeService;

import io.fabric8.kubernetes.api.model.Taint;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2022/5/10 2:45 下午
 */
@Service
@Slf4j
public class ActiveAreaServiceImpl implements ActiveAreaService {

    @Autowired
    private NodeWrapper nodeWrapper;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private BeanActiveAreaMapper beanActiveAreaMapper;

    @Override
    public void dividePool(String clusterId, ActivePoolDto activePoolDto){
        List<io.fabric8.kubernetes.api.model.Node> nodes = nodeWrapper.list(clusterId);
        nodes =
            nodes.stream()
                .filter(node -> activePoolDto.getNodeList().stream()
                    .anyMatch(node1 -> node1.getName().equals(node.getMetadata().getName())))
                .collect(Collectors.toList());
        for (io.fabric8.kubernetes.api.model.Node node : nodes){
            // 设置污点
            List<Taint> taintList = node.getSpec().getTaints();
            if (CollectionUtils.isEmpty(taintList)){
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
            if (CollectionUtils.isEmpty(labels)){
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
            labels.put(ZONE, activeAreaDto.getName());
            node.getMetadata().setLabels(labels);
            // 更新节点
            nodeWrapper.update(node, clusterId);
        }
    }

    @Override
    public void removeActiveAreaNode(String clusterId, String areaName, String nodeName) {
        io.fabric8.kubernetes.api.model.Node node = nodeWrapper.get(clusterId, nodeName);
        if (node == null){
            throw new BusinessException(ErrorMessage.NODE_NOT_FOUND);
        }
        Map<String, String> labels = node.getMetadata().getLabels();
        if (!CollectionUtils.isEmpty(labels) && labels.containsKey(ZONE) && areaName.equals(labels.get(ZONE))){
            labels.remove(ZONE, areaName);
            node.getMetadata().setLabels(labels);
        }
        nodeWrapper.update(node, clusterId);
    }

    @Override
    public List<ActiveAreaDto> list(String clusterId) {
        QueryWrapper<BeanActiveArea> wrapper = new QueryWrapper<BeanActiveArea>().eq("cluster_id", clusterId);
        List<BeanActiveArea> beanActiveAreaList = beanActiveAreaMapper.selectList(wrapper);
        Map<String, String> map = new HashMap<>();
        if (!CollectionUtils.isEmpty(beanActiveAreaList)) {
            map = beanActiveAreaList.stream()
                .collect(Collectors.toMap(BeanActiveArea::getAreaName, BeanActiveArea::getAliasName));
        }
        List<ActiveAreaDto> activeAreaDtoList = new ArrayList<>();
        for (ActiveAreaEnum activeAreaEnum : ActiveAreaEnum.values()) {
            ActiveAreaDto activeAreaDto = new ActiveAreaDto();
            activeAreaDto.setName(activeAreaEnum.getName());
            if (map.containsKey(activeAreaEnum.getName())) {
                activeAreaDto.setAliasName(map.get(activeAreaEnum.getName()));
            } else {
                activeAreaDto.setAliasName(activeAreaEnum.getAliasName());
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
        labels.put(ZONE, areaName);
        // 获取指定可用区节点
        List<Node> nodes = nodeService.list(clusterId, labels);
        // 计算可用节点数量
        int runningNode = 0;
        int errorNode = 0;
        for (Node node : nodes){
            if ("True".equals(node.getStatus())){
                runningNode = runningNode + 1;
            }else {
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
        }
        return activeAreaDto;
    }

    @Override
    public List<ClusterNodeResourceDto> getAreaNode(String clusterId, String areaName) {
        Map<String, String> labels = new HashMap<>();
        labels.put(ZONE, areaName);
        // 获取指定可用区节点
        List<Node> nodes = nodeService.list(clusterId, labels);
        return nodeService.getNodeResource(clusterId, nodes, false);
    }

    @Override
    public BeanActiveArea get(String clusterId, String areaName) {
        QueryWrapper<BeanActiveArea> queryWrapper  = new QueryWrapper<>();
        queryWrapper.eq("cluster_id", clusterId);
        queryWrapper.eq("area_name", areaName);
        return beanActiveAreaMapper.selectOne(queryWrapper);
    }
}
