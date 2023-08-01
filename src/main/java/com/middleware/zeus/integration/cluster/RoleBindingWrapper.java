package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareList;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/7/25 11:36 上午
 */
@Slf4j
@Component
public class RoleBindingWrapper {

    public void create(String clusterId, RoleBinding roleBinding){
        K8sClient.getClient(clusterId).rbac().roleBindings().resource(roleBinding).create();
    }

    public void update(String clusterId, RoleBinding roleBinding){
        K8sClient.getClient(clusterId).rbac().roleBindings().resource(roleBinding).update();
    }

    public List<RoleBinding> List(String clusterId, String namespace, Map<String, String> labels) {
        if (CollectionUtils.isEmpty(labels)) {
            labels = new HashMap<>();
        }
        NonNamespaceOperation<RoleBinding, RoleBindingList, Resource<RoleBinding>> roleBindingClient =
            K8sClient.getClient(clusterId).resources(RoleBinding.class, RoleBindingList.class);
        if (StringUtils.isNotEmpty(namespace)) {
            roleBindingClient = ((MixedOperation<RoleBinding, RoleBindingList, Resource<RoleBinding>>)roleBindingClient)
                .inNamespace(namespace);
        }
        RoleBindingList roleBindingList = roleBindingClient.withLabels(labels).list();
        if (roleBindingList == null || CollectionUtils.isEmpty(roleBindingList.getItems())) {
            return new ArrayList<>();
        }
        return roleBindingList.getItems();
    }

    public RoleBinding get(String clusterId, String namespace, String name){
        return K8sClient.getClient(clusterId).rbac().roleBindings().inNamespace(namespace).withName(name).get();
    }

    public void delete(String clusterId, String namespace, String name){
        K8sClient.getClient(clusterId).rbac().roleBindings().inNamespace(namespace).withName(name).delete();
    }

    public void delete(String clusterId, String namespace, Map<String, String> labels) {
        NonNamespaceOperation<RoleBinding, RoleBindingList, Resource<RoleBinding>> roleBindingClient =
            K8sClient.getClient(clusterId).resources(RoleBinding.class, RoleBindingList.class);
        if (StringUtils.isNotEmpty(namespace)) {
            roleBindingClient = ((MixedOperation<RoleBinding, RoleBindingList, Resource<RoleBinding>>)roleBindingClient)
                .inNamespace(namespace);
        }
        roleBindingClient.withLabels(labels).delete();
    }

}
