package com.harmonycloud.zeus.service.middleware.impl;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.EventDetail;
import com.harmonycloud.caas.common.model.PersistentVolumeClaim;
import com.harmonycloud.caas.common.model.k8s.PvDo;
import com.harmonycloud.caas.common.model.middleware.MiddlewarePvcDto;
import com.harmonycloud.zeus.integration.cluster.bean.Maintenance;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.MiddlewarePvcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.NameConstant.*;

/**
 * @author xutianhong
 * @Date 2023/1/10 10:13 上午
 */
@Service
@Slf4j
public class MiddlewarePvcServiceImpl implements MiddlewarePvcService {

    @Autowired
    private PvcService pvcService;
    @Autowired
    private PvService pvService;
    @Autowired
    private EventService eventService;
    @Autowired
    private MaintenanceService maintenanceService;

    @Override
    public List<MiddlewarePvcDto> list(String clusterId, String namespace, String middlewareName, String type) {
        // 获取pvc信息
        Map<String, String> labels = new HashMap<>(1);
        labels.put("app", middlewareName);
        List<PersistentVolumeClaim> pvcList =  pvcService.listWithLabels(clusterId, namespace, labels);

        // 封装middlewarePvcDto对象
        List<MiddlewarePvcDto> middlewarePvcDtoList = pvcList.stream().map(this::convertToDto).collect(Collectors.toList());

        // 获取名称list
        List<String> pvcNameList = pvcList.stream().map(PersistentVolumeClaim::getName).collect(Collectors.toList());
        // 获取pv列表
        List<PvDo> pvList = pvService.listPv(clusterId, namespace, pvcNameList);
        // 设置回收策略
        Map<String, String> reclaimPolicyMap = pvList.stream().collect(Collectors.toMap(PvDo::getPvcName, PvDo::getReclaimPolicy));
        for (MiddlewarePvcDto dto : middlewarePvcDtoList){
            dto.setReclaimPolicy(reclaimPolicyMap.getOrDefault(dto.getPvcName(), null));
        }

        // check scale up
        checkScaleUp(clusterId, namespace, middlewareName, middlewarePvcDtoList);
        return middlewarePvcDtoList;
    }

    @Override
    public List<EventDetail> getEvent(String clusterId, String namespace, String middlewareName, String pvcName) {
        Map<String, String> fields = new HashMap<>();
        fields.put("involvedObject.name", pvcName);
        fields.put("involvedObject.namespace", namespace);
        fields.put("involvedObject.kind", PERSISTENT_VOLUME_CLAIM);
        return eventService.getEvents(clusterId, fields);
    }

    @Override
    public void scalePvc(String clusterId, String namespace, String middlewareName, String pvcName, Double storage, Double targetStorage) {
        // 扩容
        createMaintenance(clusterId, namespace, middlewareName, pvcName, storage, targetStorage, SCALE_UP_PV);
    }

    @Override
    public void rollback(String clusterId, String namespace, String middlewareName, String pvcName) {
        // 设置标签
        Map<String, String> labels = new HashMap<>();
        labels.put(APP, middlewareName);
        labels.put(ACTION, SCALE_UP_PV);
        labels.put(PVC, pvcName);

        List<Maintenance> maintenanceList = maintenanceService.list(clusterId, namespace, labels);
        if (CollectionUtils.isEmpty(maintenanceList)){
            throw new BusinessException(ErrorMessage.MIDDLEWARE_MAINTENANCE_SCALE_UP_NOT_FOUND);
        }

        // 获取时间上最新的maintenance
        maintenanceList.sort(Comparator.comparing(maintenance -> maintenance.getMetadata().getCreationTimestamp()));
        Maintenance maintenance = maintenanceList.get(maintenanceList.size() - 1);
        // 判断maintenance是否符合条件
        if (maintenance.getStatus() == null || CollectionUtils.isEmpty(maintenance.getStatus().getConditions())){
            throw new BusinessException(ErrorMessage.MAINTENANCE_STATUS_ERROR);
        }
        Map<String, String> pvcMap = maintenance.getStatus().getConditions().get(0);
        if (!pvcMap.containsKey(PVC) || !pvcMap.containsKey(STATUS) || !pvcMap.get(PVC).equals(pvcName) || !pvcMap.get(STATUS).equals(FAILED)){
            log.error("maintenance状态条件匹配失败");
            throw new BusinessException(ErrorMessage.MIDDLEWARE_MAINTENANCE_SCALE_UP_NOT_FOUND);
        }

        // 获取回滚pvc容量
        Map<String, String> mainLabels = maintenance.getMetadata().getLabels();
        if (CollectionUtils.isEmpty(mainLabels)){
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }

        Double storage = Double.parseDouble(mainLabels.get(STORAGE));
        // 回滚
        createMaintenance(clusterId, namespace, middlewareName, pvcName, storage, storage, SCALE_UP_PV_ROLL_BACK);
    }
    
    /**
     * 创建运维组件
     */
    public void createMaintenance(String clusterId, String namespace, String middlewareName, String pvcName,
        Double storage, Double targetStorage, String action) {
        // 拼接pvc name
        List<String> pvcNameList = new ArrayList<>();
        pvcNameList.add(pvcName);

        // 设置标签
        Map<String, String> labels = new HashMap<>(2);
        labels.put(APP, middlewareName);
        labels.put(ACTION, action);
        labels.put(STORAGE, String.valueOf(storage));
        labels.put(PVC, pvcName);

        // 创建maintenance
        if (action.equals(SCALE_UP_PV)) {
            maintenanceService.scaleStorage(clusterId, namespace, middlewareName, pvcNameList, targetStorage, labels);
        } else if (action.equals(SCALE_UP_PV_ROLL_BACK)) {
            maintenanceService.rollBack(clusterId, namespace, middlewareName, pvcNameList, storage, labels);
        }
    }

    /**
     * 封装PersistentVolumeClaim对象
     * */
    public MiddlewarePvcDto convertToDto(PersistentVolumeClaim pvc){
        MiddlewarePvcDto dto = new MiddlewarePvcDto();
        dto.setPvcName(pvc.getName());
        dto.setStorage(pvc.getRequest());
        dto.setStatus(pvc.getPhase());
        dto.setCreateTime(pvc.getCreateTime());
        dto.setStorageClass(pvc.getStorageClassName());
        // 拼接访问策略
        StringBuilder sb = new StringBuilder();
        for (String accessMode : pvc.getAccessModes()){
            sb.append(accessMode).append(",");
        }
        if (sb.length() > 0){
            sb.deleteCharAt(sb.length() - 1);
        }
        dto.setAccessModes(sb.toString());
        return dto;
    }

    /**
     * 确认pvc状态是否为扩容中或回滚中
     * */
    public void checkScaleUp(String clusterId, String namespace, String middlewareName,
        List<MiddlewarePvcDto> middlewarePvcDtoList) {
        // 获取该中间件相关的运维cr
        List<Maintenance> maintenanceList = maintenanceService.list(clusterId, namespace, middlewareName, null);
        // 过滤获取扩容/回滚相关
        maintenanceList = maintenanceList.stream()
            .filter(maintenance -> maintenance.getMetadata().getLabels().containsKey(ACTION)
                && (maintenance.getMetadata().getLabels().get(ACTION).equals(SCALE_UP_PV)
                    || maintenance.getMetadata().getLabels().get(ACTION).equals(SCALE_UP_PV_ROLL_BACK)))
            .collect(Collectors.toList());
        for (Maintenance maintenance : maintenanceList) {
            if (maintenance != null && maintenance.getStatus() != null && maintenance.getStatus().getPhase() != null
                && !maintenance.getStatus().getPhase().equals(DONE)) {
                if (!CollectionUtils.isEmpty(maintenance.getStatus().getConditions())) {
                    Map<String, String> map = maintenance.getStatus().getConditions().stream()
                        .collect(Collectors.toMap(con -> con.get("pvc"), con -> con.get("status")));
                    for (MiddlewarePvcDto middlewarePvcDto : middlewarePvcDtoList) {
                        if (map.containsKey(middlewarePvcDto.getPvcName())
                            && "Running".equals(map.get(middlewarePvcDto.getPvcName()))) {
                            if (maintenance.getMetadata().getLabels().get(ACTION).equals(SCALE_UP_PV)) {
                                middlewarePvcDto.setStatus(SCALE_UP_PV);
                            } else if (maintenance.getMetadata().getLabels().get(ACTION)
                                .equals(SCALE_UP_PV_ROLL_BACK)) {
                                middlewarePvcDto.setStatus(SCALE_UP_PV_ROLL_BACK);
                            }
                        }
                    }
                }
            }
        }
    }
}
