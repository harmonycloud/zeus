package com.harmonycloud.zeus.integration.cluster;

import java.util.ArrayList;
import java.util.List;

import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.NodeList;
import org.springframework.stereotype.Component;

import io.fabric8.kubernetes.api.model.Node;
import org.springframework.util.CollectionUtils;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@Component
public class NodeWrapper {

    public List<Node> list(String clusterId) {
        NodeList list = K8sClient.getClient(clusterId).nodes().list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }
    
}
