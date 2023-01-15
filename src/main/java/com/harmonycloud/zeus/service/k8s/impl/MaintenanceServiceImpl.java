package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.ResourceUnitEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.integration.cluster.MaintenanceWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.Maintenance;
import com.harmonycloud.zeus.integration.cluster.bean.MaintenanceList;
import com.harmonycloud.zeus.integration.cluster.bean.MaintenancePvc;
import com.harmonycloud.zeus.integration.cluster.bean.MaintenanceSpec;
import com.harmonycloud.zeus.service.k8s.MaintenanceService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.NameConstant.*;

/**
 * @author xutianhong
 * @Date 2023/1/10 4:08 下午
 */
@Service
@Slf4j
public class MaintenanceServiceImpl implements MaintenanceService {

    @Autowired
    private MaintenanceWrapper maintenanceWrapper;

    @Override
    public List<Maintenance> list(String clusterId, String namespace, Map<String, String> labels) {
        MaintenanceList maintenanceList = maintenanceWrapper.listByLabels(clusterId, namespace, labels);
        if (CollectionUtils.isEmpty(maintenanceList.getItems())){
            return new ArrayList<>();
        }
        return maintenanceList.getItems();
    }

    @Override
    public List<Maintenance> list(String clusterId, String namespace, String middlewareName, String action) {
        Map<String, String> labels = new HashMap<>(2);
        labels.put(APP, middlewareName);
        if (StringUtils.isNotEmpty(action)){
            labels.put(ACTION, action);
        }
        return maintenanceWrapper.listByLabels(clusterId, namespace, labels).getItems();
    }

    @Override
    public void scaleStorage(String clusterId, String namespace, String middlewareName, List<String> pvcNameList,
        Double targetStorage, Map<String, String> labels) {

        // 元数据
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(namespace);
        meta.setName(middlewareName + UUIDUtils.get8UUID());

        // 标签
        if (!CollectionUtils.isEmpty(labels)){
            meta.setLabels(labels);
        }

        // pvc扩容数据
        List<MaintenancePvc> maintenancePvcList = pvcNameList.stream().map(pvcName -> {
            MaintenancePvc maintenancePvc = new MaintenancePvc();
            maintenancePvc.setPvc(pvcName);
            maintenancePvc.setNamespace(namespace);
            maintenancePvc.setTargetRequestSize(targetStorage + ResourceUnitEnum.GI.getUnit());
            return maintenancePvc;
        }).collect(Collectors.toList());

        MaintenanceSpec spec = new MaintenanceSpec();
        spec.setPvcs(maintenancePvcList);
        spec.setAction(SCALE_UP_PV);

        Maintenance maintenance = new Maintenance();
        maintenance.setMetadata(meta);
        maintenance.setSpec(spec);

        try {
            maintenanceWrapper.create(clusterId, maintenance);
        } catch (Exception e) {
            log.error("集群{} 分区{} 中间件{} pvc{} 扩容失败", clusterId, namespace, middlewareName, pvcNameList.get(0), e);
            throw new BusinessException(ErrorMessage.MIDDLEWARE_PVC_SCALE_UP_FAILED);
        }
    }

    @Override
    public void rollBack(String clusterId, String namespace, String middlewareName, List<String> pvcNameList, Double targetStorage, Map<String, String> labels) {
        // 元数据
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(namespace);
        meta.setName(middlewareName + UUIDUtils.get8UUID());

        // 标签
        if (!CollectionUtils.isEmpty(labels)){
            meta.setLabels(labels);
        }

        // pvc扩容数据
        List<MaintenancePvc> maintenancePvcList = pvcNameList.stream().map(pvcName -> {
            MaintenancePvc maintenancePvc = new MaintenancePvc();
            maintenancePvc.setPvc(pvcName);
            maintenancePvc.setNamespace(namespace);
            maintenancePvc.setRollBackRequestSize(targetStorage + ResourceUnitEnum.GI.getUnit());
            return maintenancePvc;
        }).collect(Collectors.toList());

        MaintenanceSpec spec = new MaintenanceSpec();
        spec.setPvcs(maintenancePvcList);
        spec.setAction(SCALE_UP_PV_ROLL_BACK);

        Maintenance maintenance = new Maintenance();
        maintenance.setMetadata(meta);
        maintenance.setSpec(spec);

        try {
            maintenanceWrapper.create(clusterId, maintenance);
        } catch (Exception e) {
            log.error("集群{} 分区{} 中间件{} pvc{} 创建回滚失败", clusterId, namespace, middlewareName, pvcNameList.get(0), e);
            throw new BusinessException(ErrorMessage.MIDDLEWARE_PVC_ROLL_BACK_FAILED);
        }
    }

    @Override
    public void delete() {
        // todo 中间件删除时清除cr
    }
}
