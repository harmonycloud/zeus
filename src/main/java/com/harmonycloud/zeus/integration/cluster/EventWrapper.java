package com.harmonycloud.zeus.integration.cluster;

import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.stereotype.Component;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventList;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/3/26 11:41 上午
 */
@Component
public class EventWrapper {

    public List<Event> list(String clusterId, String namespace) {
        KubernetesClient client = K8sClient.getClient(clusterId);
        EventList list = client.events().inNamespace(namespace).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

}
