package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.common.model.k8s.PvDo;
import com.middleware.zeus.integration.cluster.PvWrapper;
import com.middleware.zeus.service.k8s.PvService;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2023/1/10 11:25 上午
 */
@Service
@Slf4j
public class PvServiceImpl implements PvService {

    @Autowired
    private PvWrapper pvWrapper;

    @Override
    public List<PvDo> listPv(String clusterId, String namespace, List<String> pvcNameList) {
        List<PersistentVolume> persistentVolumeList = pvWrapper.list(clusterId);
        if (CollectionUtils.isEmpty(persistentVolumeList)) {
            return new ArrayList<>();
        }

        return persistentVolumeList.stream()
            .filter(pv -> namespace == null || filterByPvc(pv, namespace, pvcNameList))
            .map(this::convertPv).collect(Collectors.toList());
    }

    public boolean filterByPvc(PersistentVolume pv, String namespace, List<String> pvcNameList) {
        return pv.getSpec() != null && pv.getSpec().getClaimRef() != null
            && pv.getSpec().getClaimRef().getNamespace() != null
            && pv.getSpec().getClaimRef().getNamespace().equals(namespace)
            && pvcNameList.stream().anyMatch(pvcName -> pvcName.equals(pv.getSpec().getClaimRef().getName()));
    }

    public PvDo convertPv(PersistentVolume pv) {
        PvDo pvDo = new PvDo();
        pvDo.setPvName(pv.getMetadata().getName());
        pvDo.setPvcName(pv.getSpec().getClaimRef().getName());
        pvDo.setReclaimPolicy(pv.getSpec().getPersistentVolumeReclaimPolicy());
        pvDo.setStatus(pv.getStatus() == null ? null : pv.getStatus().getPhase());
        pvDo.setNamespace(pv.getSpec().getClaimRef().getNamespace());
        return pvDo;
    }
}
