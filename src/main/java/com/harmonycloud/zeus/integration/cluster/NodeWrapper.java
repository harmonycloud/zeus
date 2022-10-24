package com.harmonycloud.zeus.integration.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.NodeList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.fabric8.kubernetes.api.model.Node;
import org.springframework.util.CollectionUtils;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@Component
@Slf4j
public class NodeWrapper {

    @Value("${active-active.label.key:topology.kubernetes.io/zone}")
    private String zoneKey;

    public List<Node> list(String clusterId) {
        NodeList list = K8sClient.getClient(clusterId).nodes().list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public Node get(String clusterId, String nodeName) {
        return K8sClient.getClient(clusterId).nodes().withName(nodeName).get();
    }

    public List<Node> list(String clusterId, Map<String, String> labels){
        NodeList list = K8sClient.getClient(clusterId).nodes().withLabels(labels).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public List<Node> listAllocatableNode(String clusterId){
        NodeList list = K8sClient.getClient(clusterId).nodes().withoutLabel(zoneKey).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public void update(Node node, String clusterId){
        try {
            K8sClient.getClient(clusterId).nodes().createOrReplace(node);
        }catch (Exception e){
            log.error("集群节点更新失败");
            throw e;
        }
    }


    
}
