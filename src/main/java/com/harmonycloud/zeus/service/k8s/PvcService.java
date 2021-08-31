package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.PersistentVolumeClaim;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/04/01
 */
public interface PvcService {

    List<PersistentVolumeClaim> list(String clusterId, String namespace);

}
