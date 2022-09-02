package com.harmonycloud.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.*;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.bean.BeanIngressComponents;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.dao.BeanIngressComponentsMapper;
import com.harmonycloud.zeus.dao.BeanMiddlewareInfoMapper;
import com.harmonycloud.zeus.integration.cluster.ConfigMapWrapper;
import com.harmonycloud.zeus.integration.cluster.IngressRouteTCPWrapper;
import com.harmonycloud.zeus.integration.cluster.IngressWrapper;
import com.harmonycloud.zeus.integration.cluster.ServiceWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.MiddlewareCrTypeService;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.service.user.UserService;
import com.harmonycloud.zeus.util.DateUtil;
import com.harmonycloud.zeus.util.MiddlewareServicePurposeUtil;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.*;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.CommonConstant.NUM_ONE;
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
    private static final String INGRESS_CLASS_NAME = "kubernetes.io/ingress.class";
    private static final String NODE_PORT = "nodeport";
    private static final int RANDOM_LENGTH = 6;

    @Autowired
    private IngressWrapper ingressWrapper;

    @Autowired
    private ConfigMapWrapper configMapWrapper;

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private ServiceWrapper serviceWrapper;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private UserService userService;
    @Autowired
    private MiddlewareCrTypeService middlewareCrTypeService;
    @Autowired
    private IngressComponentService ingressComponentService;
    @Autowired
    private BeanMiddlewareInfoMapper middlewareInfoMapper;
    @Autowired
    private PodService podService;
    @Autowired
    private BeanIngressComponentsMapper beanIngressComponentsMapper;
    @Autowired
    private IngressRouteTCPWrapper ingressRouteTCPWrapper;
    @Autowired
    private NodeService nodeService;

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
        List<IngressComponentDto> ingressComponentDtoList = ingressComponentService.list(clusterId);
        if (!CollectionUtils.isEmpty(ingressComponentDtoList)) {
            for (IngressComponentDto ingress : ingressComponentDtoList) {
                if (IngressEnum.NGINX.getName().equals(ingress.getType())) {
                    if (StringUtils.isNotEmpty(ingress.getConfigMapName())) {
                        // tcp routing list
                        ConfigMap configMap =
                                configMapWrapper.get(clusterId, ingress.getNamespace(), ingress.getConfigMapName());
                        dealTcpRoutine(clusterId, namespace, configMap, ingressDtoList, ingress);
                    }
                } else {
                    Map<String, String> labels = new HashMap<>();
                    labels.put("ingressName", ingress.getName());
                    IngressRouteTCPList routeTCPList = ingressRouteTCPWrapper.list(clusterId, namespace, labels);
                    ingressDtoList.addAll(convertIngressDTOList(ingress, routeTCPList, null));
                }
            }
        }

        // nodePort routing list
        List<io.fabric8.kubernetes.api.model.Service> serviceList = serviceWrapper.list(clusterId, namespace, MIDDLEWARE_NAME);
        dealNodePortRoutineList(clusterId, namespace, serviceList, ingressDtoList);
        if (CollectionUtils.isEmpty(ingressDtoList)) {
            return ingressDtoList;
        }

        // 过滤未纳管的分区中的服务
        List<Namespace> registeredNamespace = clusterService.listRegisteredNamespace(clusterId);
        List<String> registeredNamespaceNameList = registeredNamespace.stream().map(Namespace::getName).collect(Collectors.toList());
        ingressDtoList = ingressDtoList.stream().filter(ingressDTO -> {
            return !StringUtils.isEmpty(ingressDTO.getMiddlewareName()) && registeredNamespaceNameList.contains(ingressDTO.getNamespace());
        }).collect(Collectors.toList());

        // package assembly
        for (IngressDTO ingressDTO : ingressDtoList) {
            if (StringUtils.isBlank(namespace)) {
                namespace = ingressDTO.getNamespace();
            }
            JSONObject values = helmChartService.getInstalledValues(ingressDTO.getMiddlewareName(), namespace, cluster);
            if (values == null) {
                continue;
            }
            ingressDTO.setChartVersion(values.getOrDefault("chart-version", "").toString());
            ingressDTO.setMiddlewareMode(values.getOrDefault("mode", "").toString());
            ingressDTO.setMiddlewareNickName(values.getOrDefault("aliasName", "").toString());
            setMiddlewareImage(ingressDTO);
            ingressDTO.setMiddlewareOfficialName(MiddlewareOfficialNameEnum.findByChartName(ingressDTO.getMiddlewareType()));
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
        // 判断端口是否已被使用
        if (!CollectionUtils.isEmpty(ingressDTO.getServiceList())) {
            ingressDTO.getServiceList().forEach(ingress -> {
                if (StringUtils.isNotBlank(ingress.getExposePort())) {
                    verifyServicePort(clusterId, Integer.parseInt(ingress.getExposePort()));
                }
            });
        }
        // 为rocketmq和kafka设置服务端口号
        checkAndAllocateServicePort(clusterId, ingressDTO);
        if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_INGRESS)) {
            try {
                QueryWrapper<BeanIngressComponents> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("cluster_id", ingressDTO.getClusterId());
                queryWrapper.eq("ingress_class_name", ingressDTO.getIngressClassName());
                BeanIngressComponents ingressComponents = beanIngressComponentsMapper.selectOne(queryWrapper);
                if (ingressDTO.getProtocol().equals(Protocol.HTTP.getValue())) {
                    Ingress ingress = convertK8sIngress(namespace, ingressDTO);
                    ingressWrapper.create(clusterId, namespace, ingress);
                } else if (ingressDTO.getProtocol().equals(Protocol.TCP.getValue())) {
                    if (IngressEnum.TRAEFIK.getName().equals(ingressComponents.getType())) {
                        ingressRouteTCPWrapper.benchCreate(clusterId, convertIngressRouteTCP(ingressDTO, ingressComponents.getName()));
                    } else {
                        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
                        ConfigMap configMap = covertTcpConfig(cluster, namespace, ingressDTO);
                        configMapWrapper.update(clusterId, getIngressTcpNamespace(cluster, ingressDTO.getIngressClassName()), configMap);
                    }
                }
            } catch (KubernetesClientException e) {
                log.error("创建服务暴露出错了", e);
                throw new CaasRuntimeException(ErrorMessage.INGRESS_DOMAIN_NAME_FORMAT_NOT_SUPPORT);
            }
        } else if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_NODEPORT)) {
            List<io.fabric8.kubernetes.api.model.Service> serviceList;
            if (mqCheck(ingressDTO)) {
                List<ServiceDTO> dtoList = ingressDTO.getServiceList().stream().filter(item -> {
                    String serviceName = item.getServiceName();
                    return serviceName.contains("proxy") || serviceName.contains("kafka-external-svc");
                }).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(dtoList)) {
                    serviceList = covertMQNodePortService(namespace, middlewareName, ingressDTO);
                } else {
                    serviceList = covertNodePortService(clusterId, namespace, middlewareName, ingressDTO);
                }
            } else {
                serviceList = covertNodePortService(clusterId, namespace, middlewareName, ingressDTO);
            }
            if (CollectionUtils.isEmpty(serviceList)) {
                throw new CaasRuntimeException(ErrorMessage.INGRESS_NODEPORT_NOT_NULL);
            }
            serviceWrapper.batchCreate(clusterId, namespace, serviceList);
        } else {
            throw new CaasRuntimeException(ErrorMessage.UNSUPPORT_EXPOSE_TYPE);
        }
        // 特殊处理kafka和rocketmq(仅更新端口时)
        if (mqCheck(ingressDTO) && ingressDTO.getServiceList() != null && checkExternalService(ingressDTO)) {
            upgradeValues(clusterId, namespace, middlewareName, ingressDTO);
        }
    }

    @Override
    public void checkServiceTcpPort(MiddlewareClusterDTO cluster, List<ServiceDTO> serviceList) {
        if (CollectionUtils.isEmpty(serviceList)) {
            return;
        }
        // 校验NodePort端口是否已存在
        List<io.fabric8.kubernetes.api.model.Service> svcList = serviceWrapper.list(cluster.getId(), null);
        HashSet<Integer> nodePorts = new HashSet<>();
        if (!CollectionUtils.isEmpty(svcList)) {
            svcList.forEach(service -> {
                if (MIDDLEWARE_EXPOSE_NODEPORT.equals(service.getSpec().getType())) {
                    service.getSpec().getPorts().forEach(nodePortSvc -> {
                        nodePorts.add(nodePortSvc.getNodePort());
                    });
                }
            });
        }
        serviceList.forEach(serviceDTO -> {
            if (nodePorts.contains(Integer.parseInt(serviceDTO.getExposePort()))) {
                throw new BusinessException(ErrorMessage.TCP_PORT_ALREADY_USED);
            }
        });

        // 校验Nginx TCP配置文件
        List<IngressComponentDto> nginxComponentDtoList = ingressComponentService.list(cluster.getId(), IngressEnum.NGINX.getName());
        nginxComponentDtoList.forEach(ingress -> {
            String ingressTcpCmName = ingress.getConfigMapName();
            ConfigMap configMap = configMapWrapper.get(cluster.getId(),
                    getIngressTcpNamespace(cluster, ingress.getIngressClassName()), ingressTcpCmName);
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
        });
        // 校验traefik additionalArguments配置
        Set<String> portSet = new HashSet<>();
        List<IngressComponentDto> traefikComponentDtoList = ingressComponentService.list(cluster.getId(), IngressEnum.TRAEFIK.getName());
        for (IngressComponentDto ingress : traefikComponentDtoList) {
            JSONObject installedValues = null;
            try {
                installedValues = helmChartService.getInstalledValues(ingress.getIngressClassName(), ingress.getNamespace(), clusterService.findById(ingress.getClusterId()));
            } catch (Exception e) {
                log.error("查询traefik values失败了", e);
                continue;
            }
            if (installedValues == null) {
                continue;
            }
            JSONArray additionalArguments = installedValues.getJSONArray("additionalArguments");
            additionalArguments.forEach(arg -> {
                String[] strs = arg.toString().split(":");
                if (strs.length == 2) {
                    portSet.add(strs[1]);
                }
            });
        }
        if (!CollectionUtils.isEmpty(portSet)) {
            serviceList.forEach(serviceDTO -> {
                if (portSet.contains(Integer.parseInt(serviceDTO.getExposePort()))) {
                    throw new BusinessException(ErrorMessage.TCP_PORT_ALREADY_USED);
                }
            });
        }
    }

    @Override
    public Set<Integer> getUsedPortSet(MiddlewareClusterDTO cluster) {
        Set<Integer> portSet = new HashSet<>();
        // 查询NodePort端口
        List<io.fabric8.kubernetes.api.model.Service> svcList = serviceWrapper.list(cluster.getId(), null);
        HashSet<Integer> nodePorts = new HashSet<>();
        if (!CollectionUtils.isEmpty(svcList)) {
            svcList.forEach(service -> {
                if (MIDDLEWARE_EXPOSE_NODEPORT.equals(service.getSpec().getType())) {
                    service.getSpec().getPorts().forEach(nodePortSvc -> {
                        nodePorts.add(nodePortSvc.getNodePort());
                    });
                }
            });
        }
        portSet.addAll(nodePorts);

        // 查询Nginx TCP已配置端口
        List<IngressComponentDto> nginxComponentDtoList = ingressComponentService.list(cluster.getId(), IngressEnum.NGINX.getName());
        for (IngressComponentDto ingress : nginxComponentDtoList) {
            String ingressTcpCmName = ingress.getConfigMapName();
            ConfigMap configMap = configMapWrapper.get(cluster.getId(),
                    getIngressTcpNamespace(cluster, ingress.getIngressClassName()), ingressTcpCmName);
            if (configMap == null || CollectionUtils.isEmpty(configMap.getData())) {
                continue;
            }
            configMap.getData().keySet().forEach(port -> {
                portSet.add(Integer.parseInt(port));
            });
        }
        // 查询traefik 端口
        List<IngressComponentDto> traefikComponentDtoList = ingressComponentService.list(cluster.getId(), IngressEnum.TRAEFIK.getName());
        for (IngressComponentDto ingress : traefikComponentDtoList) {
            JSONObject installedValues = helmChartService.getInstalledValues(ingress.getIngressClassName(), ingress.getNamespace(), clusterService.findById(ingress.getClusterId()));
            if (installedValues == null) {
                continue;
            }
            JSONArray additionalArguments = installedValues.getJSONArray("additionalArguments");
            additionalArguments.forEach(arg -> {
                String[] strs = arg.toString().split(":");
                if (strs.length == 2) {
                    portSet.add(Integer.parseInt(strs[1]));
                }
            });
        }
        return portSet;
    }

    @Override
    public void createIngressTcp(MiddlewareClusterDTO cluster, String namespace, List<ServiceDTO> serviceList,
                                 boolean checkPort) {
        if (CollectionUtils.isEmpty(serviceList)) {
            return;
        }
        if (checkPort) {
            checkServiceTcpPort(cluster, serviceList);
        }
        // 转换Ingress TCP配置文件
        IngressDTO ingressDTO = new IngressDTO();
        ingressDTO.setServiceList(serviceList);
        ConfigMap configMap = covertTcpConfig(cluster, namespace, ingressDTO);
        // 更新配置文件
        configMapWrapper.update(cluster.getId(), getIngressTcpNamespace(cluster, null), configMap);
    }

    @Override
    public void delete(String clusterId, String namespace, String middlewareName, String name, IngressDTO ingressDTO) {
        if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_INGRESS)) {
            if (ingressDTO.getProtocol().equals(Protocol.HTTP.getValue())) {
                ingressWrapper.delete(clusterId, namespace, name);
            } else if (ingressDTO.getProtocol().equals(Protocol.TCP.getValue())) {
                MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
                IngressComponentDto ingressComponentDto =
                        ingressComponentService.get(clusterId, ingressDTO.getIngressClassName());
                if (ingressComponentDto != null) {
                    if (IngressEnum.NGINX.getName().equals(ingressComponentDto.getType())) {
                        if (StringUtils.isEmpty(ingressComponentDto.getConfigMapName())) {
                            return;
                        }
                        ConfigMap configMap = configMapWrapper.get(clusterId,
                                getIngressTcpNamespace(cluster, ingressDTO.getIngressClassName()),
                                ingressComponentDto.getConfigMapName());
                        removeTcpPort(configMap, ingressDTO.getServiceList());
                        configMapWrapper.update(clusterId,
                                getIngressTcpNamespace(cluster, ingressDTO.getIngressClassName()), configMap);
                    } else if (IngressEnum.TRAEFIK.getName().equals(ingressComponentDto.getType())) {
                        ingressRouteTCPWrapper.delete(clusterId, namespace, name);
                    }
                }
            }
        } else if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_NODEPORT)) {
            serviceWrapper.delete(clusterId, namespace, name);
        }
    }

    @Override
    public void delete(String clusterId, String namespace, String type, String middlewareName) {
        List<IngressDTO> ingressList;
        try {
            ingressList = this.get(clusterId, namespace, type, middlewareName);
        } catch (Exception e) {
            log.error("集群：{}，命名空间：{}，中间件：{}/{}，删除对外访问时查询列表异常", clusterId, namespace, type, middlewareName, e);
            return;
        }
        ingressList.forEach(ing -> {
            try {
                this.delete(clusterId, namespace, middlewareName, ing.getName(), ing);
            } catch (Exception e) {
                log.error("集群：{}，命名空间：{}，中间件：{}/{}，对外服务{}/{}，删除对外访问异常", clusterId, namespace, type, middlewareName,
                        ing.getExposeType(), ing.getName(), e);
            }
        });
    }

    @Override
    public List<IngressDTO> get(String clusterId, String namespace, String type, String middlewareName) {
        MiddlewareCR crd = middlewareCRService.getCR(clusterId, namespace, type, middlewareName);
        if (crd == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middlewareName, ErrorMessage.NOT_EXIST);
        }
        if (crd.getStatus() == null) {
            return new ArrayList<>(0);
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
                        || ing.getMetadata().getName().length() != baseIngressName.length() + 1 + RANDOM_LENGTH) {
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
                    if (!svc.getMetadata().getName().contains(middlewareName) || !svc.getMetadata().getName().contains("nodeport") || svc.getMetadata().getLabels() == null || !svc.getMetadata().getLabels().containsKey(MIDDLEWARE_NAME) || !svc.getMetadata().getLabels().containsValue(middlewareName)) {
                        return;
                    }
                    IngressDTO dto = dealNodePortRoutine(clusterId, namespace, cluster.getHost(), svc);
                    if (dto != null) {
                        resList.add(dto);
                    }
                });
            }
            // ingress tcp
            List<IngressComponentDto> ingressComponentDtoList = ingressComponentService.list(clusterId);
            if (!CollectionUtils.isEmpty(ingressComponentDtoList)) {
                for (IngressComponentDto ingress : ingressComponentDtoList) {
                    if (IngressEnum.NGINX.getName().equals(ingress.getType())) {
                        if (StringUtils.isNotEmpty(ingress.getConfigMapName())) {
                            JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);
                            String middlewareAliasName = values.getOrDefault("aliasName", "").toString();
                            ConfigMap configMap = configMapWrapper.get(clusterId,
                                    getIngressTcpNamespace(cluster, ingress.getIngressClassName()),
                                    ingress.getConfigMapName());
                            Map<String, List<ServiceDTO>> tcpRoutineMap = getTcpRoutineMap(configMap);
                            svcNameList.forEach(svcName -> {
                                List<IngressDTO> tcpDtos = getTcpRoutineDetail(clusterId, namespace, crd, svcName, tcpRoutineMap);
                                resList.addAll(convertIngressDTOList(tcpDtos, ingress, type, middlewareAliasName));
                            });
                        }
                    } else if (IngressEnum.TRAEFIK.getName().equals(ingress.getType())) {
                        IngressRouteTCPList routeTCPList = ingressRouteTCPWrapper.list(clusterId, namespace,
                                getIngressTCPLabels(middlewareName, type, ingress.getName()));
                        resList.addAll(convertIngressDTOList(ingress, routeTCPList, null));
                    }
                }
            }
        }
        // 添加ingress pod信息
        setIngressExtralInfo(clusterId, resList);
        // 设置图片
        setMiddlewareImage(clusterId, namespace, type, middlewareName, resList);
        // 特殊处理rocketmq和kafka
        if ("rocketmq".equals(type) || "kafka".equals(type)) {
            setExternalServiceExposeStatus(type, resList);
        }
        List<IngressDTO> ingressDTOList = resList.stream().
                filter(ingressDTO -> ingressDTO.getServicePurpose() != null && !"null".equals(ingressDTO.getServicePurpose())).
                collect(Collectors.toList());
        return ingressDTOList;
    }

    @Override
    public IngressDTO get(String clusterId, String namespace, String type, String middlewareName, String name, String exposeType, String protocol) {
        /*if (StringUtils.equals(exposeType, MIDDLEWARE_EXPOSE_INGRESS)) {
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
        }*/

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

    @Override
    public void verifyServicePort(String clusterId, Integer port) {
        MiddlewareClusterDTO clusterDTO = clusterService.findById(clusterId);
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setExposePort(String.valueOf(port));
        List<ServiceDTO> serviceDTOList = new ArrayList<>();
        serviceDTOList.add(serviceDTO);
        checkServiceTcpPort(clusterDTO, serviceDTOList);
    }

    @Override
    public Set<String> listIngressIp(String clusterId, String ingressClassName) {
        IngressComponentDto ingressComponentDto = ingressComponentService.get(clusterId, ingressClassName);
        if (ingressComponentDto == null) {
            return Collections.emptySet();
        }
        List<PodInfo> podInfoList;
        Set<String> ingressPodIpSet = new HashSet<>();
        if (StringUtils.isNotBlank(ingressComponentDto.getAddress())) {
            ingressPodIpSet.add(ingressComponentDto.getAddress());
        } else {
            podInfoList = listIngressPod(clusterId, ingressComponentDto.getNamespace(), ingressComponentDto.getName());
            podInfoList = podInfoList.stream().filter(podInfo -> "Running".equals(podInfo.getStatus())
                    && StringUtils.isNotBlank(podInfo.getHostIp())).collect(Collectors.toList());
            podInfoList.forEach(podInfo -> {
                ingressPodIpSet.add(podInfo.getHostIp());
            });
        }
        return ingressPodIpSet;
    }

    /**
     * 检查是否是消息队列
     * @param ingressDTO
     * @return
     */
    private boolean mqCheck(IngressDTO ingressDTO) {
        return ingressDTO.getMiddlewareType().equals(MiddlewareTypeEnum.ROCKET_MQ.getType())
                || ingressDTO.getMiddlewareType().equals(MiddlewareTypeEnum.KAFKA.getType());
    }

    /**
     * 获取中间件ingressroutetcp label
     *
     * @param middlewareName
     * @return
     */
    private Map<String, String> getIngressTCPLabels(String middlewareName, String type, String ingressName) {
        Map<String, String> labels = new HashMap<>(1);
        labels.put("middlewareName", middlewareName);
        labels.put("middlewareType", type);
        labels.put("ingressName", ingressName);
        return labels;
    }

    /**
     * 设置中间件图片
     *
     * @param ingressDTO
     */
    private void setMiddlewareImage(IngressDTO ingressDTO) {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("chart_version", ingressDTO.getChartVersion());
        wrapper.eq("chart_name", ingressDTO.getMiddlewareType());
        BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoMapper.selectOne(wrapper);
        if (beanMiddlewareInfo == null) {
            wrapper = new QueryWrapper<>();
            wrapper.eq("chart_name", ingressDTO.getMiddlewareType());
            List<BeanMiddlewareInfo> middlewareInfoList = middlewareInfoMapper.selectList(wrapper);
            if (!CollectionUtils.isEmpty(middlewareInfoList)) {
                beanMiddlewareInfo = middlewareInfoList.get(0);
            }
        }
        if (beanMiddlewareInfo != null) {
            ingressDTO.setImagePath(beanMiddlewareInfo.getImagePath());
        }
    }

    /**
     * 添加ingress 其他信息
     *
     * @param clusterId
     * @param ingressDTOS
     */
    public void setIngressExtralInfo(String clusterId, List<IngressDTO> ingressDTOS) {
        ingressDTOS.forEach(ingressDTO -> {
            if (StringUtils.isNotEmpty(ingressDTO.getIngressClassName())) {
                // 设置ingress pod
                ingressDTO.setIngressIpSet(listIngressIp(clusterId, ingressDTO.getIngressClassName()));
            }
            // 设置服务暴露的网络模型 4层或7层
            setServiceNetworkModel(ingressDTO);
            // 设置服务用途
            ingressDTO.setServicePurpose(MiddlewareServicePurposeUtil.convertChinesePurpose(ingressDTO));
        });
    }

    /**
     * 设置ingress额外信息
     *
     * @param ingressDTO
     */
    public void setServiceNetworkModel(IngressDTO ingressDTO) {
        if (MIDDLEWARE_EXPOSE_INGRESS.equals(ingressDTO.getExposeType())) {
            if (ingressDTO.getProtocol().equals(Protocol.HTTP.getValue())) {
                ingressDTO.setNetworkModel(7);
            } else if (ingressDTO.getProtocol().equals(Protocol.TCP.getValue())) {
                ingressDTO.setNetworkModel(4);
            }
        } else if (MIDDLEWARE_EXPOSE_NODEPORT.equals(ingressDTO.getExposeType())) {
            ingressDTO.setNetworkModel(4);
        } else {
            ingressDTO.setNetworkModel(0);
        }
    }

    /**
     * 设置中间件图片
     *
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param type
     * @param ingressDTOS
     */
    private void setMiddlewareImage(String clusterId, String namespace, String type, String middlewareName, List<IngressDTO> ingressDTOS) {
        JSONObject installedValues = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        QueryWrapper<BeanMiddlewareInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chart_version", installedValues.getString("chart-version"));
        queryWrapper.eq("chart_name", type);
        BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoMapper.selectOne(queryWrapper);
        if (beanMiddlewareInfo != null) {
            ingressDTOS.forEach(ingressDTO -> {
                ingressDTO.setImagePath(beanMiddlewareInfo.getImagePath());
            });
        }
    }

    /**
     * 设置集群外服务暴露状态
     *
     * @param type
     * @param ingressDTOS
     */
    private void setExternalServiceExposeStatus(String type, List<IngressDTO> ingressDTOS) {
        AtomicBoolean enableExternal = new AtomicBoolean(false);
        ingressDTOS.forEach(ingressDTO -> {
            switch (type) {
                case "rocketmq":
                    if (ingressDTO.getName().contains("nameserver-proxy-svc")) {
                        enableExternal.set(true);
                    }
                    break;
                case "kafka":
                    if (ingressDTO.getName().contains("external")) {
                        enableExternal.set(true);
                    }
                    break;
                default:
            }
        });
        if (enableExternal.get()) {
            ingressDTOS.forEach(ingressDTO -> {
                ingressDTO.setExternalEnable(true);
            });
        }
    }

    public List<PodInfo> listIngressPod(String clusterId, String namespace, String ingressClassName) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/instance", ingressClassName);
        return podService.list(clusterId, namespace, labels);
    }

    /**
     * 获取一个ingress node ip
     *
     * @param clusterId
     * @param namespace
     * @param ingressClassName
     * @return
     */
    public String getIngressNodeIp(String clusterId, String namespace, String ingressClassName) {
        List<PodInfo> podInfos = podService.list(clusterId, namespace, ingressClassName);
        if (!CollectionUtils.isEmpty(podInfos)) {
            return podInfos.get(0).getHostIp();
        }
        return "";
    }

    /**
     * 当为kafka或rockeymq暴露服务时，若用户未设置服务端口号，则为服务随机分配端口号
     *
     * @param clusterId
     * @param ingressDTO
     */
    private void checkAndAllocateServicePort(String clusterId, IngressDTO ingressDTO) {
        if ("rocketmq".equals(ingressDTO.getMiddlewareType()) || "kafka".equals(ingressDTO.getMiddlewareType())) {
            List<ServiceDTO> serviceList = ingressDTO.getServiceList();
            if (CollectionUtils.isEmpty(serviceList)) {
                return;
            }
            for (int i = 0; i < serviceList.size(); i++) {
                ServiceDTO serviceDTO = serviceList.get(i);
                setServicePort(serviceDTO, ingressDTO.getMiddlewareType());
                if (!checkExternalService(serviceDTO)) {
                    continue;
                }
                if (StringUtils.isBlank(serviceDTO.getExposePort()) && !serviceDTO.getServiceName().contains("proxy")) {
                    serviceDTO.setExposePort(String.valueOf(getAvailablePort(clusterId)));
                }
            }
        }
    }

    /**
     * 检查是否是集群外访问相关服务
     *
     * @param serviceDTO
     * @return
     */
    private boolean checkExternalService(ServiceDTO serviceDTO) {
        if (serviceDTO.getServiceName().contains("console") || serviceDTO.getServiceName().contains("kibana")
                || serviceDTO.getServiceName().contains("proxy") || serviceDTO.getServiceName().contains("namesrv")) {
            return false;
        }
        return true;
    }

    /**
     * 检查是否是集群外访问相关服务
     *
     * @param ingressDTO
     * @return
     */
    private boolean checkExternalService(IngressDTO ingressDTO) {
        if (CollectionUtils.isEmpty(ingressDTO.getServiceList())) {
            return false;
        }
        for (ServiceDTO serviceDTO : ingressDTO.getServiceList()) {
            if (serviceDTO.getServiceName().contains("master") || serviceDTO.getServiceName().contains("slave")
                    || serviceDTO.getServiceName().contains("broker") || serviceDTO.getServiceName().contains("external-svc")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取一组可用的端口号
     *
     * @param clusterId
     * @param portNum
     * @return
     */
    private List<Integer> getAvailablePort(String clusterId, int portNum) {
        int startPort = 30000 + new Random().nextInt(100);
        List<Integer> portList = new ArrayList<>();
        for (int i = 0; i < portNum; i++) {
            try {
                verifyServicePort(clusterId, startPort);
                portList.add(startPort);
                startPort++;
            } catch (Exception e) {
                log.error("出错了", e);
                startPort++;
                i--;
            }
        }
        return portList;
    }

    /**
     * 获取1个可用的端口号
     *
     * @param clusterId
     * @return
     */
    private Integer getAvailablePort(String clusterId) {
        return getAvailablePort(clusterId, 1).get(0);
    }

    /**
     * 设置服务端口
     *
     * @param serviceDTO
     * @param middlewareType
     */
    private void setServicePort(ServiceDTO serviceDTO, String middlewareType) {
        String serviceName = serviceDTO.getServiceName();
        if (StringUtils.isNotBlank(serviceDTO.getServicePort())) {
            return;
        }
        if ("rocketmq".equals(middlewareType)) {
            if (serviceName.endsWith("nameserver-proxy-svc")) {
                serviceDTO.setServicePort("9876");
                serviceDTO.setTargetPort("9876");
            } else {
                serviceDTO.setServicePort("10911");
                serviceDTO.setTargetPort("10911");
            }
        } else if ("kafka".equals(middlewareType)) {
            serviceDTO.setServicePort("9094");
            serviceDTO.setTargetPort("9094");
        }
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
            objectMeta.setName(getNodePortSvcName(serviceName, ingressDTO.getName()));
            objectMeta.setNamespace(namespace);
            //取原services labels
            Map<String, String> labels = new HashMap<>();
            if (!CollectionUtils.isEmpty(serviceOriginal.getMetadata().getLabels())) {
                labels.putAll(serviceOriginal.getMetadata().getLabels());
            }
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

    private List<io.fabric8.kubernetes.api.model.Service> covertMQNodePortService(String namespace, String middlewareName,IngressDTO ingressDTO) {
        List<ServiceDTO> serviceDTOList = ingressDTO.getServiceList();
        if (CollectionUtils.isEmpty(serviceDTOList)) {
            return null;
        }
        List<io.fabric8.kubernetes.api.model.Service> serviceList = new ArrayList<>(10);

        if ("rocketmq".equals(ingressDTO.getMiddlewareType())) {
            serviceDTOList = serviceDTOList.stream().filter(serviceDTO ->
                    serviceDTO.getServiceName().contains("nameserver-proxy-svc")).collect(Collectors.toList());
        } else if ("kafka".equals(ingressDTO.getMiddlewareType())) {
            serviceDTOList = serviceDTOList.stream().filter(serviceDTO ->
                    serviceDTO.getServiceName().contains("kafka-external-svc")).collect(Collectors.toList());
        }
        for (ServiceDTO serviceDTO : serviceDTOList) {
            String serviceName = serviceDTO.getServiceName();
            if (StringUtils.isBlank(serviceName)) {
                continue;
            }

            io.fabric8.kubernetes.api.model.Service service = new io.fabric8.kubernetes.api.model.Service();
            ObjectMeta objectMeta = new ObjectMeta();
            objectMeta.setName(getNodePortSvcName(serviceName, ingressDTO.getName()));
            objectMeta.setNamespace(namespace);
            //取原services labels
            Map<String, String> labels = getMQServiceLabels(middlewareName, ingressDTO.getMiddlewareType(), serviceName);
            labels.put(MIDDLEWARE_NAME, middlewareName);
            labels.put(MIDDLEWARE_TYPE, ingressDTO.getMiddlewareType());
            objectMeta.setLabels(labels);
            service.setMetadata(objectMeta);
            ServiceSpec spec = new ServiceSpec();

            List<ServicePort> servicePortList = new ArrayList<>();
            servicePortList.add(covertMQServicePort(serviceDTO, ingressDTO.getMiddlewareType()));
            spec.setPorts(servicePortList);
            spec.setSelector(getMQSelector(middlewareName, ingressDTO.getMiddlewareType(), serviceName));
            spec.setType(MIDDLEWARE_EXPOSE_NODEPORT);
            service.setSpec(spec);
            serviceList.add(service);
        }
        return serviceList;
    }

    private Map<String, String> getMQServiceLabels(String middlewareName, String type, String serviceName) {
        Map<String, String> labels = new HashMap<>();
        if ("rocketmq".equals(type)) {
            String value = middlewareName + "namesrv-proxy-svc-" + serviceName.substring(serviceName.lastIndexOf("-") + 1);
            labels.put("app", value);
        } else if ("kafka".equals(type)) {
            String value = middlewareName + "-kafka-external-svc";
            labels.put("app", value);
        }
        return labels;
    }

    private Map<String, String> getMQSelector(String middlewareName, String type, String serviceName) {
        Map<String, String> selector = new HashMap<>();
        if ("rocketmq".equals(type)) {
            String value = middlewareName + "namesrv-proxy-" + serviceName.substring(serviceName.lastIndexOf("-") + 1);
            selector.put("statefulset.kubernetes.io/pod-name", value);
        } else if ("kafka".equals(type)) {
            String value = "kafka-" + serviceName.substring(serviceName.lastIndexOf("-") + 1);
            selector.put("podIndex", value);
            selector.put("app", middlewareName);
        }
        return selector;
    }

    private String getNodePortSvcName(String serviceName) {
        return serviceName + "-" + NODE_PORT + "-" + UUIDUtils.get8UUID().substring(0, 6);
    }

    private String getNodePortSvcName(String serviceName, String nodePortName) {
        log.info("获取服务 {} nodeport的名称 {}", serviceName, nodePortName);
        if (StringUtils.isNotEmpty(nodePortName)) {
            return nodePortName;
        }
        return serviceName + "-" + NODE_PORT + "-" + UUIDUtils.get8UUID().substring(0, 6);
    }

    private ServicePort covertServicePort(ServiceDTO serviceDTO) {
        ServicePort servicePort = new ServicePort();
        if (StringUtils.isNotEmpty(serviceDTO.getExposePort())) {
            servicePort.setNodePort(Integer.parseInt(serviceDTO.getExposePort()));
        }
        servicePort.setProtocol(Protocol.TCP.getValue());
        servicePort.setPort(Integer.parseInt(serviceDTO.getServicePort()));

        IntOrString intOrString = new IntOrString();
        intOrString.setIntVal(Integer.parseInt(serviceDTO.getTargetPort()));
        servicePort.setTargetPort(intOrString);

        return servicePort;
    }

    private ServicePort covertMQServicePort(ServiceDTO serviceDTO, String type) {
        ServicePort servicePort = new ServicePort();
        if (StringUtils.isNotEmpty(serviceDTO.getExposePort())) {
            servicePort.setNodePort(Integer.parseInt(serviceDTO.getExposePort()));
        }
        servicePort.setProtocol(Protocol.TCP.getValue());
        if ("rocketmq".equals(type)) {
            servicePort.setPort(9876);
            servicePort.setTargetPort(new IntOrString(9876));
        } else {
            servicePort.setPort(9094);
            servicePort.setTargetPort(new IntOrString(9094));
        }
        return servicePort;
    }

    private void dealNodePortRoutineList(String clusterId, String namespace, List<io.fabric8.kubernetes.api.model.Service> serviceList, List<IngressDTO> ingressDtoList) {
        if (CollectionUtils.isEmpty(serviceList)) {
            return;
        }

        MiddlewareClusterDTO middlewareCluster = clusterService.findById(clusterId);

        String exposeIP = middlewareCluster.getHost();
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
        ingressDTO.setNamespace(service.getMetadata().getNamespace());
        ingressDTO.setClusterId(clusterId);
        ingressDTO.setName(service.getMetadata().getName());
        ingressDTO.setExposeIP(exposeIP);
        ingressDTO.setExposeType(MIDDLEWARE_EXPOSE_NODEPORT);
        ingressDTO.setProtocol(Protocol.TCP.getValue());
        ingressDTO.setLabels(service.getMetadata().getLabels());
        ingressDTO.setCreateTime(DateUtil.utc2Local(service.getMetadata().getCreationTimestamp(),
                DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue()));
        List<OwnerReference> ownerReferences = service.getMetadata().getOwnerReferences();
        if (!CollectionUtils.isEmpty(ownerReferences)) {
            ingressDTO.setOwnerReferences(ownerReferences);
        }

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

    private List<IngressDTO> getTcpRoutineDetail(String clusterId, String namespace, MiddlewareCR crd,
                                                 String svcName, Map<String, List<ServiceDTO>> tcpRoutineMap) {
        String nsSvcName = namespace + "/" + svcName;
        List<ServiceDTO> svcDtoList = tcpRoutineMap.get(nsSvcName);
        // 没有匹配的
        if (CollectionUtils.isEmpty(svcDtoList)) {
            return Collections.emptyList();
        }
        List<IngressDTO> ingressDTOList = new ArrayList<>();
        MiddlewareClusterDTO middlewareClusterDTO = clusterService.findById(clusterId);
        svcDtoList.forEach(svcDto -> {
            List<ServiceDTO> serviceList = new ArrayList<>(1);
            serviceList.add(svcDto);
            IngressDTO ingressDTO = new IngressDTO()
                    .setMiddlewareName(crd.getSpec().getName())
                    .setClusterId(clusterId)
                    .setClusterNickname(middlewareClusterDTO.getNickname())
                    .setName(getIngressTcpName(svcDtoList.get(0).getServiceName(), namespace))
                    .setNamespace(namespace)
                    .setExposeType(MIDDLEWARE_EXPOSE_INGRESS)
                    .setProtocol(Protocol.TCP.getValue())
                    .setServiceList(serviceList);
            ingressDTOList.add(ingressDTO);
        });
        return ingressDTOList;
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
     * @param ingressDTO
     * @return
     */
    private ConfigMap covertTcpConfig(MiddlewareClusterDTO cluster, String namespace, IngressDTO ingressDTO) {
        if (CollectionUtils.isEmpty(ingressDTO.getServiceList())) {
            throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_NOT_NULL);
        }
        // 获取指定的ingress tcpCmName
        IngressComponentDto ingressComponentDto =
                ingressComponentService.get(cluster.getId(), ingressDTO.getIngressClassName());
        if (ingressComponentDto == null) {
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }
        String ingressTcpCmName = ingressComponentDto.getConfigMapName();
        String ingressTcpNamespace = ingressComponentDto.getNamespace();

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
        for (ServiceDTO serviceDTO : ingressDTO.getServiceList()) {
            if (StringUtils.isBlank(serviceDTO.getExposePort())) {
                throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_PORT_NOT_NULL);
            }

            if (StringUtils.isNotEmpty(ingressDTO.getName()) && StringUtils.isEmpty(serviceDTO.getOldExposePort())) {
                throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_OLD_PORT_NOT_NULL);
            }

            checkExposePort(serviceDTO, ingressDTO, data);

            if (StringUtils.isNotEmpty(ingressDTO.getName())) {
                String oldServiceInfo = namespace + "/" + serviceDTO.getOldServiceName() + ":" + serviceDTO.getOldServicePort();
                data.remove(serviceDTO.getOldExposePort(), oldServiceInfo);
            }
            String serviceInfo = namespace + "/" + serviceDTO.getServiceName() + ":" + serviceDTO.getServicePort();
            data.put(serviceDTO.getExposePort(), serviceInfo);
        }
        configMap.setData(data);
        return configMap;
    }


    private List<IngressRouteTCPCR> convertIngressRouteTCP(IngressDTO ingressDTO, String ingressName) {
        if (CollectionUtils.isEmpty(ingressDTO.getServiceList())) {
            throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_NOT_NULL);
        }
        List<IngressRouteTCPCR> routeTCPCRList = new ArrayList<>();
        for (ServiceDTO serviceDTO : ingressDTO.getServiceList()) {
            String ingressRouteTCPName = serviceDTO.getServiceName() + "-tcp-" + UUIDUtils.get8UUID();
            if (StringUtils.isNotBlank(ingressDTO.getName())) {
                ingressRouteTCPName = ingressDTO.getName();
            }
            Map<String, String> labels = getIngressTCPLabels(ingressDTO.getMiddlewareName(), ingressDTO.getMiddlewareType(), ingressName);
            IngressRouteTCPCR ingressRouteTCPCR = new IngressRouteTCPCR(ingressRouteTCPName, ingressDTO.getNamespace(),
                    ingressName + "-p" + serviceDTO.getExposePort(), serviceDTO.getServiceName(), Integer.parseInt(serviceDTO.getServicePort()), labels);
            routeTCPCRList.add(ingressRouteTCPCR);
        }
        return routeTCPCRList;
    }

    /**
     * 检查ingress tcp端口是否冲突
     *
     * @param serviceDTO
     * @param ingressDTO
     * @param data
     */
    private void checkExposePort(ServiceDTO serviceDTO, IngressDTO ingressDTO, Map<String, String> data) {
        if ((StringUtils.isNotEmpty(ingressDTO.getName()) && serviceDTO.getExposePort().equals(serviceDTO.getOldExposePort()))) {
            return;
        }
        if (StringUtils.isNotBlank(data.get(serviceDTO.getExposePort()))) {
            throw new BusinessException(ErrorMessage.INGRESS_TCP_PORT_EXIST);
        }
    }

    /**
     * tcp configMap transform to IngressDTO list
     *
     * @param clusterId
     * @param namespace
     * @param configMap
     * @param ingressDtoList
     */
    private void dealTcpRoutine(String clusterId, String namespace, ConfigMap configMap, List<IngressDTO> ingressDtoList, IngressComponentDto ingress) {
        if (configMap == null) {
            return;
        }

        if (configMap.getData() == null || configMap.getData().isEmpty()) {
            return;
        }
        Map<String, String> data = configMap.getData();

        List<MiddlewareCR> middlewareList = middlewareCRService.listCR(clusterId, namespace, null);
        if (CollectionUtils.isEmpty(middlewareList)) {
            return;
        }
        Map<String, Map<String, String>> mapHashMap = new HashMap<>(10);
        for (MiddlewareCR middleware : middlewareList) {
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
                map.put("type", middlewareCrTypeService.findTypeByCrType(middleware.getSpec().getType()));
                mapHashMap.put(middlewareInfo.getName(), map);
            }
        }

        MiddlewareClusterDTO middlewareCluster = clusterService.findById(clusterId);
        Map<String, IngressDTO> map = new HashMap<>(10);
        List<IngressDTO> middlewareIngressList = new ArrayList<>();
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
            if (StringUtils.isNotEmpty(namespace) && !StringUtils.equals(serviceNames[0], namespace)) {
                continue;
            }
            serviceDTO.setServiceName(serviceNames[1]);

            IngressDTO ingressDTO = new IngressDTO();
            ingressDTO.setClusterId(clusterId);
            ingressDTO.setNamespace(serviceNames[0]);
            List<ServiceDTO> list = new ArrayList<>(1);
            list.add(serviceDTO);
            ingressDTO.setServiceList(list);
            ingressDTO.setProtocol(Protocol.TCP.getValue());
            ingressDTO.setName(getIngressTcpName(serviceNames[1], serviceNames[0]));
            ingressDTO.setExposeIP(ingress.getAddress() == null ? middlewareCluster.getHost() : ingress.getAddress());
            ingressDTO.setExposeType(MIDDLEWARE_EXPOSE_INGRESS);
            ingressDTO.setIngressClassName(ingress.getIngressClassName());
            Map<String, String> stringStringMap = mapHashMap.get(serviceNames[1]);
            if (stringStringMap != null) {
                ingressDTO.setMiddlewareType(stringStringMap.get("type"));
                ingressDTO.setMiddlewareName(stringStringMap.get("name"));
            }
            middlewareIngressList.add(ingressDTO);
        }

        ingressDtoList.addAll(middlewareIngressList);
    }


    /**
     * IngressDTO convert to Ingress（k8s）
     *
     * @param ingressDTO
     * @return
     */
    private Ingress convertK8sIngress(String namespace, IngressDTO ingressDTO) {
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
        String ingressClassName = ingressDTO.getIngressClassName();
        annotations.put("kubernetes.io/ingress.class",
                StringUtils.isBlank(ingressClassName) ? defaultIngressName : ingressClassName);
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

        if (ingressDTO.getProtocol().equals(Protocol.HTTPS.getValue())) {
            List<IngressTLS> tls = new ArrayList<>(1);
            IngressTLS ingressTls = new IngressTLS();
            tls.add(ingressTls);
            spec.setTls(tls);
        }
        ingress.setSpec(spec);

        return ingress;
    }

    private List<IngressDTO> convertIngressDTOList(List<IngressDTO> ingressDTOS, IngressComponentDto ingress, String type, String aliasName) {
        if (CollectionUtils.isEmpty(ingressDTOS)) {
            return Collections.emptyList();
        }
        List<IngressDTO> resList = new ArrayList<>(ingressDTOS.size());
        ingressDTOS.forEach(item -> {
            item.setIngressClassName(ingress.getIngressClassName());
            resList.add(item.setMiddlewareType(type)
                    .setMiddlewareNickName(aliasName)
                    .setExposeIP(ingress.getAddress()));
        });
        return resList;
    }

    private List<IngressDTO> convertIngressDTOList(IngressComponentDto ingressDTO, IngressRouteTCPList ingressRouteTCPList, String aliasName) {
        List<IngressDTO> ingressDTOList = new ArrayList<>();
        String address;
        if (StringUtils.isNotBlank(ingressDTO.getAddress())) {
            address = ingressDTO.getAddress();
        } else {
            address = null;
        }
        String finalAddress = address;
        ingressRouteTCPList.getItems().forEach(ingressRouteTCPCR -> {
            List<String> entryPoints = ingressRouteTCPCR.getSpec().getEntryPoints();
            if (!CollectionUtils.isEmpty(entryPoints) && !CollectionUtils.isEmpty(ingressRouteTCPCR.getSpec().getRoutes()) && !CollectionUtils.isEmpty(ingressRouteTCPCR.getSpec().getRoutes().get(0).getServices())) {
                String entryPoint = entryPoints.get(0);
                IngressRouteTCPSpecRoute ingressRouteTCPSpecRoute = ingressRouteTCPCR.getSpec().getRoutes().get(0);
                IngressRouteTCPSpecRouteService ingressRouteTCPSpecRouteService = ingressRouteTCPSpecRoute.getServices().get(0);
                String port = entryPoint.substring(entryPoint.lastIndexOf("p") + 1);
                IngressDTO ingress = new IngressDTO();
                List<ServiceDTO> serviceList = new ArrayList<>();
                ServiceDTO serviceDTO = new ServiceDTO();
                serviceDTO.setExposePort(port);
                serviceDTO.setServiceName(ingressRouteTCPSpecRouteService.getName());
                serviceDTO.setServicePort(ingressRouteTCPSpecRouteService.getPort().toString());
                serviceList.add(serviceDTO);
                ingress.setServiceList(serviceList);
                ingress.setExposeIP(finalAddress);
                ingress.setMiddlewareName(ingressRouteTCPCR.getMetadata().getLabels().get("middlewareName"));
                ingress.setMiddlewareType(ingressRouteTCPCR.getMetadata().getLabels().get("middlewareType"));
                ingress.setMiddlewareNickName(aliasName);
                ingress.setName(ingressRouteTCPCR.getMetadata().getName());
                ingress.setNamespace(ingressRouteTCPCR.getMetadata().getNamespace());
                ingress.setClusterId(ingress.getClusterId());
                ingress.setProtocol(Protocol.TCP.getValue());
                ingress.setIngressClassName(ingressDTO.getIngressClassName());
                ingress.setExposeType(MIDDLEWARE_EXPOSE_INGRESS);
                ingressDTOList.add(ingress);
            }
        });
        return ingressDTOList;
    }

    private String getBaseIngressName(String middlewareName, String middlewareType, String protocol) {
        return middlewareName + "-" + middlewareType + "-" + protocol;
    }

    private String getIngressName(IngressDTO ingressDTO) {
        return getBaseIngressName(ingressDTO.getMiddlewareName(), ingressDTO.getMiddlewareType(),
                ingressDTO.getProtocol().toLowerCase()) + "-"
                + PasswordUtils.generateCommonPassword(RANDOM_LENGTH).toLowerCase();
    }

    private String getIngressTcpName(String serviceName, String namespace) {
        return serviceName + "-" + namespace + "-" + Protocol.TCP.getValue().toLowerCase();
    }

    private String getIngressTcpNamespace(MiddlewareClusterDTO cluster, String ingressClassName) {
        IngressComponentDto ingressComponentDto = ingressComponentService.get(cluster.getId(), ingressClassName);
        return ingressComponentDto == null || StringUtils.isBlank(ingressComponentDto.getNamespace()) ? KUBE_SYSTEM
                : ingressComponentDto.getNamespace();
    }

    /**
     * Ingress（k8s）convert to IngressDTO
     *
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
        if (!CollectionUtils.isEmpty(labels)) {
            if (StringUtils.isNotBlank(labels.get(MIDDLEWARE_TYPE))) {
                ingressDTO.setMiddlewareType(labels.get(MIDDLEWARE_TYPE));
            }
            if (StringUtils.isNotBlank(labels.get(MIDDLEWARE_NAME))) {
                ingressDTO.setMiddlewareName(labels.get(MIDDLEWARE_NAME));
            }
            if (StringUtils.isNotEmpty(ingress.getMetadata().getAnnotations().get(INGRESS_CLASS_NAME))) {
                ingressDTO.setIngressClassName(ingress.getMetadata().getAnnotations().get(INGRESS_CLASS_NAME));
            }
        }
        return ingressDTO;
    }

    @Override
    public List<IngressDTO> listAllIngress(String clusterId, String namespace, String keyword) {
        // 获取所有ingress
        List<IngressDTO> ingressDTOLists = list(clusterId, namespace, null);
        // 添加ingress pod信息
        setIngressExtralInfo(clusterId, ingressDTOLists);
        // 关键词过滤
        if (StringUtils.isNotEmpty(keyword)) {
            ingressDTOLists = filterByKeyword(ingressDTOLists, keyword);
        }
        // 过滤数据
        Map<String, String> power = userService.getPower();
        if (!CollectionUtils.isEmpty(power)) {
            Set<String> typeSet = power.keySet().stream()
                    .filter(key -> power.get(key).split("")[1].equals(String.valueOf(NUM_ONE))).collect(Collectors.toSet());
            ingressDTOLists = ingressDTOLists.stream()
                    .filter(ingress -> typeSet.stream().anyMatch(key -> {
                        log.info("ingress信息：{}", ingress);
                        if (ingress.getMiddlewareType() != null) {
                            return ingress.getMiddlewareType().equals(key);
                        }
                        return false;
                    }))
                    .collect(Collectors.toList());
        }
        return ingressDTOLists;
    }

    @Override
    public int getAvailablePort(String clusterId, String ingressClassName) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        //获取指定ingress
        IngressComponentDto ingressComponentDto = ingressComponentService.get(clusterId, ingressClassName);
        if (ingressComponentDto == null) {
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }
        String ingressTcpCmName = ingressComponentDto.getConfigMapName();
        String ingressTcpNamespace = ingressComponentDto.getNamespace();
        // tcp routing list
        ConfigMap configMap = configMapWrapper.get(cluster.getId(), ingressTcpNamespace, ingressTcpCmName);
        Map<String, String> data = configMap.getData();
        Random random = new Random();
        int port = 31000 + random.nextInt(100);
        for (; ; ) {
            if (data == null) {
                return port;
            }
            if (null == data.get(String.valueOf(port))) {
                return port;
            }
            port++;
        }
    }

    private List<IngressDTO> filterByKeyword(List<IngressDTO> ingressDTOList, String keyword) {
        ingressDTOList = ingressDTOList.stream().filter(ingressDTO -> {
            if (ingressDTO.getServicePurpose() != null && ingressDTO.getServicePurpose().contains(keyword)) {
                return true;
            }
            // 根据服务暴露名称、服务名称、服务中文名称过滤
            if (StringUtils.isNotBlank(ingressDTO.getMiddlewareName()) && ingressDTO.getMiddlewareName().contains(keyword)) {
                return true;
            }
            if (StringUtils.isNotBlank(ingressDTO.getMiddlewareNickName()) && ingressDTO.getMiddlewareNickName().contains(keyword)) {
                return true;
            }
            if (StringUtils.isNotBlank(ingressDTO.getName()) && ingressDTO.getName().contains(keyword)) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        return ingressDTOList;
    }

    public void upgradeValues(String clusterId, String namespace, String middlewareName, IngressDTO ingressDTO) {
        if (!checkExternalService(ingressDTO)) {
            return;
        }
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);
        // 开启对外访问
        JSONObject external = values.getJSONObject(EXTERNAL);
        String externalTag = "externalIPAddress";
        if ("kafka".equals(ingressDTO.getMiddlewareType())) {
            external.put(USE_NODE_PORT, false);
        }
        // 获取暴露ip地址
        String exposeIp = getExposeIp(cluster, ingressDTO);
        // 指定分隔符号
        String splitTag = ingressDTO.getMiddlewareType().equals(MiddlewareTypeEnum.ROCKET_MQ.getType()) ? ";" : ",";
        // 修改端口
        StringBuilder sbf = new StringBuilder();
        for (ServiceDTO serviceDTO : ingressDTO.getServiceList()) {
            if (serviceDTO.getServiceName().contains("nameserver") || serviceDTO.getServiceName().contains("manager")
                    || serviceDTO.getServiceName().contains("console")) {
                continue;
            }
            sbf.append(exposeIp).append(":").append(serviceDTO.getExposePort()).append(splitTag);
        }
        String brokerAddress = sbf.substring(0, sbf.length() - 1);
        external.put(externalTag, brokerAddress);
        external.put(ENABLE, true);
        // upgrade
        Middleware middleware = new Middleware().setChartName(ingressDTO.getMiddlewareType()).setName(middlewareName)
                .setChartVersion(values.getString("chart-version")).setNamespace(namespace);
        helmChartService.upgrade(middleware, values, values, cluster);
    }

    @Override
    public String getExposeIp(MiddlewareClusterDTO cluster, IngressDTO ingressDTO) {
        if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_NODEPORT)) {
            return nodeService.getNodeIp(cluster.getId());
        } else if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_INGRESS) && ingressDTO.getProtocol().equals(Protocol.TCP.getValue())) {
            IngressComponentDto ingressComponentDto =
                    ingressComponentService.get(cluster.getId(), ingressDTO.getIngressClassName());
            if (ingressComponentDto != null && StringUtils.isNotBlank(ingressComponentDto.getAddress())) {
                return ingressComponentDto.getAddress();
            } else {
                return getIngressNodeIp(cluster.getId(), ingressComponentDto.getNamespace(),
                        ingressComponentDto.getIngressClassName());
            }
        }
        return null;
    }

}
