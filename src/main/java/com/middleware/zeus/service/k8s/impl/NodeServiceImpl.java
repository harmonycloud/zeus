package com.middleware.zeus.service.k8s.impl;

import static com.middleware.caas.common.constants.NameConstant.CONTAINER_RUNTIME_VERSION;
import static com.middleware.caas.common.constants.NameConstant.KUBELET_VERSION;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import com.middleware.caas.common.constants.NameConstant;
import com.middleware.tool.date.DateUtils;
import com.middleware.tool.numeric.ResourceCalculationUtil;
import com.middleware.zeus.integration.cluster.PrometheusWrapper;
import com.middleware.zeus.service.k8s.NodeService;
import com.middleware.zeus.service.prometheus.PrometheusResourceMonitorService;
import com.middleware.caas.common.model.*;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.api.model.NodeCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.integration.cluster.NodeWrapper;

import io.fabric8.kubernetes.api.model.NodeSystemInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@Slf4j
@Service
public class NodeServiceImpl implements NodeService {

    @Autowired
    private NodeWrapper nodeWrapper;
    @Autowired
    protected PrometheusWrapper prometheusWrapper;
    @Autowired
    private PrometheusResourceMonitorService prometheusResourceMonitorService;

    @Override
    public List<Node> list(String clusterId) {
        List<io.fabric8.kubernetes.api.model.Node> nodes = nodeWrapper.list(clusterId);
        return convertToDto(nodes);
    }

    @Override
    public List<Node> list(String clusterId, Map<String, String> labels){
        List<io.fabric8.kubernetes.api.model.Node> nodes = nodeWrapper.list(clusterId, labels);
        return convertToDto(nodes);
    }

    @Override
    public List<Node> listAllocatableNode(String clusterId) {
        return convertToDto(nodeWrapper.listAllocatableNode(clusterId));
    }

    @Override
    public void setClusterVersion(MiddlewareClusterDTO cluster) {
        try {
            List<io.fabric8.kubernetes.api.model.Node> nodeList = nodeWrapper.list(cluster.getId());
            // 设置集群状态信息
            setClusterStatusCode(cluster, nodeList);
            if (CollectionUtils.isEmpty(nodeList)) {
                return;
            }
            io.fabric8.kubernetes.api.model.Node node = nodeList.get(0);
            NodeSystemInfo nodeInfo = node.getStatus().getNodeInfo();
            String[] dockerVersion = nodeInfo.getContainerRuntimeVersion().split("/");
            if (cluster.getAttributes() == null) {
                cluster.setAttributes(new JSONObject());
            }
            cluster.getAttributes().put(CONTAINER_RUNTIME_VERSION, dockerVersion[dockerVersion.length - 1]);
            cluster.getAttributes().put(KUBELET_VERSION, nodeInfo.getKubeletVersion().split("-")[0]);
        } catch (Exception e) {
            log.error("集群：{}，查询节点列表设置k8s版本异常", cluster.getId(), e);
        }
    }

    @Override
    public List<String> listTaints(String clusterId) {
        List<Node> list = list(clusterId);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        Set<String> taintsSet = new HashSet<>();
        list.forEach(node -> {
            if (!CollectionUtils.isEmpty(node.getTaints())) {
                node.getTaints().forEach(taint -> {
                    StringBuffer sbf = new StringBuffer();
                    sbf.append(taint.getKey());
                    if (taint.getValue() != null) {
                        sbf.append("=");
                        sbf.append(taint.getValue());
                    }
                    sbf.append(":").append(taint.getEffect());
                    log.info("node {} taints {}", node.getIp(), sbf);
                    taintsSet.add(sbf.toString());
                });
            }
        });
        return new ArrayList<>(taintsSet);
    }

    @Override
    public List<Node> convertToDto(List<io.fabric8.kubernetes.api.model.Node> nodes){
        return nodes.stream().map(no -> {
            Node node = new Node().setName(no.getMetadata().getName()).setLabels(no.getMetadata().getLabels());
            // taint
            if (!CollectionUtils.isEmpty(no.getSpec().getTaints())) {
                node.setTaints(JSONArray.parseArray(JSONArray.toJSONString(no.getSpec().getTaints()), Taint.class));
            }
            // ip
            no.getStatus().getAddresses().forEach(address -> {
                if ("InternalIP".equals(address.getType())) {
                    node.setIp(address.getAddress());
                }
            });
            // resource
            NodeResource cpu = new NodeResource();
            NodeResource memory = new NodeResource();
            cpu.setTotal(no.getStatus().getCapacity().get("cpu").getAmount());
            cpu.setAllocated(no.getStatus().getAllocatable().get("cpu").getAmount());
            memory.setTotal(no.getStatus().getCapacity().get("memory").getAmount());
            memory.setAllocated(no.getStatus().getAllocatable().get("memory").getAmount());
            node.setCpu(cpu);
            node.setMemory(memory);
            //status
            no.getStatus().getConditions().forEach(nodeCondition -> {
                if ("Ready".equals(nodeCondition.getType())){
                    node.setStatus(nodeCondition.getStatus());
                }
            });
            //createTime
            node.setCreateTime(
                    DateUtils.parseDate(no.getMetadata().getCreationTimestamp(), DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z));
            return node;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ClusterNodeResourceDto> getNodeResource(String clusterId, List<Node> nodeList, Boolean all) {
        String tempQuery = getTempQuery(nodeList, all);
        // 查询cpu使用量
        String nodeCpuQuery = "sum(irate(node_cpu_seconds_total{mode!=\"idle\", " + tempQuery + "}[5m])) by (kubernetes_pod_node_name)";
        Map<String, Double> nodeCpuUsed = nodeQuery(clusterId, nodeCpuQuery);
        // 查询cpu总量
        String nodeCpuTotalQuery = "count(node_cpu_seconds_total{ mode='system', " + tempQuery + "}) by (kubernetes_pod_node_name)";
        Map<String, Double> nodeCpuTotal = nodeQuery(clusterId, nodeCpuTotalQuery);
        // 查询memory使用量
        String nodeMemoryQuery = "((node_memory_MemTotal_bytes{" + tempQuery + "} - node_memory_MemFree_bytes - node_memory_Cached_bytes - node_memory_Buffers_bytes - node_memory_Slab_bytes)/1024/1024/1024)";
        Map<String, Double> nodeMemoryUsed = nodeQuery(clusterId, nodeMemoryQuery);
        // 查询memory总量
        String nodeMemoryTotalQuery = "(node_memory_MemTotal_bytes{" + tempQuery + "}/1024/1024/1024)";
        Map<String, Double> nodeMemoryTotal = nodeQuery(clusterId, nodeMemoryTotalQuery);
        return nodeList.stream().map(node -> {
            ClusterNodeResourceDto nodeRs = new ClusterNodeResourceDto();
            nodeRs.setClusterId(clusterId);
            nodeRs.setIp(node.getIp());
            nodeRs.setNodeName(node.getName());
            nodeRs.setStatus(node.getStatus());
            nodeRs.setCreateTime(node.getCreateTime());
            // 设置cpu
            nodeRs.setCpuUsed(nodeCpuUsed.getOrDefault(node.getName(), null));
            nodeRs.setCpuTotal(nodeCpuTotal.getOrDefault(node.getName(), null));
            if (nodeRs.getCpuUsed() != null && nodeRs.getCpuTotal() != null) {
                nodeRs.setCpuRate(ResourceCalculationUtil.roundNumber(
                        BigDecimal.valueOf(nodeRs.getCpuUsed() / nodeRs.getCpuTotal() * 100), 2, RoundingMode.CEILING));
            }
            // 设置memory
            nodeRs.setMemoryUsed(nodeMemoryUsed.getOrDefault(node.getName(), null));
            nodeRs.setMemoryTotal(nodeMemoryTotal.getOrDefault(node.getName(), null));
            if (nodeRs.getMemoryUsed() != null && nodeRs.getMemoryUsed() != null){
                nodeRs.setMemoryRate(ResourceCalculationUtil.roundNumber(
                        BigDecimal.valueOf(nodeRs.getMemoryUsed() / nodeRs.getMemoryTotal() * 100), 2, RoundingMode.CEILING));
            }
            return nodeRs;
        }).sorted((o1, o2) -> o1.getCreateTime() == null ? -1
                : o2.getCreateTime() == null ? -1 : o2.getCreateTime().compareTo(o1.getCreateTime()))
                .collect(Collectors.toList());
    }

    @Override
    public ClusterNodeResourceDto getSumNodeResource(String clusterId, List<Node> nodes, Boolean all) {
        ClusterNodeResourceDto clusterNodeResourceDto = new ClusterNodeResourceDto();
        String tempQuery = getTempQuery(nodes, all);
        // 获取cpu使用量
        String cpuUsingQuery = "sum(irate(node_cpu_seconds_total{mode!=\"idle\", " + tempQuery + "}[5m]))";
        Double cpuUsing = prometheusResourceMonitorService.queryAndConvert(clusterId, cpuUsingQuery);

        // 获取cpu总量
        String cpuTotalQuery = "count(node_cpu_seconds_total{ mode='system', " + tempQuery + "})";
        Double cpuTotal = prometheusResourceMonitorService.queryAndConvert(clusterId, cpuTotalQuery);

        // 获取memory使用量
        String memoryUsingQuery = "sum((node_memory_MemTotal_bytes{" + tempQuery
            + "} - node_memory_MemFree_bytes - node_memory_Cached_bytes - node_memory_Buffers_bytes - node_memory_Slab_bytes)/1024/1024/1024)";
        Double memoryUsing = prometheusResourceMonitorService.queryAndConvert(clusterId, memoryUsingQuery);

        // 获取memory总量
        String memoryTotalQuery = "sum(node_memory_MemTotal_bytes{" + tempQuery + "})/1024/1024/1024";
        Double memoryTotal = prometheusResourceMonitorService.queryAndConvert(clusterId, memoryTotalQuery);

        // 计算cpu使用率
        if (cpuUsing != null && cpuTotal != null && cpuTotal != 0) {
            clusterNodeResourceDto.setCpuRate(ResourceCalculationUtil
                .roundNumber(BigDecimal.valueOf(cpuUsing / cpuTotal * 100), 2, RoundingMode.CEILING));
        }
        // 计算memory使用率
        if (cpuUsing != null && cpuTotal != null && cpuTotal != 0) {
            clusterNodeResourceDto.setMemoryRate(ResourceCalculationUtil
                .roundNumber(BigDecimal.valueOf(memoryUsing / memoryTotal * 100), 2, RoundingMode.CEILING));
        }
        clusterNodeResourceDto.setCpuUsed(cpuUsing);
        clusterNodeResourceDto.setCpuTotal(cpuTotal);
        clusterNodeResourceDto.setMemoryUsed(memoryUsing);
        clusterNodeResourceDto.setMemoryTotal(memoryTotal);
        clusterNodeResourceDto.setClusterId(clusterId);
        return clusterNodeResourceDto;
    }

    @Override
    public String getNodeIp(String clusterId) {
        List<io.fabric8.kubernetes.api.model.Node> nodes = nodeWrapper.list(clusterId);
        if (!CollectionUtils.isEmpty(nodes)) {
            List<NodeAddress> addresses = nodes.get(0).getStatus().getAddresses();
            List<NodeAddress> nodeAddresses = addresses.stream().filter(nodeAddress -> "InternalIP".equals(nodeAddress.getType())).collect(Collectors.toList());
            return nodeAddresses.get(0).getAddress();
        }
        return "";
    }

    public Map<String, Double> nodeQuery(String clusterId, String query){
        Map<String, Double> resultMap = new HashMap<>();
        Map<String, String> queryMap = new HashMap<>();
        // 查询cpu使用量
        try {
            queryMap.put("query", query);
            PrometheusResponse nodeMemoryRequest =
                    prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, queryMap);
            if (!CollectionUtils.isEmpty(nodeMemoryRequest.getData().getResult())) {
                nodeMemoryRequest.getData().getResult().forEach(result -> {
                    resultMap.put(result.getMetric().get("kubernetes_pod_node_name"), ResourceCalculationUtil.roundNumber(
                            BigDecimal.valueOf(Double.parseDouble(result.getValue().get(1))), 2, RoundingMode.CEILING));
                });
            }
        }catch (Exception e){
            log.error("node列表，查询cpu失败", e);
        }
        return resultMap;
    }

    private void setClusterStatusCode(MiddlewareClusterDTO clusterDTO, List<io.fabric8.kubernetes.api.model.Node> nodes) {
        for (io.fabric8.kubernetes.api.model.Node node : nodes) {
            if (node.getStatus() == null || CollectionUtils.isEmpty(node.getStatus().getConditions())) {
                clusterDTO.setStatusCode(0);
            }
            List<NodeCondition> conditions = node.getStatus().getConditions();
            NodeCondition nodeCondition = conditions.get(conditions.size() - 1);
            if (!"Ready".equalsIgnoreCase(nodeCondition.getType())) {
                clusterDTO.setStatusCode(0);
            }
        }
        clusterDTO.setStatusCode(1);
    }

    public String getTempQuery(List<Node> nodes, Boolean all) {
        if (all) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("kubernetes_pod_node_name=~\"");
            for (Node node : nodes) {
                sb.append(node.getName()).append("|");
            }
            sb.append("\"");
            return sb.toString();
        }
    }


}
