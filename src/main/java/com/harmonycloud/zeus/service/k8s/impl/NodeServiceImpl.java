package com.harmonycloud.zeus.service.k8s.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.CONTAINER_RUNTIME_VERSION;
import static com.harmonycloud.caas.common.constants.NameConstant.KUBELET_VERSION;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.service.k8s.NodeService;
import com.harmonycloud.zeus.util.K8sConvert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.Node;
import com.harmonycloud.caas.common.model.NodeResource;
import com.harmonycloud.caas.common.model.Taint;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.integration.cluster.NodeWrapper;

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

    @Override
    public List<Node> list(String clusterId) {
        List<io.fabric8.kubernetes.api.model.Node> nodes = nodeWrapper.list(clusterId);
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
    public void setClusterVersion(MiddlewareClusterDTO cluster) {
        try {
            List<io.fabric8.kubernetes.api.model.Node> nodeList = nodeWrapper.list(cluster.getId());
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
                    sbf.append("=");
                    if (taint.getValue() != null) {
                        sbf.append(taint.getValue());
                    }else{
                        sbf.append(":Exists");
                    }
                    sbf.append(":" + taint.getEffect());
                    log.info("node {} taints {}", node.getIp(), sbf);
                    taintsSet.add(sbf.toString());
                });
            }
        });
        return new ArrayList<>(taintsSet);
    }


}
