package com.middleware.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.model.middleware.PortDetailDTO;
import com.middleware.caas.common.model.middleware.ServicePortDTO;
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
import com.middleware.zeus.util.MiddlewareServicePurposeUtil;
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

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.EXPORTER;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.HEADLESS;

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
        if ("rocketmq".equals(type)) {
            return getMQInternalService(name, namespace);
        }
        List<ServicePortDTO> servicePortDTOList = list(clusterId, namespace, name, type);
        servicePortDTOList.forEach(service -> {
            service.setServicePurpose(MiddlewareServicePurposeUtil.convertChinesePurpose(name, type, service.getServiceName()));
            List<PortDetailDTO> portDetailDtoList = service.getPortDetailDtoList();
            if (!CollectionUtils.isEmpty(portDetailDtoList)) {
                PortDetailDTO portDetailDTO = portDetailDtoList.get(0);
                service.setInternalAddress(service.getServiceName() + "." + namespace + ":" + portDetailDTO.getPort());
            }
        });
        List<ServicePortDTO> servicePortDTOS = servicePortDTOList.stream().
                filter(servicePortDTO -> servicePortDTO.getServicePurpose() != null && !"null".equals(servicePortDTO.getServicePurpose())).
                collect(Collectors.toList());
        return filterByMiddlewareType(clusterId, namespace, name, type, servicePortDTOS);
    }

    private List<ServicePortDTO> filterByMiddlewareType(String clusterId, String namespace, String name, String type, List<ServicePortDTO> servicePortDTOS) {
        if ("redis".equals(type)) {
            JSONObject values = helmChartService.getInstalledValues(name, namespace, clusterService.findById(clusterId));
            JSONObject jsonObject = values.getJSONObject("predixy");
            if (jsonObject != null && jsonObject.containsKey("enableProxy") && jsonObject.getBoolean("enableProxy")) {
                return servicePortDTOS.stream().filter(servicePortDTO ->
                        servicePortDTO.getServiceName().contains("predixy")).collect(Collectors.toList());
            } else {
                return servicePortDTOS.stream().filter(servicePortDTO ->
                        !servicePortDTO.getServiceName().contains("sentinel")).collect(Collectors.toList());
            }
        } else if ("kafka".equals(type)) {
            return servicePortDTOS.stream().filter(servicePortDTO ->
                    !servicePortDTO.getServiceName().contains("manager")).collect(Collectors.toList());
        } else {
            return servicePortDTOS;
        }
    }

    private List<ServicePortDTO> getMQInternalService(String middlewareName,String namespace) {
        String svc0 = middlewareName + "namesrv-0." + middlewareName + "namesrv-headless-svc." + namespace + ":9876";
        String svc1 = middlewareName + "namesrv-1." + middlewareName + "namesrv-headless-svc." + namespace + ":9876";
        ServicePortDTO servicePortDTO0 = new ServicePortDTO();
        servicePortDTO0.setServicePurpose("读写");
        servicePortDTO0.setServiceName("namesrv-headless-svc");
        servicePortDTO0.setInternalAddress(svc0 + ";" + svc1);
        List<ServicePortDTO> list = new ArrayList<>();
        list.add(servicePortDTO0);
        return list;
    }

}
