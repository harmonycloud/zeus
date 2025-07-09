package com.middleware.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.model.k8s.OwnerReferencesDo;
import com.middleware.zeus.common.model.k8s.ServiceDo;
import com.middleware.zeus.common.model.middleware.PortDetailDTO;
import com.middleware.zeus.common.model.middleware.ServicePortDTO;
import com.middleware.zeus.bean.BeanMiddlewareInfo;
import com.middleware.zeus.dao.BeanMiddlewareInfoMapper;
import com.middleware.zeus.integration.cluster.ServiceWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;
import com.middleware.zeus.integration.cluster.bean.MiddlewareStatus;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.MiddlewareCRService;
import com.middleware.zeus.service.k8s.ServiceService;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.util.middleware.InternalServiceFilterUtil;
import com.middleware.zeus.util.middleware.MiddlewareServicePurposeUtil;
import com.skyview.language.annotations.TranslateAfterResult;
import io.fabric8.kubernetes.api.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.NameConstant.REDIS;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.EXPORTER;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.HEADLESS;

/**
 * @author tangtx
 * @date 4/02/21 4:56 PM
 */
@Service
public class ServiceServiceImpl implements ServiceService {

    @Autowired
    private MiddlewareCRService middlewareCRService;

    @Autowired
    private ServiceWrapper serviceWrapper;

    @Autowired
    private HelmChartService helmChartService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private BeanMiddlewareInfoMapper middlewareInfoMapper;

    @Override
    @TranslateAfterResult
    public List<ServicePortDTO> list(String clusterId, String namespace, String name, String type) {
        MiddlewareCR middleware = middlewareCRService.getCR(clusterId, namespace, type, name);
        if (middleware == null || middleware.getStatus() == null) {
            return null;
        }
        MiddlewareStatus status = middleware.getStatus();
        Map<String, List<MiddlewareInfo>> stringListMap = status.getInclude();
        if (stringListMap == null || CollectionUtils.isEmpty(stringListMap.get("services"))) {
            return null;
        }
        List<MiddlewareInfo> middlewareInfoList = stringListMap.get("services");
        List<ServicePortDTO> servicePortDTOList = new ArrayList<>(10);
        JSONObject values = helmChartService.getInstalledValues(name, namespace, clusterService.findById(clusterId));
        QueryWrapper<BeanMiddlewareInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chart_version", values.getOrDefault("chart-version", ""));
        queryWrapper.eq("chart_name", values.getString("chart-name"));
        BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoMapper.selectOne(queryWrapper);

        for (MiddlewareInfo middlewareInfo : middlewareInfoList) {
            if (StringUtils.isBlank(middlewareInfo.getName())) {
                continue;
            }
            io.fabric8.kubernetes.api.model.Service service = serviceWrapper.get(clusterId, namespace, middlewareInfo.getName());
            if (service == null || service.getSpec() == null) {
                continue;
            }
            List<ServicePort> servicePortList = service.getSpec().getPorts();
            if (CollectionUtils.isEmpty(servicePortList)) {
                continue;
            }
            ServicePortDTO servicePortDTO = new ServicePortDTO();
            servicePortDTO.setServiceName(middlewareInfo.getName());
            List<PortDetailDTO> portDetailDTOList = new ArrayList<>();
            for (ServicePort servicePort : servicePortList) {
                if (servicePort.getPort() == null) {
                    continue;
                }
                PortDetailDTO portDetailDTO = new PortDetailDTO();
                portDetailDTO.setPort(servicePort.getPort() + "");
                portDetailDTO.setProtocol(servicePort.getProtocol());
                portDetailDTO.setTargetPort(servicePort.getTargetPort().getIntVal() + "");
                portDetailDTOList.add(portDetailDTO);
            }
            if (!CollectionUtils.isEmpty(portDetailDTOList)) {
                servicePortDTO.setPortDetailDtoList(portDetailDTOList);
            }
            servicePortDTO.setServicePurpose(MiddlewareServicePurposeUtil.convertChinesePurpose(name, type, servicePortDTO.getServiceName()));

            if (beanMiddlewareInfo != null) {
                servicePortDTO.setImagePath(beanMiddlewareInfo.getImagePath());
            }
            servicePortDTOList.add(servicePortDTO);
        }
        return servicePortDTOList.stream().filter(servicePortDTO -> !servicePortDTO.getServiceName().contains(EXPORTER)
            && !servicePortDTO.getServiceName().contains(HEADLESS)).collect(Collectors.toList());
    }

    @Override
    public List<ServicePortDTO> list(String clusterId, String namespace) {
        List<io.fabric8.kubernetes.api.model.Service> serviceList = serviceWrapper.list(clusterId, namespace);
        return serviceList.stream().map(this::convertService).collect(Collectors.toList());
    }

    @Override
    public ServicePortDTO get(String clusterId, String namespace, String name) {
        io.fabric8.kubernetes.api.model.Service service = serviceWrapper.get(clusterId, namespace, name);
        if (service == null || service.getSpec() == null || CollectionUtils.isEmpty(service.getSpec().getPorts())) {
            return null;
        }
        return convertService(service);
    }

    @Override
    @TranslateAfterResult
    public List<ServicePortDTO> listInternalService(String clusterId, String namespace, String name, String type) {
        if ("rocketmq".equals(type)) {
            return getMQInternalService(name, namespace);
        }
        List<ServicePortDTO> servicePortDTOList = list(clusterId, namespace, name, type);
        if(servicePortDTOList == null){
            servicePortDTOList = new ArrayList<>();
        }
        servicePortDTOList.forEach(service -> {
            service.setServicePurpose(MiddlewareServicePurposeUtil.convertChinesePurpose(name, type, service.getServiceName()));
            List<PortDetailDTO> portDetailDtoList = service.getPortDetailDtoList();
            if (!CollectionUtils.isEmpty(portDetailDtoList)) {
                PortDetailDTO portDetailDTO = portDetailDtoList.get(0);
                service.setInternalAddress(service.getServiceName() + "." + namespace + ":" + portDetailDTO.getPort());
            }
        });
        // 过滤掉多余的服务
        servicePortDTOList = filterUnusedService(name, namespace, clusterId, type, servicePortDTOList);

        List<ServicePortDTO> servicePortDTOS = servicePortDTOList.stream().
                filter(servicePortDTO -> servicePortDTO.getServicePurpose() != null && !"null".equals(servicePortDTO.getServicePurpose())).
                collect(Collectors.toList());
        return filterByMiddlewareType(clusterId, namespace, name, type, servicePortDTOS);
    }

    @Override
    public void create(String clusterId, String namespace, ServiceDo serviceDo) {
        // 装换数据结构
        serviceDo.setNamespace(namespace);
        io.fabric8.kubernetes.api.model.Service service = convertServiceDo2Service(serviceDo);
        // 创建service
        serviceWrapper.create(clusterId, namespace, service);
    }

    @Override
    public void delete(String clusterId, String namespace, String name) {
        serviceWrapper.delete(clusterId, namespace, name);
    }

    /**
     * 过滤掉多余的服务
     * @param middlewareName
     * @param namespace
     * @param clusterId
     * @param type
     * @param servicePortDTOList
     * @return
     */
    private List<ServicePortDTO> filterUnusedService(String middlewareName, String namespace, String clusterId, String type, List<ServicePortDTO> servicePortDTOList) {
        String mode = helmChartService.getMiddlewareMode(middlewareName, namespace, clusterId);
        return InternalServiceFilterUtil.filterUnused(type, mode, servicePortDTOList);
    }

    private List<ServicePortDTO> filterByMiddlewareType(String clusterId, String namespace, String name, String type,
        List<ServicePortDTO> servicePortDTOS) {
        if ("redis".equals(type)) {
            JSONObject values =
                helmChartService.getInstalledValues(name, namespace, clusterService.findById(clusterId));
            JSONObject jsonObject = values.getJSONObject("predixy");
            if (jsonObject != null && jsonObject.containsKey("enableProxy") && jsonObject.getBoolean("enableProxy")) {
                return servicePortDTOS.stream()
                    .filter(servicePortDTO -> servicePortDTO.getServiceName().contains("predixy")
                        || servicePortDTO.getServiceName().contains("sentinel"))
                    .collect(Collectors.toList());
            } else {
                return servicePortDTOS;
            }
        } else if ("kafka".equals(type)) {
            return servicePortDTOS.stream()
                .filter(servicePortDTO -> !servicePortDTO.getServiceName().contains("manager"))
                .collect(Collectors.toList());
        } else {
            return servicePortDTOS;
        }
    }

    public ServicePortDTO convertService(io.fabric8.kubernetes.api.model.Service service){
        ServiceSpec spec = service.getSpec();
        ServicePort servicePort = spec.getPorts().get(0);
        ServicePortDTO servicePortDTO = new ServicePortDTO();
        List<PortDetailDTO> portDetailDtoList = new ArrayList<>();
        PortDetailDTO portDetailDTO = new PortDetailDTO();
        portDetailDTO.setPort(String.valueOf(servicePort.getPort().intValue()));
        portDetailDTO.setTargetPort(String.valueOf(servicePort.getTargetPort().getIntVal()));
        portDetailDTO.setProtocol(servicePort.getProtocol());
        portDetailDtoList.add(portDetailDTO);

        servicePortDTO.setServiceName(service.getMetadata().getName());
        servicePortDTO.setClusterIP(spec.getClusterIP());
        servicePortDTO.setPortDetailDtoList(portDetailDtoList);
        servicePortDTO.setSelector(spec.getSelector());
        return servicePortDTO;
    }

    private List<ServicePortDTO> getMQInternalService(String middlewareName,String namespace) {
        String svc0 = middlewareName + "namesrv-0." + middlewareName + "namesrv-headless-svc." + namespace + ":9876";
        String svc1 = middlewareName + "namesrv-1." + middlewareName + "namesrv-headless-svc." + namespace + ":9876";
        ServicePortDTO servicePortDTO0 = new ServicePortDTO();
        servicePortDTO0.setServicePurpose("服务连接");
        servicePortDTO0.setServiceName("namesrv-headless-svc");
        servicePortDTO0.setInternalAddress(svc0 + ";" + svc1);
        List<ServicePortDTO> list = new ArrayList<>();
        list.add(servicePortDTO0);
        return list;
    }

    public io.fabric8.kubernetes.api.model.Service convertServiceDo2Service(ServiceDo serviceDo){
        io.fabric8.kubernetes.api.model.Service service = new io.fabric8.kubernetes.api.model.Service();

        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(serviceDo.getNamespace());
        meta.setName(serviceDo.getName());
        meta.setLabels(serviceDo.getLabels());
        meta.setAnnotations(serviceDo.getAnnotations());

        // 设置OwnerReference
        if (!CollectionUtils.isEmpty(serviceDo.getOwnerReferencesDoList())){
            OwnerReferencesDo ownerReferencesDo = serviceDo.getOwnerReferencesDoList().get(0);
            OwnerReference ownerReference = new OwnerReference();
            ownerReference.setApiVersion(ownerReferencesDo.getApiVersion());
            ownerReference.setKind(ownerReferencesDo.getKind());
            ownerReference.setName(ownerReferencesDo.getName());
            ownerReference.setController(ownerReferencesDo.getController());
            ownerReference.setBlockOwnerDeletion(ownerReferencesDo.getBlockOwnerDeletion());
            ownerReference.setUid(ownerReferencesDo.getUid());
            meta.setOwnerReferences(Collections.singletonList(ownerReference));
        }

        ServiceSpec spec = new ServiceSpec();
        List<ServicePort> servicePortList = new ArrayList<>();
        for (PortDetailDTO portDetailDTO : serviceDo.getPortDetailDtoList()){
            ServicePort servicePort = new ServicePort();
            servicePort.setName(REDIS);
            servicePort.setProtocol("TCP");
            servicePort.setPort(Integer.valueOf(portDetailDTO.getPort()));
            servicePort.setTargetPort(new IntOrString(Integer.valueOf(portDetailDTO.getTargetPort())));
            if (StringUtils.isNotEmpty(portDetailDTO.getNodePort())){
                servicePort.setNodePort(Integer.valueOf(portDetailDTO.getNodePort()));
            }
            servicePortList.add(servicePort);
        }
        spec.setPorts(servicePortList);
        spec.setSelector(serviceDo.getSelector());
        spec.setType(serviceDo.getType());

        service.setMetadata(meta);
        service.setSpec(spec);

        return service;
    }

}
