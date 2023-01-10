package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.zeus.integration.cluster.bean.Maintenance;
import com.harmonycloud.zeus.service.k8s.MaintenanceService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/1/10 4:08 下午
 */
@Service
@Slf4j
public class MaintenanceServiceImpl implements MaintenanceService {


    @Override
    public void scaleStorage(String clusterId, String namespace, List<String> pvcNameList, Double targetStorage) {
        Maintenance maintenance = new Maintenance();

        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(namespace);
    }
}
