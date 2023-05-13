package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;


/**
 * @author dengyulong
 * @date 2021/03/23
 * 封装ingress的处理（调用k8s）
 */
@Component
public class IngressWrapper {

    public List<Ingress> list(String clusterId, String namespace) {
        return list(clusterId, namespace, null, null);
    }

    public List<Ingress> list(String clusterId, String namespace, String labelKey) {
        return list(clusterId, namespace, labelKey, null);
    }

    public List<Ingress> list(String clusterId, String namespace, String labelKey, String labelValue) {
        IngressList ingressList;
        if (StringUtils.isEmpty(namespace)){
            if (StringUtils.isEmpty(labelKey)) {
                ingressList = K8sClient.getClient(clusterId).network().v1().ingresses().inAnyNamespace().list();
            } else {
                ingressList = K8sClient.getClient(clusterId).network().v1().ingresses().inAnyNamespace().withLabel(labelKey).list();
            }
        }else if (StringUtils.isEmpty(labelKey)) {
            ingressList = K8sClient.getClient(clusterId).network().v1().ingresses().inNamespace(namespace).list();
        } else if (StringUtils.isEmpty(labelValue)) {
            ingressList = K8sClient.getClient(clusterId).network().v1().ingresses().inNamespace(namespace)
                .withLabel(labelKey).list();
        } else {
            ingressList = K8sClient.getClient(clusterId).network().v1().ingresses().inNamespace(namespace)
                .withLabel(labelKey, labelValue).list();
        }

        if (CollectionUtils.isEmpty(ingressList.getItems())) {
            return null;
        }
        return ingressList.getItems();
    }

    public Ingress create(String clusterId, String namespace, Ingress ingress) {
        try {
            return K8sClient.getClient(clusterId).network().v1().ingresses().inNamespace(namespace).resource(ingress).create();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 409) {
                throw new BusinessException(DictEnum.INGRESS, ingress.getMetadata().getName(), ErrorMessage.EXIST);
            }
            throw e;
        }
    }

    public Ingress update(String clusterId, String namespace, Ingress ingress) {
        Ingress res = K8sClient.getClient(clusterId).network().v1().ingresses().inNamespace(namespace).resource(ingress).update();
        return res;
    }

    public boolean delete(String clusterId, String namespace, String name) {
        K8sClient.getClient(clusterId).network().v1().ingresses().inNamespace(namespace).withName(name).delete();
        return true;
    }

    public Ingress get(String clusterId, String namespace, String name) {
        Ingress res = K8sClient.getClient(clusterId).network().v1().ingresses().inNamespace(namespace).withName(name).get();
        return res;
    }

}
