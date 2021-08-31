package com.harmonycloud.zeus.integration.cluster;

import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;


/**
 * @author tangtx
 * @date 2021/04/01 11:00 AM
 * service
 */
@Component
public class ServiceWrapper {

    public List<Service> list(String clusterId, String namespace) {
        return list(clusterId, namespace, null);
    }

    public List<Service> list(String clusterId, String namespace, String labelKey) {
        ServiceList serviceList;
        if (StringUtils.isEmpty(namespace)) {
            serviceList = K8sClient.getClient(clusterId).services().list();
        } else if (StringUtils.isEmpty(labelKey)) {
            serviceList = K8sClient.getClient(clusterId).services().inNamespace(namespace).list();
        } else {
            serviceList = K8sClient.getClient(clusterId).services().inNamespace(namespace).withLabel(labelKey).list();
        }
        if (CollectionUtils.isEmpty(serviceList.getItems())) {
            return null;
        }
        return serviceList.getItems();
    }

    public Service create(String clusterId, String namespace, Service service) {
        try {
            return K8sClient.getClient(clusterId).services().inNamespace(namespace).create(service);
        } catch (KubernetesClientException e) {
            if (e.getCode() == 409) {
                throw new BusinessException(DictEnum.SERVICE_NAME, service.getMetadata().getName(), ErrorMessage.EXIST);
            }
            throw e;
        }
    }

    public void batchCreate(String clusterId, String namespace, List<Service> serviceList) {
        for (Service service : serviceList) {
            try {
                Service res = K8sClient.getClient(clusterId).services().inNamespace(namespace).createOrReplace(service);
                if (res == null) {
                    throw new BusinessException(ErrorMessage.CREATE_FAIL);
                }
            } catch (KubernetesClientException e) {
                if (e.getCode() == 422 && e.getMessage().contains("provided port is already allocated")) {
                    throw new BusinessException(ErrorMessage.INGRESS_NODEPORT_PORT_EXIST);
                } else if (e.getCode() != 409) {
                    throw e;
                }
            }
        }
    }

    public Service update(String clusterId, String namespace, Service service) {
        Service res = K8sClient.getClient(clusterId).services().inNamespace(namespace).createOrReplace(service);
        return res;
    }

    public boolean delete(String clusterId, String namespace, String name) {
        boolean res = K8sClient.getClient(clusterId).services().inNamespace(namespace).withName(name).delete();
        return res;
    }

    public Service get(String clusterId, String namespace, String name) {
        Service res = K8sClient.getClient(clusterId).services().inNamespace(namespace).withName(name).get();
        return res;
    }

}
