package com.harmonycloud.zeus.service.middleware.impl;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.NameConstant.PERSISTENT_VOLUME_CLAIM;

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
    public void scalePvc(String clusterId, String namespace, String middlewareName, String pvcName, Double targetStorage) {
        List<String> pvcNameList = new ArrayList<>();
        pvcNameList.add(pvcName);
        maintenanceService.scaleStorage(clusterId, namespace, pvcNameList, targetStorage);

    }

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
}
