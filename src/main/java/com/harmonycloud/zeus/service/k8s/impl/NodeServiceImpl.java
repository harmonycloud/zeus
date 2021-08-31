package com.harmonycloud.zeus.service.k8s.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.CONTAINER_RUNTIME_VERSION;
import static com.harmonycloud.caas.common.constants.NameConstant.KUBELET_VERSION;

import java.util.List;
import java.util.stream.Collectors;

import com.harmonycloud.zeus.service.k8s.NodeService;
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
            if (CollectionUtils.isEmpty(no.getSpec().getTaints())) {
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

}
