package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.Protocol;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.integration.cluster.ConfigMapWrapper;
import com.harmonycloud.zeus.integration.cluster.IngressWrapper;
import com.harmonycloud.zeus.integration.cluster.ServiceWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareStatus;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRDService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareInfoServiceImpl;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareServiceImpl;
import com.harmonycloud.zeus.util.DateUtil;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;
import static com.harmonycloud.caas.common.constants.registry.HelmChartConstant.HELM_RELEASE_ANNOTATION_KEY;
import static com.harmonycloud.caas.common.constants.registry.HelmChartConstant.HELM_RELEASE_LABEL_KEY;
import static com.harmonycloud.caas.common.constants.registry.HelmChartConstant.HELM_RELEASE_LABEL_VALUE;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 处理ingress
 */
@Slf4j
@Service
public class IngressServiceImpl implements IngressService {

    private static final String MIDDLEWARE_TYPE = "middleware_type";
    private static final String MIDDLEWARE_NAME = "middleware_name";
    private static final String NODE_PORT = "nodeport";
    private static final int randomLength = 6;

    @Autowired
    private IngressWrapper ingressWrapper;

    @Autowired
    private ConfigMapWrapper configMapWrapper;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private MiddlewareCRDService middlewareCRDService;

    @Autowired
    private ServiceWrapper serviceWrapper;

    @Autowired
    private MiddlewareService middlewareService;

    @Autowired
    private MiddlewareInfoService middlewareInfoService;

    @Value("${k8s.ingress.default.name:nginx-ingress-controller}")
    private String defaultIngressName;
    @Override
    public List<IngressDTO> list(String clusterId, String namespace, String keyword) {
        List<IngressDTO> ingressDtoList = new ArrayList<>();

        // http routing list
        List<Ingress> ingressList = ingressWrapper.list(clusterId, namespace, MIDDLEWARE_NAME);
        if (!CollectionUtils.isEmpty(ingressList)) {
            for (Ingress ingress : ingressList) {
                ingressDtoList.add(convertDto(ingress));
            }
        }

        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        if (cluster.getIngress().getTcp() != null && cluster.getIngress().getTcp().isEnabled()) {
            // tcp routing list
            ConfigMap configMap = configMapWrapper.get(clusterId, getIngressTcpNamespace(cluster),
                cluster.getIngress().getTcp().getConfigMapName());
            dealTcpRoutine(clusterId, namespace, configMap, ingressDtoList);
        }


        // nodePort routing list
        List<io.fabric8.kubernetes.api.model.Service> serviceList = serviceWrapper.list(clusterId, namespace, MIDDLEWARE_NAME);
        dealNodePortRoutineList(clusterId, namespace, serviceList, ingressDtoList);

        if (CollectionUtils.isEmpty(ingressDtoList)) {
            return ingressDtoList;
        }

        // package assembly
        Map<String, Middleware> middlewareMap = getMiddlewareMap(clusterId,namespace);
        if (middlewareMap != null && !middlewareMap.isEmpty()) {
            for (IngressDTO ingressDTO : ingressDtoList) {
                if (middlewareMap.get(ingressDTO.getMiddlewareName()) == null) {
                    continue;
                }
                Middleware middleware = middlewareMap.get(ingressDTO.getMiddlewareName());
                ingressDTO.setMiddlewareName(middleware.getName());
                ingressDTO.setMiddlewareType(middleware.getType());
                ingressDTO.setMiddlewareNickName(middleware.getAliasName());
            }
        }

        boolean filter = StringUtils.isNotBlank(keyword);
        return ingressDtoList.stream().filter(ingress -> !filter || StringUtils.contains(ingress.getName(), keyword)).collect(Collectors.toList());
    }


    @Override
    public void create(String clusterId, String namespace, String middlewareName, IngressDTO ingressDTO) {
        if (StringUtils.isBlank(ingressDTO.getClusterId())) {
            ingressDTO.setClusterId(clusterId);
        }
        if (StringUtils.isBlank(ingressDTO.getNamespace())) {
            ingressDTO.setNamespace(namespace);
        }
        if (StringUtils.isBlank(ingressDTO.getMiddlewareName())) {
            ingressDTO.setMiddlewareName(middlewareName);
        }
        if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_INGRESS)) {
            if (ingressDTO.getProtocol().equals(Protocol.HTTP.getValue())) {
                Ingress ingress = convertK8sIngress(clusterId, namespace, ingressDTO);
                ingressWrapper.create(clusterId, namespace, ingress);
            } else if (ingressDTO.getProtocol().equals(Protocol.TCP.getValue())) {
                MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
                ConfigMap configMap = covertTcpConfig(cluster, namespace, ingressDTO.getServiceList());
                configMapWrapper.update(clusterId, getIngressTcpNamespace(cluster), configMap);
            }
        } else if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_NODEPORT)) {
            List<io.fabric8.kubernetes.api.model.Service> serviceList = covertNodePortService(clusterId, namespace, middlewareName, ingressDTO);
            if (CollectionUtils.isEmpty(serviceList)) {
                throw new CaasRuntimeException(ErrorMessage.INGRESS_NODEPORT_NOT_NULL);
            }
            serviceWrapper.batchCreate(clusterId, namespace, serviceList);
        } else {
            throw new CaasRuntimeException(ErrorMessage.UNSUPPORT_EXPOSE_TYPE);
        }
    }

    @Override
    public void checkIngressTcpPort(MiddlewareClusterDTO cluster, String namespace, List<ServiceDTO> serviceList) {
        if (CollectionUtils.isEmpty(serviceList)) {
            return;
        }
        // 校验NodePort端口是否已存在
        List<io.fabric8.kubernetes.api.model.Service> svcList = serviceWrapper.list(cluster.getId(), null);
        if (!CollectionUtils.isEmpty(svcList) && svcList.stream()
            .anyMatch(svc -> MIDDLEWARE_EXPOSE_NODEPORT.equals(svc.getKind()) && serviceList.stream().anyMatch(
                dto -> dto.getExposePort().equals(String.valueOf(svc.getSpec().getPorts().get(0).getNodePort()))))) {
            throw new BusinessException(ErrorMessage.INGRESS_NODEPORT_PORT_EXIST);
        }

        // 校验Ingress TCP配置文件
        String ingressTcpCmName = cluster.getIngress().getTcp().getConfigMapName();
        ConfigMap configMap = configMapWrapper.get(cluster.getId(), getIngressTcpNamespace(cluster), ingressTcpCmName);
        if (configMap == null || CollectionUtils.isEmpty(configMap.getData())) {
            return;
        }
        for (ServiceDTO svc : serviceList) {
            if (StringUtils.isBlank(svc.getExposePort())) {
                throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_PORT_NOT_NULL);
            }

            if (StringUtils.isNotBlank(configMap.getData().get(svc.getExposePort()))) {
                throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_PORT_EXIST);
            }
        }
    }

    @Override
    public void createIngressTcp(MiddlewareClusterDTO cluster, String namespace, List<ServiceDTO> serviceList, 
                                 boolean checkPort) {
        if (CollectionUtils.isEmpty(serviceList)) {
            return;
        }
        if (checkPort) {
            checkIngressTcpPort(cluster, namespace, serviceList);
        }
        // 转换Ingress TCP配置文件
        ConfigMap configMap = covertTcpConfig(cluster, namespace, serviceList);
        // 更新配置文件
        configMapWrapper.update(cluster.getId(), getIngressTcpNamespace(cluster), configMap);
    }

    @Override
    public void delete(String clusterId, String namespace, String middlewareName, String name, IngressDTO ingressDTO) {
        if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_INGRESS)) {
            if (ingressDTO.getProtocol().equals(Protocol.HTTP.getValue())) {
                ingressWrapper.delete(clusterId, namespace, name);
            } else if (ingressDTO.getProtocol().equals(Protocol.TCP.getValue())) {
                MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
                if (cluster.getIngress().getTcp() == null || !cluster.getIngress().getTcp().isEnabled()) {
                    return;
                }
                ConfigMap configMap = configMapWrapper.get(clusterId, getIngressTcpNamespace(cluster),
                    cluster.getIngress().getTcp().getConfigMapName());
                removeTcpPort(configMap, ingressDTO.getServiceList());
                configMapWrapper.update(clusterId, getIngressTcpNamespace(cluster), configMap);
            }
        } else if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_NODEPORT)) {
            serviceWrapper.delete(clusterId, namespace, name);
        }

    }

    @Override
    public List<IngressDTO> get(String clusterId, String namespace, String type, String middlewareName) {
        MiddlewareCRD crd = middlewareCRDService.getCR(clusterId, namespace, type, middlewareName);
        if (crd == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middlewareName, ErrorMessage.NOT_EXIST);
        }
        Map<String, List<MiddlewareInfo>> include = crd.getStatus().getInclude();
        if (CollectionUtils.isEmpty(include)) {
            return new ArrayList<>(0);
        }
        List<IngressDTO> resList = new ArrayList<>();
        // ingress
        List<Ingress> ingList = ingressWrapper.list(clusterId, namespace);
        if (!CollectionUtils.isEmpty(ingList)) {
            ingList.forEach(ing -> {
                if (ing.getMetadata().getLabels() == null
                    || !ing.getMetadata().getLabels().containsKey(MIDDLEWARE_NAME)) {
                    return;
                }
                String baseIngressName =
                    getBaseIngressName(middlewareName, type, Protocol.HTTP.getValue().toLowerCase());
                if (!ing.getMetadata().getName().startsWith(baseIngressName)
                    || ing.getMetadata().getName().length() != baseIngressName.length() + 1 + randomLength) {
                    return;
                }
                resList.add(convertDto(ing));
            });
        }

        List<MiddlewareInfo> services = include.get(SERVICES);
        if (!CollectionUtils.isEmpty(services)) {
            MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
            List<String> svcNameList =
                services.stream().map(MiddlewareInfo::getName).distinct().collect(Collectors.toList());
            // service nodePort
            List<io.fabric8.kubernetes.api.model.Service> svcList = serviceWrapper.list(clusterId, namespace);
            if (!CollectionUtils.isEmpty(svcList)) {
                svcList.forEach(svc -> {
                    // 过滤不包含命名规则的中间件
                    if (!svcNameList.contains(svc.getMetadata().getName())
                        && !svcNameList.contains(svc.getMetadata().getName().replace("-" + NODE_PORT, ""))
                        || svc.getMetadata().getLabels() == null
                        || !svc.getMetadata().getLabels().containsKey(MIDDLEWARE_NAME)) {
                        return;
                    }
                    IngressDTO dto = dealNodePortRoutine(clusterId, namespace, cluster.getIngress().getAddress(), svc);
                    if (dto != null) {
                        resList.add(dto);
                    }
                });
            }
            // tcp
            if (cluster.getIngress().getTcp() != null && cluster.getIngress().getTcp().isEnabled()) {
                Middleware detail = middlewareService.detail(clusterId, namespace, middlewareName, type);
                ConfigMap configMap = configMapWrapper.get(clusterId, getIngressTcpNamespace(cluster),
                    cluster.getIngress().getTcp().getConfigMapName());
                Map<String, List<ServiceDTO>> tcpRoutineMap = getTcpRoutineMap(configMap);
                svcNameList.forEach(svc -> {
                    IngressDTO tcpDto = getTcpRoutineDetail(clusterId, namespace, crd, svc, tcpRoutineMap);
                    if (tcpDto != null) {
                        resList.add(tcpDto.setMiddlewareType(type).setMiddlewareNickName(detail.getAliasName()));
                    }
                });
            }
        }

        return resList;
    }

    @Override
    public IngressDTO get(String clusterId, String namespace, String type, String middlewareName, String name, String exposeType, String protocol) {
        if (StringUtils.equals(exposeType, MIDDLEWARE_EXPOSE_INGRESS)) {
            if (StringUtils.equals(protocol, Protocol.HTTP.getValue())) {
                Ingress ingress = ingressWrapper.get(clusterId, namespace, name);
                return convertDto(ingress);
            } else if (StringUtils.equals(protocol, Protocol.TCP.getValue())) {
                MiddlewareCRD crd = middlewareCRDService.getCR(clusterId, namespace, type, middlewareName);
                MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
                if (cluster.getIngress().getTcp() == null || !cluster.getIngress().getTcp().isEnabled()) {
                    return null;
                }
                ConfigMap configMap = configMapWrapper.get(clusterId, getIngressTcpNamespace(cluster),
                    cluster.getIngress().getTcp().getConfigMapName());
                Map<String, List<ServiceDTO>> tcpRoutineMap = getTcpRoutineMap(configMap);
                return getTcpRoutineDetail(clusterId, namespace, crd, name, tcpRoutineMap);
            }
        } else if (StringUtils.equals(exposeType, MIDDLEWARE_EXPOSE_NODEPORT)) {
            io.fabric8.kubernetes.api.model.Service service = serviceWrapper.get(clusterId, namespace, name);
            MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
            return dealNodePortRoutine(clusterId, namespace, cluster.getIngress().getAddress(), service);
        }

        return null;
    }

    @Override
    public List<IngressRuleDTO> getHelmIngress(String clusterId, String namespace, String helmReleaseName) {
        List<Ingress> list =
            ingressWrapper.list(clusterId, namespace, HELM_RELEASE_LABEL_KEY, HELM_RELEASE_LABEL_VALUE);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        List<IngressRuleDTO> resList = new ArrayList<>();
        list.forEach(ingress -> {
            if (CollectionUtils.isEmpty(ingress.getMetadata().getAnnotations())) {
                return;
            }
            if (!StringUtils.equals(helmReleaseName,
                ingress.getMetadata().getAnnotations().get(HELM_RELEASE_ANNOTATION_KEY))) {
                return;
            }
            IngressRule rule = ingress.getSpec().getRules().get(0);
            IngressRuleDTO dto =
                new IngressRuleDTO().setIngressName(ingress.getMetadata().getName()).setDomain(rule.getHost());
            HTTPIngressPath httpPath = rule.getHttp().getPaths().get(0);
            dto.setIngressHttpPaths(Collections.singletonList(
                new IngressHttpPath().setPath(httpPath.getPath()).setServiceName(httpPath.getBackend().getServiceName())
                    .setServicePort(httpPath.getBackend().getServicePort().toString())));
            resList.add(dto);
        });
        return resList;
    }

    private List<io.fabric8.kubernetes.api.model.Service> covertNodePortService(String clusterId, String namespace, String middlewareName, IngressDTO ingressDTO) {
        List<ServiceDTO> serviceDTOList = ingressDTO.getServiceList();
        if (CollectionUtils.isEmpty(serviceDTOList)) {
            return null;
        }
        Map<String, io.fabric8.kubernetes.api.model.Service> map = new HashMap<>(serviceDTOList.size());
        for (ServiceDTO serviceDTO : serviceDTOList) {
            String serviceName = serviceDTO.getServiceName();
            if (StringUtils.isBlank(serviceName)) {
                continue;
            }


            io.fabric8.kubernetes.api.model.Service serviceR = map.get(serviceName);
            if (serviceR != null) {
                if (covertServicePort(serviceDTO) != null) {
                    List<ServicePort> servicePortList = serviceR.getSpec().getPorts();
                    servicePortList.add(covertServicePort(serviceDTO));
                    serviceR.getSpec().setPorts(servicePortList);
                }
                continue;
            }

            io.fabric8.kubernetes.api.model.Service serviceOriginal = serviceWrapper.get(clusterId, namespace, serviceName);
            ServiceSpec serviceSpecOriginal = serviceOriginal.getSpec();
            if (serviceSpecOriginal == null) {
                continue;
            }

            io.fabric8.kubernetes.api.model.Service service = new io.fabric8.kubernetes.api.model.Service();
            ObjectMeta objectMeta = new ObjectMeta();
            objectMeta.setName(getNodePortSvcName(serviceName));
            objectMeta.setNamespace(namespace);
            Map<String, String> labels = new HashMap<>(2);
            labels.put(MIDDLEWARE_NAME, middlewareName);
            labels.put(MIDDLEWARE_TYPE, ingressDTO.getMiddlewareType());
            objectMeta.setLabels(labels);
            service.setMetadata(objectMeta);

            ServiceSpec spec = new ServiceSpec();


            List<ServicePort> servicePortList = new ArrayList<>(10);
            if (covertServicePort(serviceDTO) == null) {
                continue;
            }
            servicePortList.add(covertServicePort(serviceDTO));
            spec.setPorts(servicePortList);
            spec.setSelector(serviceSpecOriginal.getSelector());
            spec.setType(MIDDLEWARE_EXPOSE_NODEPORT);
            service.setSpec(spec);

            map.put(serviceName, service);

        }
        List<io.fabric8.kubernetes.api.model.Service> serviceList = new ArrayList<>(10);
        for (String key : map.keySet()) {
            if (map.get(key) == null) {
                continue;
            }
            serviceList.add(map.get(key));
        }

        return serviceList;
    }

    private String getNodePortSvcName(String serviceName) {
        return serviceName + "-" + NODE_PORT;
    }

    private ServicePort covertServicePort(ServiceDTO serviceDTO) {
        if (StringUtils.isBlank(serviceDTO.getExposePort())) {
            return null;
        }
        ServicePort servicePort = new ServicePort();
        servicePort.setNodePort(Integer.parseInt(serviceDTO.getExposePort()));
        servicePort.setProtocol(Protocol.TCP.getValue());
        servicePort.setPort(Integer.parseInt(serviceDTO.getServicePort()));

        IntOrString intOrString = new IntOrString();
        intOrString.setIntVal(Integer.parseInt(serviceDTO.getTargetPort()));
        servicePort.setTargetPort(intOrString);

        return servicePort;
    }

    private void dealNodePortRoutineList(String clusterId, String namespace, List<io.fabric8.kubernetes.api.model.Service> serviceList, List<IngressDTO> ingressDtoList) {
        if (CollectionUtils.isEmpty(serviceList)) {
            return;
        }

        MiddlewareClusterDTO middlewareCluster = clusterService.findById(clusterId);
        String exposeIP = middlewareCluster.getIngress().getAddress();
        for (io.fabric8.kubernetes.api.model.Service service : serviceList) {
            IngressDTO ingressDTO = dealNodePortRoutine(clusterId, namespace, exposeIP, service);
            if (ingressDTO == null) {
                continue;
            }
            ingressDtoList.add(ingressDTO);
        }
    }

    private IngressDTO dealNodePortRoutine(String clusterId, String namespace, String exposeIP, io.fabric8.kubernetes.api.model.Service service) {
        if (service == null) {
            return null;
        }
        if (!StringUtils.equals(service.getSpec().getType(), MIDDLEWARE_EXPOSE_NODEPORT)) {
            return null;
        }
        IngressDTO ingressDTO = new IngressDTO();
        ingressDTO.setNamespace(namespace);
        ingressDTO.setClusterId(clusterId);
        ingressDTO.setName(service.getMetadata().getName());
        ingressDTO.setExposeIP(exposeIP);
        ingressDTO.setExposeType(MIDDLEWARE_EXPOSE_NODEPORT);
        ingressDTO.setProtocol(Protocol.TCP.getValue());
        ingressDTO.setCreateTime(DateUtil.utc2Local(service.getMetadata().getCreationTimestamp(),
                DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue()));

        Map<String, String> labels = service.getMetadata().getLabels();
        if (labels != null && !labels.isEmpty()) {
            ingressDTO.setMiddlewareName(labels.get(MIDDLEWARE_NAME));
            ingressDTO.setMiddlewareType(labels.get(MIDDLEWARE_TYPE));
        }

        List<ServicePort> servicePortList = service.getSpec().getPorts();
        if (CollectionUtils.isEmpty(servicePortList)) {
            return null;
        }
        List<ServiceDTO> serviceDTOList = new ArrayList<>(10);
        for (ServicePort servicePort : servicePortList) {
            ServiceDTO serviceDTO = new ServiceDTO();
            serviceDTO.setServiceName(service.getMetadata().getName());
            serviceDTO.setServicePort(servicePort.getPort() + "");
            serviceDTO.setExposePort(servicePort.getNodePort() + "");
            serviceDTO.setTargetPort(servicePort.getTargetPort().getIntVal() + "");
            serviceDTOList.add(serviceDTO);
        }
        ingressDTO.setServiceList(serviceDTOList);
        return ingressDTO;
    }

    private Map<String, List<ServiceDTO>> getTcpRoutineMap(ConfigMap configMap) {
        if (configMap == null) {
            return new HashMap<>(0);
        }
        Map<String, String> data = configMap.getData();
        if (CollectionUtils.isEmpty(data)) {
            return new HashMap<>(0);
        }

        Map<String, List<ServiceDTO>> tcpRoutineMap = new HashMap<>(data.size());
        data.forEach((k, v) -> {
            // 格式如"30001": blue/mqtest-svc-0:80
            if (StringUtils.isBlank(v)) {
                return;
            }
            String[] domainAndPort = v.split(":");
            if (domainAndPort.length != 2) {
                return;
            }
            List<ServiceDTO> dtoList = tcpRoutineMap.computeIfAbsent(domainAndPort[0], t -> new ArrayList<>());
            ServiceDTO dto = new ServiceDTO().setExposePort(k).setServicePort(domainAndPort[1])
                .setServiceName(domainAndPort[0].split("/")[1]);
            dtoList.add(dto);
        });
        return tcpRoutineMap;
    }

    private IngressDTO getTcpRoutineDetail(String clusterId, String namespace, MiddlewareCRD crd,
                                           String svcName, Map<String, List<ServiceDTO>> tcpRoutineMap) {
        String nsSvcName = namespace + "/" + svcName;
        List<ServiceDTO> svcDtoList = tcpRoutineMap.get(nsSvcName);
        // 没有匹配的
        if (CollectionUtils.isEmpty(svcDtoList)) {
            return null;
        }

        MiddlewareClusterDTO middlewareClusterDTO = clusterService.findById(clusterId);
        return new IngressDTO()
                .setMiddlewareName(crd.getMetadata().getName())
                .setClusterId(clusterId)
                .setClusterNickname(middlewareClusterDTO.getNickname())
                .setName(getIngressTcpName(svcDtoList.get(0).getServiceName(), namespace))
                .setNamespace(namespace)
                .setExposeIP(middlewareClusterDTO.getIngress().getAddress())
                .setExposeType(MIDDLEWARE_EXPOSE_INGRESS)
                .setProtocol(Protocol.TCP.getValue())
                .setServiceList(svcDtoList);
    }

    /**
     * remove service from ConfigMap
     *
     * @param configMap
     * @param serviceDTOList
     */
    private void removeTcpPort(ConfigMap configMap, List<ServiceDTO> serviceDTOList) {
        if (configMap == null || configMap.getData().isEmpty()) {
            throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_CONFIG_NOT_EXIST);
        }
        Map<String, String> data = configMap.getData();
        for (ServiceDTO serviceDTO : serviceDTOList) {
            if (StringUtils.isBlank(serviceDTO.getExposePort())) {
                continue;
            }
            data.keySet().removeIf(key -> key.equals(serviceDTO.getExposePort()));
        }
        configMap.setData(data);
    }

    /**
     * IngressDTO（TCP） covert to ConfigMap
     *
     * @param cluster
     * @param namespace
     * @param serviceDTOList
     * @return
     */
    private ConfigMap covertTcpConfig(MiddlewareClusterDTO cluster, String namespace, List<ServiceDTO> serviceDTOList) {
        if (CollectionUtils.isEmpty(serviceDTOList)) {
            throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_NOT_NULL);
        }

        if (cluster.getIngress().getTcp() == null || !cluster.getIngress().getTcp().isEnabled()) {
            throw new CaasRuntimeException(ErrorMessage.UNSUPPORT_EXPOSE_TYPE);
        }
        String ingressTcpCmName = cluster.getIngress().getTcp().getConfigMapName();
        String ingressTcpNamespace = getIngressTcpNamespace(cluster);

        // tcp routing list
        ConfigMap configMap = configMapWrapper.get(cluster.getId(), ingressTcpNamespace, ingressTcpCmName);
        if (configMap == null) {
            configMap = new ConfigMap();
            ObjectMeta objectMeta = new ObjectMeta();
            objectMeta.setNamespace(ingressTcpNamespace);
            objectMeta.setName(ingressTcpCmName);
            configMap.setMetadata(objectMeta);
        }
        Map<String, String> data = configMap.getData();
        if (CollectionUtils.isEmpty(data)) {
            data = new HashMap<>(16);
        }
        for (ServiceDTO serviceDTO : serviceDTOList) {
            if (StringUtils.isBlank(serviceDTO.getExposePort())) {
                throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_PORT_NOT_NULL);
            }

            if (StringUtils.isNotBlank(data.get(serviceDTO.getExposePort()))) {
                throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_PORT_EXIST);
            }

            String serviceInfo = namespace + "/" + serviceDTO.getServiceName() + ":" + serviceDTO.getServicePort();
            data.put(serviceDTO.getExposePort(), serviceInfo);
        }
        configMap.setData(data);
        return configMap;
    }

    /**
     * tcp configMap transform to IngressDTO list
     *
     * @param clusterId
     * @param namespace
     * @param configMap
     * @param ingressDtoList
     */
    private void dealTcpRoutine(String clusterId, String namespace, ConfigMap configMap, List<IngressDTO> ingressDtoList) {
        if (configMap == null) {
            return;
        }

        if (configMap.getData() == null || configMap.getData().isEmpty()) {
            return;
        }
        Map<String, String> data = configMap.getData();

        List<MiddlewareCRD> middlewareList = middlewareCRDService.listCR(clusterId, namespace, null);
        if (CollectionUtils.isEmpty(middlewareList)) {
            return;
        }
        Map<String, Map<String, String>> mapHashMap = new HashMap<>(10);
        for (MiddlewareCRD middleware : middlewareList) {
            MiddlewareStatus status = middleware.getStatus();
            if (status == null) {
                continue;
            }
            Map<String, List<MiddlewareInfo>> stringListMap = status.getInclude();
            if (stringListMap == null) {
                continue;
            }
            List<MiddlewareInfo> middlewareInfoList = stringListMap.get(SERVICES);
            if (CollectionUtils.isEmpty(middlewareInfoList)) {
                continue;
            }
            for (MiddlewareInfo middlewareInfo : middlewareInfoList) {
                Map<String, String> map = new HashMap<>(2);
                map.put("name", middleware.getSpec().getName());
                map.put("type", middleware.getSpec().getType());
                mapHashMap.put(middlewareInfo.getName(), map);
            }
        }

        MiddlewareClusterDTO middlewareCluster = clusterService.findById(clusterId);
        Map<String, IngressDTO> map = new HashMap<>(10);
        for (String key : data.keySet()) {
            String serviceInfo = data.get(key);
            String[] serviceInfos = serviceInfo.split(":");
            if (serviceInfos.length != 2) {
                continue;
            }
            ServiceDTO serviceDTO = new ServiceDTO();
            serviceDTO.setExposePort(key);
            serviceDTO.setServicePort(serviceInfos[1]);
            String[] serviceNames = serviceInfos[0].split("/");
            if (serviceNames.length != 2) {
                continue;
            }
            if (!StringUtils.equals(serviceNames[0], namespace)) {
                continue;
            }
            serviceDTO.setServiceName(serviceNames[1]);

            IngressDTO ingressDTO = map.get(serviceInfos[0]);
            if (ingressDTO != null) {
                List<ServiceDTO> serviceDTOList = ingressDTO.getServiceList();
                serviceDTOList.add(serviceDTO);
                continue;
            }

            ingressDTO = new IngressDTO();
            ingressDTO.setClusterId(clusterId);
            ingressDTO.setNamespace(serviceNames[0]);
            List<ServiceDTO> list = new ArrayList<>(1);
            list.add(serviceDTO);
            ingressDTO.setServiceList(list);
            ingressDTO.setProtocol(Protocol.TCP.getValue());
            ingressDTO.setName(getIngressTcpName(serviceNames[1], serviceNames[0]));
            ingressDTO.setExposeIP(middlewareCluster.getIngress().getAddress());
            ingressDTO.setExposeType(MIDDLEWARE_EXPOSE_INGRESS);
            Map<String,String> stringStringMap = mapHashMap.get(serviceNames[1]);
            if (stringStringMap != null) {
                ingressDTO.setMiddlewareType(stringStringMap.get("type"));
                ingressDTO.setMiddlewareName(stringStringMap.get("name"));
            }

            map.put(serviceInfos[0], ingressDTO);
        }

        for (String key : map.keySet()) {
            ingressDtoList.add(map.get(key));
        }
    }


    /**
     * IngressDTO convert to Ingress（k8s）
     *
     * @param ingressDTO
     * @return
     */
    private Ingress convertK8sIngress(String clusterId, String namespace, IngressDTO ingressDTO) {
        Ingress ingress = new Ingress();
        ObjectMeta metadata = new ObjectMeta();

        String ingressName = ingressDTO.getName();
        if (StringUtils.isBlank(ingressName)) {
            ingressName = getIngressName(ingressDTO);
        }
        metadata.setName(ingressName);
        Map<String, String> labels = new HashMap<>(1);
        labels.put("middleware_name", ingressDTO.getMiddlewareName());
        labels.put("middleware_type", ingressDTO.getMiddlewareType());
        metadata.setLabels(labels);
        metadata.setNamespace(namespace);

        Map<String, String> annotations = new HashMap<>(1);
        String ingressClassName = clusterService.findById(clusterId).getIngress().getIngressClassName();
        annotations.put("kubernetes.io/ingress.class",
                StringUtils.isBlank(ingressClassName )? defaultIngressName : ingressClassName);
        metadata.setAnnotations(annotations);
        ingress.setMetadata(metadata);

        IngressSpec spec = new IngressSpec();
        List<IngressRule> rules = new ArrayList<>(10);

        if (!CollectionUtils.isEmpty(ingressDTO.getRules())) {
            for (IngressRuleDTO ruleDTO : ingressDTO.getRules()) {
                if (StringUtils.isBlank(ruleDTO.getDomain())) {
                    continue;
                }
                IngressRule rule = new IngressRule();
                rule.setHost(ruleDTO.getDomain());
                if (!CollectionUtils.isEmpty(ruleDTO.getIngressHttpPaths())) {
                    List<HTTPIngressPath> paths = new ArrayList<>();
                    for (IngressHttpPath ingressHttpPath : ruleDTO.getIngressHttpPaths()) {
                        if (StringUtils.isBlank(ingressHttpPath.getPath())) {
                            continue;
                        }
                        HTTPIngressPath httpIngressPath = new HTTPIngressPath();
                        httpIngressPath.setPath(ingressHttpPath.getPath());

                        IngressBackend ingressBackend = new IngressBackend();
                        IntOrString servicePort = new IntOrString();
                        servicePort.setIntVal(Integer.parseInt(ingressHttpPath.getServicePort()));
                        ingressBackend.setServicePort(servicePort);
                        ingressBackend.setServiceName(ingressHttpPath.getServiceName());
                        httpIngressPath.setBackend(ingressBackend);
                        paths.add(httpIngressPath);
                    }
                    if (paths.size() > 0) {
                        HTTPIngressRuleValue http = new HTTPIngressRuleValue();
                        http.setPaths(paths);
                        rule.setHttp(http);
                    }
                }
                rules.add(rule);
            }
        }

        spec.setRules(rules);

        // TODO support HTTPS
        if (ingressDTO.getProtocol().equals(Protocol.HTTPS.getValue())) {
            List<IngressTLS> tls = new ArrayList<>(1);
            IngressTLS ingressTls = new IngressTLS();
            tls.add(ingressTls);
            spec.setTls(tls);
        }
        ingress.setSpec(spec);

        return ingress;
    }
    
    private String getBaseIngressName(String middlewareName, String middlewareType, String protocol) {
        return middlewareName + "-" + middlewareType + "-" + protocol;
    }

    private String getIngressName(IngressDTO ingressDTO) {
        return getBaseIngressName(ingressDTO.getMiddlewareName(), ingressDTO.getMiddlewareType(),
            ingressDTO.getProtocol().toLowerCase()) + "-"
            + PasswordUtils.generateCommonPassword(randomLength).toLowerCase();
    }

    private String getIngressTcpName(String serviceName, String namespace) {
        return serviceName + "-" + namespace + "-" + Protocol.TCP.getValue().toLowerCase();
    }

    private String getIngressTcpNamespace(MiddlewareClusterDTO cluster) {
        return cluster.getIngress().getTcp() == null
            || StringUtils.isBlank(cluster.getIngress().getTcp().getNamespace()) ? KUBE_SYSTEM
                : cluster.getIngress().getTcp().getNamespace();
    }

    /**
     * Ingress（k8s）convert to IngressDTO
     * @param ingress
     * @return
     */
    private IngressDTO convertDto(Ingress ingress) {
        IngressDTO ingressDTO = new IngressDTO();
        ingressDTO.setName(ingress.getMetadata().getName());
        ingressDTO.setNamespace(ingress.getMetadata().getNamespace());
        ingressDTO.setHttpExposePort("80");
        ingressDTO.setExposeType(MIDDLEWARE_EXPOSE_INGRESS);
        ingressDTO.setProtocol(Protocol.HTTP.getValue());
        ingressDTO.setCreateTime(DateUtil.utc2Local(ingress.getMetadata().getCreationTimestamp(),
                DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue()));
        List<IngressRuleDTO> rules = new ArrayList<>(1);
        List<IngressRule> ingressRuleList = ingress.getSpec().getRules();
        if (!CollectionUtils.isEmpty(ingressRuleList)) {
            for (IngressRule rule : ingressRuleList) {
                if (StringUtils.isBlank(rule.getHost())) {
                    continue;
                }
                IngressRuleDTO ingressRuleDTO = new IngressRuleDTO();
                ingressRuleDTO.setDomain(rule.getHost());
                HTTPIngressRuleValue httpIngressRuleValue = rule.getHttp();
                List<HTTPIngressPath> httpIngressPathList = httpIngressRuleValue.getPaths();
                if (!CollectionUtils.isEmpty(httpIngressPathList)) {
                    List<IngressHttpPath> ingressHttpPaths = new ArrayList<>();
                    for (HTTPIngressPath httpIngressPath : httpIngressPathList) {
                        if (StringUtils.isBlank(httpIngressPath.getPath())) {
                            continue;
                        }
                        IngressBackend ingressBackend = httpIngressPath.getBackend();
                        if (ingressBackend == null) {
                            continue;
                        }
                        IngressHttpPath ingressHttpPath = new IngressHttpPath();
                        ingressHttpPath.setPath(httpIngressPath.getPath());
                        ingressHttpPath.setServiceName(ingressBackend.getServiceName());
                        ingressHttpPath.setServicePort(ingressBackend.getServicePort().getIntVal() + "");
                        ingressHttpPaths.add(ingressHttpPath);
                    }
                    if (!CollectionUtils.isEmpty(ingressHttpPaths)) {
                        ingressRuleDTO.setIngressHttpPaths(ingressHttpPaths);
                    }
                }
                rules.add(ingressRuleDTO);
            }
        }
        ingressDTO.setRules(rules);

        Map<String, String> labels = ingress.getMetadata().getLabels();
        if (StringUtils.isNotBlank(labels.get(MIDDLEWARE_TYPE))) {
            ingressDTO.setMiddlewareType(labels.get(MIDDLEWARE_TYPE));
        }
        if (StringUtils.isNotBlank(labels.get(MIDDLEWARE_NAME))) {
            ingressDTO.setMiddlewareName(labels.get(MIDDLEWARE_NAME));
        }
        return ingressDTO;
    }

    private Map<String, Middleware> getMiddlewareMap(String clusterId, String namespace) {
        List<Middleware> middlewareList = middlewareService.simpleList(clusterId, namespace, null,null);
        if (CollectionUtils.isEmpty(middlewareList)) {
            return null;
        }
        Map<String, Middleware> map = new HashMap<>(middlewareList.size());
        for (Middleware middleware : middlewareList) {
            if (middleware == null) {
                continue;
            }
            if (StringUtils.isBlank(middleware.getName())) {
                continue;
            }
            map.put(middleware.getName(), middleware);
        }

        return map;
    }

    @Override
    public List listAllIngress(String clusterId, String namespace, String keyword) {
        List<MiddlewareInfoDTO> middlewareInfoDTOList = middlewareInfoService.list(clusterId);
        List<Map<String, Object>> ingressList = new ArrayList<>();
        boolean filter = StringUtils.isBlank(keyword);
        List<Middleware> middlewareServiceList = middlewareService.simpleList(clusterId, namespace, null, null);
        middlewareInfoDTOList.forEach(middlewareInfoDTO -> {
            List<IngressDTO> ingressDTOList = new ArrayList<>();
            for (Middleware middleware : middlewareServiceList) {
                if (!middlewareInfoDTO.getChartName().equals(middleware.getType())) {
                    break;
                }
                List<IngressDTO> singleIngressDTOList = get(clusterId, namespace, middlewareInfoDTO.getChartName(), middleware.getName());
                singleIngressDTOList.forEach(ingressDTO -> {
                    List<ServiceDTO> serviceList = ingressDTO.getServiceList();
                    if (!CollectionUtils.isEmpty(serviceList)) {
                        ServiceDTO serviceDTO = serviceList.get(0);
                        ingressDTO.setExposePort(serviceDTO.getExposePort());
                        ingressDTO.setServicePort(serviceDTO.getServicePort());
                    }
                });
                List<IngressDTO> ingressDTOS = singleIngressDTOList.stream().filter(ingress -> filter ||
                        StringUtils.contains(ingress.getName(), keyword) || StringUtils.contains(ingress.getMiddlewareName(), keyword) ||
                        StringUtils.contains(ingress.getMiddlewareNickName(), keyword) || StringUtils.contains(ingress.getExposeIP(), keyword)
                ).collect(Collectors.toList());
                ingressDTOList.addAll(ingressDTOS);
            }

            Map<String, Object> middlewareMap = new HashMap<>();
            middlewareMap.put("name", middlewareInfoDTO.getChartName());
            middlewareMap.put("image", middlewareInfoDTO.getImage());
            middlewareMap.put("imagePath", middlewareInfoDTO.getImagePath());
            middlewareMap.put("chartName", middlewareInfoDTO.getChartName());
            middlewareMap.put("chartVersion", middlewareInfoDTO.getChartVersion());
            middlewareMap.put("version", middlewareInfoDTO.getVersion());
            middlewareMap.put("ingressList", ingressDTOList);
            middlewareMap.put("serviceNum", ingressDTOList.size());
            ingressList.add(middlewareMap);
        });

        Collections.sort(ingressList, new MiddlewareServiceImpl.ServiceMapComparator());
        return ingressList;
    }

}
