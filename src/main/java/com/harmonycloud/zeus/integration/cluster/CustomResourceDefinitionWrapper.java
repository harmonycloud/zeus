package com.harmonycloud.zeus.integration.cluster;

import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/8/12 1:49 下午
 */
@Slf4j
@Component
public class CustomResourceDefinitionWrapper {

    public List<CustomResourceDefinition> list(String clusterId) {
        CustomResourceDefinitionList crdList = K8sClient.getClient(clusterId).customResourceDefinitions().list();
        if (CollectionUtils.isEmpty(crdList.getItems())) {
            return new ArrayList<>();
        }
        return crdList.getItems();
    }
    
}
