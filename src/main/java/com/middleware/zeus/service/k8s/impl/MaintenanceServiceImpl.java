package com.middleware.zeus.service.k8s.impl;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.middleware.ResourceUnitEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.tool.uuid.UUIDUtils;
import com.middleware.zeus.integration.cluster.MaintenanceWrapper;
import com.middleware.zeus.integration.cluster.bean.Maintenance;
import com.middleware.zeus.integration.cluster.bean.MaintenancePvc;
import com.middleware.zeus.integration.cluster.bean.MaintenanceSpec;
import com.middleware.zeus.service.k8s.MaintenanceService;
import com.middleware.zeus.service.k8s.MiddlewareCRService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.middleware.caas.common.constants.CommonConstant.LINE;
import static com.middleware.caas.common.constants.NameConstant.*;

/**
 * @author xutianhong
 * @Date 2023/1/10 4:08 下午
 */
@Service
@Slf4j
public class MaintenanceServiceImpl implements MaintenanceService {

    @Autowired
    private MaintenanceWrapper maintenanceWrapper;
    @Autowired
    private MiddlewareCRService middlewareCRService;

    @Override
    public Maintenance getScaleUp(String clusterId, String namespace, String middlewareName, String pvcName) {
        Map<String, String> labels = new HashMap<>();
        labels.put(APP, middlewareName);
        labels.put(ACTION, SCALE_UP_PV);
        labels.put(PVC, pvcName);

        List<Maintenance> maintenanceList = maintenanceWrapper.listByLabels(clusterId, namespace, labels);
        if (CollectionUtils.isEmpty(maintenanceList)){
            return null;
        }
        // 获取时间上最新的maintenance
        maintenanceList.sort(Comparator.comparing(maintenance -> maintenance.getMetadata().getCreationTimestamp()));
        return maintenanceList.get(maintenanceList.size() - 1);
    }

    @Override
    public List<Maintenance> list(String clusterId, String namespace, Map<String, String> labels) {
        return maintenanceWrapper.listByLabels(clusterId, namespace, labels);
    }

    @Override
    public List<Maintenance> list(String clusterId, String namespace, String middlewareName, String action) {
        Map<String, String> labels = new HashMap<>(2);
        labels.put(APP, middlewareName);
        if (StringUtils.isNotEmpty(action)){
            labels.put(ACTION, action);
        }
        return maintenanceWrapper.listByLabels(clusterId, namespace, labels);
    }

    @Override
    public void scaleStorage(String clusterId, String namespace, String middlewareName, String type, List<String> pvcNameList,
        Double targetStorage, Map<String, String> labels) {

        // 元数据
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(namespace);
        meta.setName(middlewareName + LINE + UUIDUtils.get8UUID());

        // 标签
        if (!CollectionUtils.isEmpty(labels)){
            meta.setLabels(labels);
        }

        // 查询pod列表
        List<String> podList = middlewareCRService.getPod(clusterId, namespace, type, middlewareName);
        // pvc扩容数据
        List<MaintenancePvc> maintenancePvcList = pvcNameList.stream().map(pvcName -> {
            MaintenancePvc maintenancePvc = new MaintenancePvc();
            maintenancePvc.setPvc(pvcName);
            maintenancePvc.setNamespace(namespace);
            maintenancePvc.setTargetRequestSize(targetStorage + ResourceUnitEnum.GI.getUnit());
            if (podList.stream().anyMatch(pvcName::contains)) {
                maintenancePvc.setPod(podList.stream().filter(pvcName::contains).collect(Collectors.toList()).get(0));
            }
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
        meta.setName(middlewareName + LINE + UUIDUtils.get8UUID());

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
    public void delete(String clusterId, String namespace, String middlewareName) {
        // 中间件删除时清除cr
        Map<String, String> labels = new HashMap<>();
        labels.put(APP, middlewareName);

        List<Maintenance> maintenanceList = maintenanceWrapper.listByLabels(clusterId, namespace, labels);
        if (!CollectionUtils.isEmpty(maintenanceList)){
            for (Maintenance maintenance : maintenanceList){
                try {
                    maintenanceWrapper.delete(clusterId, namespace, maintenance.getMetadata().getName());
                } catch (Exception e){
                    log.error("集群:{} 分区:{} Maintenance:{} 删除失败", clusterId, namespace, maintenance.getMetadata().getName());
                }
            }
        }
    }
}
