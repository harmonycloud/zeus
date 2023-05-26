package com.middleware.zeus.service.middleware.impl;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.EventDetail;
import com.middleware.zeus.common.model.PersistentVolumeClaim;
import com.middleware.zeus.common.model.k8s.PvDo;
import com.middleware.zeus.common.model.middleware.MiddlewarePvcDto;
import com.middleware.zeus.integration.cluster.bean.Maintenance;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.MiddlewarePvcService;
import com.middleware.zeus.service.prometheus.PrometheusResourceMonitorService;
import com.middleware.zeus.util.PrometheusQueryUtil;
import io.fabric8.kubernetes.api.model.Quantity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.NameConstant.*;

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
    @Autowired
    private PrometheusResourceMonitorService prometheusResourceMonitorService;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private StorageService storageService;

    @Override
    public List<MiddlewarePvcDto> list(String clusterId, String namespace, String middlewareName, String type) {

        // 获取pvc列表
        List<PersistentVolumeClaim> pvcList = listMiddlewarePvc(clusterId, namespace, middlewareName, type);
        // 封装middlewarePvcDto对象
        List<MiddlewarePvcDto> middlewarePvcDtoList = pvcList.stream().map(this::convertToDto).collect(Collectors.toList());

        // 获取名称list
        List<String> pvcNameList = pvcList.stream().map(PersistentVolumeClaim::getName).collect(Collectors.toList());
        // 获取pv列表(过滤已释放的同名pv)
        List<PvDo> pvList = pvService.listPv(clusterId, namespace, pvcNameList).stream()
            .filter(pvDo -> StringUtils.isEmpty(pvDo.getStatus()) || !"Released".equals(pvDo.getStatus()))
            .collect(Collectors.toList());


        Map<String, String> reclaimPolicyMap = pvList.stream().collect(Collectors.toMap(PvDo::getPvcName, PvDo::getReclaimPolicy));
        for (MiddlewarePvcDto dto : middlewarePvcDtoList){
            // 设置回收策略
            dto.setReclaimPolicy(reclaimPolicyMap.getOrDefault(dto.getPvcName(), null));

            // 设置扩容中
            if (dto.getStorage() != null && dto.getCapacity() != null && dto.getStorage() > dto.getCapacity()){
                dto.setStatus(SCALE_UP_PV);
            }
        }
        return middlewarePvcDtoList;
    }

    @Override
    public List<EventDetail> getEvent(String clusterId, String namespace, String middlewareName, String pvcName) {
        Map<String, String> fields = new HashMap<>();
        fields.put("involvedObject.name", pvcName);
        fields.put("involvedObject.namespace", namespace);
        fields.put("involvedObject.kind", PERSISTENT_VOLUME_CLAIM);
        return eventService.getEvents(clusterId, namespace);
    }

    @Override
    public void scalePvc(String clusterId, String namespace, String middlewareName, String pvcName, String type, String storageClass,
        Double storage, Double targetStorage) {
        // 校验存储大小
        checkStorage(clusterId, storageClass, targetStorage - storage);
        // 扩容
        io.fabric8.kubernetes.api.model.PersistentVolumeClaim pvc = pvcService.get(clusterId, namespace, pvcName);
        pvc.getSpec().getResources().getRequests().put(STORAGE, new Quantity(targetStorage + "Gi"));
        pvcService.update(clusterId, namespace, pvc);
    }

    @Override
    public void rollback(String clusterId, String namespace, String middlewareName, String pvcName) {
        // 获取时间上最新的maintenance
        Maintenance maintenance = maintenanceService.getScaleUp(clusterId, namespace, middlewareName, pvcName);
        if (maintenance == null){
            throw new BusinessException(ErrorMessage.MIDDLEWARE_MAINTENANCE_SCALE_UP_NOT_FOUND);
        }
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
        createMaintenance(clusterId, namespace, middlewareName, null, pvcName, storage, storage, SCALE_UP_PV_ROLL_BACK);
    }

    /**
     * 查询中间件pvc列表
     * */
    public List<PersistentVolumeClaim> listMiddlewarePvc(String clusterId, String namespace, String middlewareName,
        String type) {
        // 查询中间件pvc名称
        List<String> pvcNameList = middlewareCRService.getPvc(clusterId, namespace, type, middlewareName);
        // 查询分区下所有pvc
        List<PersistentVolumeClaim> pvcList = pvcService.list(clusterId, namespace);

        if (CollectionUtils.isEmpty(pvcNameList) || CollectionUtils.isEmpty(pvcList)) {
            return new ArrayList<>();
        }
        // 根据指定名称过滤
        pvcList =
            pvcList.stream().filter(pvc -> pvcNameList.stream().anyMatch(pvcName -> pvcName.equals(pvc.getName())))
                .collect(Collectors.toList());
        return pvcList;
    }

    /**
     * 创建运维组件
     */
    public void createMaintenance(String clusterId, String namespace, String middlewareName, String type, String pvcName,
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
            maintenanceService.scaleStorage(clusterId, namespace, middlewareName, type, pvcNameList, targetStorage, labels);
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
        dto.setCapacity(pvc.getCapacity());
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
        if (CollectionUtils.isEmpty(maintenanceList)) {
            return;
        }
        // 过滤获取扩容/回滚相关
        maintenanceList = maintenanceList.stream()
            .filter(maintenance -> maintenance.getMetadata().getLabels() != null
                && maintenance.getMetadata().getLabels().containsKey(ACTION)
                && (maintenance.getMetadata().getLabels().get(ACTION).equals(SCALE_UP_PV)
                    || maintenance.getMetadata().getLabels().get(ACTION).equals(SCALE_UP_PV_ROLL_BACK)))
            .collect(Collectors.toList());

        // 设置状态
        for (MiddlewarePvcDto middlewarePvcDto : middlewarePvcDtoList) {
            // 过滤获取该pvc的运维组件
            List<Maintenance> pvcMainList = maintenanceList.stream()
                .filter(maintenance -> maintenance.getMetadata().getLabels().containsKey(PVC)
                    && maintenance.getMetadata().getLabels().get(PVC).equals(middlewarePvcDto.getPvcName()))
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(pvcMainList)){
                continue;
            }

            // 根据创建时间排序，获取最新的状态
            pvcMainList.sort(Comparator.comparing(maintenance -> maintenance.getMetadata().getCreationTimestamp()));
            // 封装状态
            Map<String, String> status = convertStatus(pvcMainList.get(pvcMainList.size() - 1), middlewarePvcDto.getPvcName());
            if (!CollectionUtils.isEmpty(status) && status.containsKey(STATUS) && !status.get(STATUS).contains("Success")){
                middlewarePvcDto.setStatus(status.get(STATUS));
            }
        }
    }

    public Map<String, String> convertStatus(Maintenance maintenance, String pvcName) {
        Map<String, String> res = new HashMap<>();
        if (maintenance.getStatus() == null || CollectionUtils.isEmpty(maintenance.getStatus().getConditions())) {
            return null;
        }
        Map<String, Map<String, String>> condition = maintenance.getStatus().getConditions().stream()
            .collect(Collectors.toMap(con -> con.get("pvc"), con -> con));
        if (condition.containsKey(pvcName)) {
            String action = maintenance.getMetadata().getLabels().get(ACTION);
            String status = condition.get(pvcName).get(STATUS);
            if (status.equalsIgnoreCase(RUNNING)) {
                res.put(STATUS, action.equals(SCALE_UP_PV) ? SCALE_UP_PV : SCALE_UP_PV_ROLL_BACK);
            } else if (status.equals(FAILED)) {
                res.put(STATUS, action.equals(SCALE_UP_PV) ? SCALE_UP_PV_FAILED : SCALE_UP_PV_ROLL_BACK_FAILED);
                if (condition.get(pvcName).containsKey(REASON)
                    && StringUtils.isNotEmpty(condition.get(pvcName).get(REASON))) {
                    res.put(REASON, condition.get(pvcName).get(REASON));
                }
            } else if (status.equalsIgnoreCase(SUCCEED)) {
                res.put(STATUS, action.equals(SCALE_UP_PV) ? SCALE_UP_PV_SUCCESS : SCALE_UP_PV_ROLL_BACK_SUCCESS);
            }
            return res;
        }
        return null;
    }

    public void checkStorage(String clusterId, String storageClassName, Double queryStorage) {
        Map<String, String> params = storageService.checkHitachiAndGetParams(clusterId, storageClassName);
        if (params.containsKey("poolID") && params.containsKey("serialNumber")) {
            if (params.containsKey("poolID") && params.containsKey("serialNumber")) {
                String query = PrometheusQueryUtil.queryHitachiFree(storageClassName, params.get("serialNumber"),
                    params.get("poolID"));
                try {
                    Double free = prometheusResourceMonitorService.queryAndConvert(clusterId, query);
                    if (queryStorage > free) {
                        throw new BusinessException(ErrorMessage.STORAGE_NOT_ENOUGH);
                    }
                } catch (Exception e) {
                    log.error("检验hitachi存储内容失败", e);
                }
            }
        }
    }
}
