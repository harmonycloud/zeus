package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.caas.common.model.middleware.PortDetailDTO;
import com.harmonycloud.caas.common.model.middleware.ServicePortDTO;
import com.harmonycloud.zeus.integration.cluster.ServiceWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareStatus;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.k8s.ServiceService;
import com.harmonycloud.zeus.util.MiddlewareServicePurposeUtil;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.EXPORTER;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.HEADLESS;

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

    @Override
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
            servicePortDTO.setServicePurpose(MiddlewareServicePurposeUtil.convertChinesePurpose(type, servicePortDTO.getServiceName()));
            servicePortDTOList.add(servicePortDTO);
        }
        return servicePortDTOList.stream().filter(servicePortDTO -> !servicePortDTO.getServiceName().contains(EXPORTER)
            && !servicePortDTO.getServiceName().contains(HEADLESS)).collect(Collectors.toList());
    }

    @Override
    public ServicePortDTO get(String clusterId, String namespace, String name) {
        io.fabric8.kubernetes.api.model.Service service = serviceWrapper.get(clusterId, namespace, name);
        if (service == null || service.getSpec() == null || CollectionUtils.isEmpty(service.getSpec().getPorts())) {
            return null;
        }
        ServiceSpec spec = service.getSpec();
        ServicePort servicePort = spec.getPorts().get(0);
        ServicePortDTO servicePortDTO = new ServicePortDTO();
        List<PortDetailDTO> portDetailDtoList = new ArrayList<>();
        PortDetailDTO portDetailDTO = new PortDetailDTO();
        portDetailDTO.setPort(String.valueOf(servicePort.getPort().intValue()));
        portDetailDTO.setTargetPort(String.valueOf(servicePort.getTargetPort().getIntVal()));
        portDetailDTO.setProtocol(servicePort.getProtocol());
        portDetailDtoList.add(portDetailDTO);

        servicePortDTO.setServiceName(name);
        servicePortDTO.setClusterIP(spec.getClusterIP());
        servicePortDTO.setPortDetailDtoList(portDetailDtoList);
        return servicePortDTO;
    }

    @Override
    public List<ServicePortDTO> listInternalService(String clusterId, String namespace, String name, String type) {
        List<ServicePortDTO> servicePortDTOList = list(clusterId, namespace, name, type);
        servicePortDTOList.forEach(service -> {
            service.setServicePurpose(MiddlewareServicePurposeUtil.convertChinesePurpose(type, service.getServiceName()));
            List<PortDetailDTO> portDetailDtoList = service.getPortDetailDtoList();
            if (!CollectionUtils.isEmpty(portDetailDtoList)) {
                PortDetailDTO portDetailDTO = portDetailDtoList.get(0);
                service.setInternalAddress(service.getServiceName() + "." + namespace + ":" + portDetailDTO.getPort());
            }
        });
        return servicePortDTOList;
    }

}
