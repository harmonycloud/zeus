package com.middleware.zeus.service.k8s.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.bean.BeanMiddlewareInfo;
import com.middleware.zeus.common.enums.*;
import com.middleware.zeus.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.exception.CaasRuntimeException;
import com.middleware.zeus.common.model.IngressComponentDto;
import com.middleware.zeus.common.model.TraefikPort;
import com.middleware.zeus.common.model.k8s.OwnerReferencesDo;
import com.middleware.zeus.common.model.k8s.ServiceDo;
import com.middleware.zeus.common.model.middleware.Namespace;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.dao.BeanMiddlewareInfoMapper;
import com.middleware.zeus.integration.cluster.*;
import com.middleware.zeus.integration.cluster.bean.*;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.MiddlewareCrTypeService;
import com.middleware.zeus.service.middleware.MiddlewareService;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.DateUtil;
import com.middleware.zeus.util.encrypt.PasswordUtils;
import com.middleware.zeus.util.middleware.MiddlewareServicePurposeUtil;
import com.middleware.zeus.util.numeric.MathUtil;
import com.middleware.zeus.util.uuid.UUIDUtils;
import com.skyview.language.annotations.TranslateAfterResult;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.networking.v1.*;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.CommonConstant.*;
import static com.middleware.zeus.common.constants.NameConstant.*;
import static com.middleware.zeus.common.constants.NameConstant.KUBE_SYSTEM;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;
import static com.middleware.zeus.common.constants.registry.HelmChartConstant.*;

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
    private IngressRouteTCPWrapper ingressRouteTCPWrapper;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private ServiceService serviceService;
    @Autowired
    private RedisClusterWrapper redisClusterWrapper;

    @Value("${k8s.ingress.default.name:nginx-ingress-controller}")
    private String defaultIngressName;
    @Value("${system.lb.traefikPortLength:5}")
    private Integer traefikPortLength;

    @Override
    public List<IngressDTO> list(String clusterId, String namespace, String keyword, String organId, String projectId) {
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
                    IngressRouteTcpList routeTCPList = ingressRouteTCPWrapper.list(clusterId, namespace, labels);
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
        List<Namespace> registeredNamespace = clusterService.listRegisteredNamespace(clusterId, organId, projectId);
        List<String> registeredNamespaceNameList = registeredNamespace.stream().map(Namespace::getName).collect(Collectors.toList());
        ingressDtoList = ingressDtoList.stream().filter(ingressDTO -> {
            return !StringUtils.isEmpty(ingressDTO.getMiddlewareName()) && registeredNamespaceNameList.contains(ingressDTO.getNamespace());
        }).collect(Collectors.toList());

        // package assembly
        for (IngressDTO ingressDTO : ingressDtoList) {
            if (StringUtils.isBlank(namespace)) {
                namespace = ingressDTO.getNamespace();
            }
            setMiddlewareImage(ingressDTO);
            ingressDTO.setMiddlewareOfficialName(MiddlewareOfficialNameEnum.findByChartName(ingressDTO.getMiddlewareType()));
            JSONObject values = helmChartService.getInstalledValues(ingressDTO.getMiddlewareName(), namespace, cluster);
            if (values == null) {
                continue;
            }
            ingressDTO.setChartVersion(values.getOrDefault("chart-version", "").toString());
            ingressDTO.setMiddlewareMode(values.getOrDefault("mode", "").toString());
            ingressDTO.setMiddlewareNickName(values.getOrDefault("aliasName", "").toString());
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
        // 跳过冲突端口
        if (ingressDTO.getSkipPortConflict() != null && ingressDTO.getSkipPortConflict()) {
            Set<Integer> usedPortSet = getUsedPortSet(clusterService.findById(clusterId), true);
            for (ServiceDTO serviceDTO : ingressDTO.getServiceList()){
                if (StringUtils.isEmpty(serviceDTO.getExposePort())){
                    return;
                }
                Integer exposePort = findNextExposePort(usedPortSet, Integer.valueOf(serviceDTO.getExposePort()));
                usedPortSet.add(exposePort);
                serviceDTO.setExposePort(String.valueOf(exposePort));
            }
        } else if (!CollectionUtils.isEmpty(ingressDTO.getServiceList())) {
            // 判断端口是否已被使用
            ingressDTO.getServiceList().forEach(ingress -> {
                if (StringUtils.isNotBlank(ingress.getExposePort())) {
                    verifyServicePort(clusterId, ingressDTO.getIngressClassName(), ingressDTO.getExposeType(),
                        Integer.parseInt(ingress.getExposePort()));
                }
            });
        }
        // 对部分中间件做特殊处理
        configCustomMiddleware(clusterId, namespace, middlewareName, ingressDTO);
        // 对自定义端口设置服务端口号
        configCustomPortMiddleware(clusterId, namespace, ingressDTO);

        if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_INGRESS)) {
            try {
                IngressComponentDto ingressComponentDto = ingressComponentService.get(clusterId, ingressDTO.getIngressClassName());
                if (ingressDTO.getProtocol().equals(Protocol.HTTP.getValue())) {
                    Ingress ingress = convertK8sIngress(namespace, ingressDTO, ingressComponentDto.getType());
                    ingressWrapper.create(clusterId, namespace, ingress);
                } else if (ingressDTO.getProtocol().equals(Protocol.TCP.getValue())) {
                    if (IngressEnum.TRAEFIK.getName().equals(ingressComponentDto.getType())) {
                        ingressRouteTCPWrapper.benchCreate(clusterId, convertIngressRouteTCP(ingressDTO, ingressComponentDto.getName()));
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
                    return serviceName.contains("proxy") || serviceName.contains("kafka-external-svc") || serviceName.endsWith("-master");
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
    public void checkServiceTcpPort(MiddlewareClusterDTO cluster, String ingressClassName, String exposeType,List<ServiceDTO> serviceList) {
        if (CollectionUtils.isEmpty(serviceList)) {
            return;
        }
        // 当服务暴露方式不是traefik时，端口不可以在traefik定义的端口范围内
        IngressComponentDto ingressComponent = ingressComponentService.get(cluster.getId(), ingressClassName);
        if (!(ingressComponent != null && StringUtils.equals(ingressComponent.getType(), IngressEnum.TRAEFIK.getName()))) {
            List<IngressComponentDto> ingressComponentDtos = ingressComponentService.list(cluster.getId(), IngressEnum.TRAEFIK.getName());
            List<TraefikPort> traefikPortList = new ArrayList<>();
            ingressComponentDtos.forEach(ingressComponentDto -> traefikPortList.addAll(ingressComponentDto.getTraefikPortList()));
            List<Integer> portList = serviceList.stream().map(serviceDTO -> Integer.parseInt(serviceDTO.getExposePort())).collect(Collectors.toList());
            for (Integer port : portList) {
                for (TraefikPort traefikPort : traefikPortList) {
                    if (port >= traefikPort.getStartPort() && port <= traefikPort.getEndPort()) {
                        throw new BusinessException(ErrorMessage.PORT_IS_DEFINED_BY_TRAEFIK, String.valueOf(port));
                    }
                }
            }
        }

        // 校验端口是否在nginx/traefik tcp、nodePort中使用
        Set<Integer> usedPort = new HashSet<>();
        usedPort.addAll(getNginxUsedPort(cluster));
        usedPort.addAll(getTraefikUsedPort(cluster));
        usedPort.addAll(getNodePortUsedPort(cluster));

        if (!CollectionUtils.isEmpty(usedPort)) {
            serviceList.forEach(serviceDTO -> {
                if (usedPort.contains(Integer.parseInt(serviceDTO.getExposePort()))) {
                    throw new BusinessException(ErrorMessage.TCP_PORT_ALREADY_USED);
                }
            });
        }
    }

    @Override
    public Set<Integer> getUsedPortSet(MiddlewareClusterDTO cluster, Boolean filter) {
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
        portSet.addAll(getTraefikUsedPort(cluster));
        if (filter){
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
        }
        return portSet;
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
                        configCustomPortMiddleware(clusterId, namespace, ingressDTO);
                        removeTcpPort(configMap, ingressDTO.getServiceList());
                        configMapWrapper.update(clusterId,
                                getIngressTcpNamespace(cluster, ingressDTO.getIngressClassName()), configMap);
                    } else if (IngressEnum.TRAEFIK.getName().equals(ingressComponentDto.getType())) {
                        ingressRouteTCPWrapper.delete(clusterId, namespace, name);
                    }
                }
            }
        } else if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_NODEPORT)) {
            if (!CollectionUtils.isEmpty(ingressDTO.getServiceList())) {
                for (ServiceDTO serviceDTO : ingressDTO.getServiceList()) {
                    serviceWrapper.delete(clusterId, namespace, serviceDTO.getServiceName());
                }
            } else {
                serviceWrapper.delete(clusterId, namespace, name);
            }
        }
        // 关闭redis哨兵模式集群外访问
        if (ingressDTO.getMiddlewareType().equals(MiddlewareTypeEnum.REDIS.getType())
            && ingressDTO.getExternalEnable() != null && ingressDTO.getExternalEnable()) {
            // traefik 删除额外服务暴露
            IngressRouteTcpList ingressRouteTcpList = ingressRouteTCPWrapper.list(clusterId, namespace,
                getIngressTCPLabels(middlewareName, ingressDTO.getMiddlewareType(), ingressDTO.getIngressClassName()));
            if (!CollectionUtils.isEmpty(ingressRouteTcpList.getItems())) {
                for (IngressRouteTcp ingressRouteTcp : ingressRouteTcpList.getItems()) {
                    String ingressRouteTcpName = ingressRouteTcp.getMetadata().getName();
                    if (!CollectionUtils.isEmpty(ingressDTO.getServiceList())
                        && ingressDTO.getServiceList().stream().anyMatch(serviceDTO -> ingressRouteTcpName
                            .matches("^" + serviceDTO.getServiceName() + LINE + TCP + LINE + ".+" + "$"))) {
                        ingressRouteTCPWrapper.delete(clusterId, namespace, ingressRouteTcpName);
                    }
                }
            }

            // 更新values.yaml
            MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
            JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);
            Middleware middleware =
                new Middleware(clusterId, namespace, middlewareName, ingressDTO.getMiddlewareType());
            middleware.setChartName(ingressDTO.getMiddlewareType());
            middleware.setChartVersion(helmChartService.getChartVersion(values, ingressDTO.getMiddlewareType()));
            helmChartService.upgrade(middleware, "redis.externalAccess.enabled=false", cluster.getId());
            // 删除代码创建的svc
            if (!CollectionUtils.isEmpty(ingressDTO.getServiceList())) {
                for (int i = 0; i < ingressDTO.getServiceList().size(); ++i) {
                    serviceService.delete(clusterId, namespace, middlewareName + LINE + i + LINE + POD);
                    serviceService.delete(clusterId, namespace,
                        middlewareName + LINE + i + LINE + POD + LINE + "16379");
                }
            }
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
                List<IngressDTO> nodePortList = new ArrayList<>();
                svcList.forEach(svc -> {
                    // 过滤不包含命名规则的中间件
                    if (!svc.getMetadata().getName().contains(middlewareName)
                        || !svc.getMetadata().getName().contains("nodeport") || svc.getMetadata().getLabels() == null
                        || !svc.getMetadata().getLabels().containsKey(MIDDLEWARE_NAME)
                        || !svc.getMetadata().getLabels().containsValue(middlewareName)) {
                        return;
                    }
                    
                    IngressDTO dto = dealNodePortRoutine(clusterId, namespace, cluster.getHost(), svc);
                    if (dto != null) {
                        nodePortList.add(dto);
                    }
                });
                // 特殊处理 kafka/rocketmq/redis等集群外访问情况
                resolveExternalSituation(new Middleware(clusterId, namespace, middlewareName, type), nodePortList);

                resList.addAll(nodePortList);
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
                        IngressRouteTcpList routeTCPList = ingressRouteTCPWrapper.list(clusterId, namespace,
                                getIngressTCPLabels(middlewareName, type, ingress.getName()));

                        List<IngressDTO> ingressDTOList = convertIngressDTOList(ingress, routeTCPList, null);
                        // 处理redis场景
                        resolveExternalSituationInTraefik(new Middleware(clusterId, namespace, middlewareName, type), ingressDTOList);

                        resList.addAll(ingressDTOList);
                    }
                }
            }
        }
        // 添加ingress pod信息
        setIngressExtralInfo(clusterId, resList);
        // 设置图片
        setMiddlewareImage(clusterId, namespace, type, middlewareName, resList);
        // 特殊处理rocketmq和kafka
        if ("rocketmq".equals(type) || "kafka".equals(type) || REDIS.equals(type)) {
            setExternalServiceExposeStatus(type, resList);
        }
        return resList;
    }

    @Override
    @TranslateAfterResult
    public List<IngressDTO> getMiddlewareIngress(String clusterId, String namespace, String type, String middlewareName) {
        return get(clusterId, namespace, type, middlewareName).stream().
                filter(ingressDTO -> ingressDTO.getServicePurpose() != null && !"null".equals(ingressDTO.getServicePurpose())).
                collect(Collectors.toList());
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
                    new IngressHttpPath().setPath(httpPath.getPath()).setServiceName(httpPath.getBackend().getService().getName())
                            .setServicePort(httpPath.getBackend().getService().getPort().getNumber().toString())));
            resList.add(dto);
        });
        return resList;
    }

    @Override
    public void verifyServicePort(String clusterId, String ingressClassName, String exposeType, Integer port) {
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setExposePort(String.valueOf(port));
        List<ServiceDTO> serviceDTOList = new ArrayList<>();
        serviceDTOList.add(serviceDTO);
        checkServiceTcpPort(clusterService.findById(clusterId), ingressClassName, exposeType,serviceDTOList);
    }

    @Override
    public List<String> listIngressIp(String clusterId, String ingressClassName) {
        IngressComponentDto ingressComponentDto = ingressComponentService.get(clusterId, ingressClassName);
        if (ingressComponentDto == null) {
            return Collections.emptyList();
        }
        List<PodInfo> podInfoList;
        List<String> ingressPodIpList = new ArrayList<>();
        if (StringUtils.isNotBlank(ingressComponentDto.getAddress())) {
            ingressPodIpList.add(ingressComponentDto.getAddress());
        } else {
            podInfoList = listIngressPod(clusterId, ingressComponentDto.getNamespace(), ingressComponentDto.getName());
            podInfoList = podInfoList.stream().filter(podInfo -> "Running".equals(podInfo.getStatus())
                    && StringUtils.isNotBlank(podInfo.getHostIp())).collect(Collectors.toList());
            podInfoList.forEach(podInfo -> {
                ingressPodIpList.add(podInfo.getHostIp());
            });
        }
        return ingressPodIpList;
    }

    @Override
    public String getIngressIp(String clusterId, String ingressClassName) {
        List<String> ingressIpSet = listIngressIp(clusterId, ingressClassName);
        if (CollectionUtils.isEmpty(ingressIpSet)) {
            throw new BusinessException(ErrorMessage.INGRESS_NOT_AVAILABLE);
        }
        return ingressIpSet.get(0);
    }

    @Override
    @TranslateAfterResult
    public List<IngressDTO> getHostNetworkAddress(String clusterId, String namespace, String type, String middlewareName) {
        return middlewareService.listHostNetworkAddress(clusterId, namespace, middlewareName, type);
    }

    @Override
    public String portCheck(String clusterId, String namespace, String middlewareName, Integer startPort, Integer endPort){
        // 获取集群对象
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 初始化记录冲突端口
        List<Integer> conflictPortList = new ArrayList<>();
        // 统计使用中的端口
        Set<Integer> usedPortSet = new HashSet<>();
        usedPortSet.addAll(getTraefikUsedPort(cluster));
        usedPortSet.addAll(getNodePortUsedPort(cluster));
        usedPortSet.addAll(getNginxUsedPort(cluster));
        // todo 去除修改场景下该中间件自身服务正在使用的端口
        for (int i = startPort; i <= endPort; ++i){
            if (usedPortSet.contains(i)){
                conflictPortList.add(i);
            }
        }
        return MathUtil.convert(conflictPortList);
    }

    // 对部分中间件做特殊处理
    private void configCustomMiddleware(String clusterId, String namespace, String middlewareName, IngressDTO ingressDTO) {
        String middlewareType = ingressDTO.getMiddlewareType();
        switch (middlewareType) {
            case "rocketmq":
            case "kafka":
                allocateMQServicePort(clusterId, ingressDTO);
                break;
            case "mysql":
                setMysqlServicePort(clusterId, namespace, middlewareName, ingressDTO);
                break;
            case "redis":
                createRedisPodService(clusterId, namespace, middlewareName, ingressDTO);
        }
    }

    /**
     * 自定义端口后，创建或删除服务暴露时，需要查询service 端口号,并配置到servicedto中
     * @param clusterId
     * @param namespace
     * @param ingressDTO
     */
    private void configCustomPortMiddleware(String clusterId, String namespace, IngressDTO ingressDTO) {
        String middlewareType = ingressDTO.getMiddlewareType();
        switch (middlewareType) {
            case "redis":
            case "elasticsearch":
            case "postgres":
                setCommonServicePort(clusterId, namespace, ingressDTO);
                break;
        }
    }

    /**
     * 自定义端口后，创建或删除服务暴露时，需要查询service 端口号
     * @param clusterId
     * @param namespace
     * @param ingressDTO
     */
    private void setCommonServicePort(String clusterId, String namespace, IngressDTO ingressDTO) {
        if (CollectionUtils.isEmpty(ingressDTO.getServiceList())){
            return;
        }
        ingressDTO.getServiceList().forEach(serviceDTO -> {
            String serviceName = serviceDTO.getServiceName();
            io.fabric8.kubernetes.api.model.Service service = serviceWrapper.get(clusterId, namespace, serviceName);
            if (service != null && service.getSpec() != null && !CollectionUtils.isEmpty(service.getSpec().getPorts())) {
                List<ServicePort> ports = service.getSpec().getPorts();
                if (!CollectionUtils.isEmpty(ports)) {
                    List<ServicePort> servicePorts = ports.stream().filter(servicePort ->
                            servicePort.getName().equals(serviceName)).collect(Collectors.toList());
                    ServicePort servicePort = ports.get(0);
                    if (!CollectionUtils.isEmpty(servicePorts)) {
                        servicePort = servicePorts.get(0);
                    }
                    serviceDTO.setTargetPort(String.valueOf(servicePort.getTargetPort().getIntVal()));
                    serviceDTO.setServicePort(servicePort.getPort().toString());
                }
            }
        });
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
     * 获取traefik已使用端口
     * @return
     */
    private Set<Integer> getTraefikUsedPort(MiddlewareClusterDTO cluster) {
        Set<Integer> traefikPortSet = new HashSet<>();
        IngressRouteTcpList tcpList = ingressRouteTCPWrapper.list(cluster.getId(), null, null);
        if (tcpList != null && !CollectionUtils.isEmpty(tcpList.getItems())) {
            tcpList.getItems().forEach(tcpCR -> {
                try {
                    if (null != tcpCR && null != tcpCR.getSpec() && !CollectionUtils.isEmpty(tcpCR.getSpec().getEntryPoints())) {
                        String entryPoint = tcpCR.getSpec().getEntryPoints().get(0);
                        String regex = "[^0-9]";
                        traefikPortSet.add(Integer.parseInt(entryPoint.replaceAll(regex, "")));
                    }
                } catch (Exception e) {
                    log.error("获取traefik tcp 端口失败", e);
                }
            });
        }
        return traefikPortSet;
    }

    /**
     * 获取nginx已使用端口
     * @return Set<Integer>
     */
    private Set<Integer> getNginxUsedPort(MiddlewareClusterDTO cluster) {
        Set<Integer> nginxUsedPort = new HashSet<>();
        List<IngressComponentDto> nginxComponentDtoList =
            ingressComponentService.list(cluster.getId(), IngressEnum.NGINX.getName());
        nginxComponentDtoList.forEach(ingress -> {
            String ingressTcpCmName = ingress.getConfigMapName();
            try {
                ConfigMap configMap = configMapWrapper.get(cluster.getId(),
                    getIngressTcpNamespace(cluster, ingress.getIngressClassName()), ingressTcpCmName);
                nginxUsedPort
                    .addAll(configMap.getData().keySet().stream().map(Integer::parseInt).collect(Collectors.toList()));
            } catch (Exception e) {
                log.error("nginx {} 获取使用端口失败", ingressTcpCmName);
            }
        });
        return nginxUsedPort;
    }

    /**
     * 获取nodeport已使用端口
     * @param cluster
     * @return
     */
    private Set<Integer> getNodePortUsedPort(MiddlewareClusterDTO cluster) {
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
        return nodePorts;
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
                ingressDTO.setIngressIpSet(new HashSet<>(listIngressIp(clusterId, ingressDTO.getIngressClassName())));
            }
            // 设置服务暴露的网络模型 4层或7层
            setServiceNetworkModel(ingressDTO);
            // 设置服务用途
            if (ingressDTO.getServicePurpose() == null) {
                ingressDTO.setServicePurpose(MiddlewareServicePurposeUtil.convertChinesePurpose(ingressDTO));
            }
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
        List<PodInfo> podInfoList = podService.list(clusterId, namespace, labels);
        if (CollectionUtils.isEmpty(podInfoList)) {
            podInfoList = getIngressPodSet(clusterId, namespace, ingressClassName);
        }
        return podInfoList;
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
        List<PodInfo> podInfoList = listIngressPod(clusterId, namespace, ingressClassName);
        if (!CollectionUtils.isEmpty(podInfoList)) {
            return podInfoList.get(0).getHostIp();
        }
        return "";
    }

    /**
     * 根据ingressclassname查找ingress pod
     * 当接入ingress时，如果ingress不是helm安装的，
     * 则需要通过pod名称来查询ingress pod
     * @param clusterId
     * @param namespace
     * @param ingressClassName
     * @return
     */
    public List<PodInfo> getIngressPodSet(String clusterId, String namespace, String ingressClassName) {
        return podService.list(clusterId, namespace, ingressClassName);
    }

    private void setMysqlServicePort(String clusterId, String namespace, String middlewareName, IngressDTO ingressDTO) {
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        String port = "3306";
        if (values != null && values.containsKey("args")) {
            JSONObject args = values.getJSONObject("args");
            if (args != null && args.containsKey("server_port")) {
                port = args.getString("server_port");
            }
        }
        String finalPort = port;
        ingressDTO.getServiceList().forEach(serviceDTO -> {
            serviceDTO.setTargetPort(finalPort);
            serviceDTO.setServicePort(finalPort);
        });
    }

    /**
     * 当为kafka或rockeymq暴露服务时，若用户未设置服务端口号，则为服务随机分配端口号
     * @param clusterId
     * @param ingressDTO
     */
    private void allocateMQServicePort(String clusterId, IngressDTO ingressDTO) {
        if ("rocketmq".equals(ingressDTO.getMiddlewareType()) || "kafka".equals(ingressDTO.getMiddlewareType())) {
            List<ServiceDTO> serviceList = ingressDTO.getServiceList();
            if (CollectionUtils.isEmpty(serviceList)) {
                return;
            }
            List<Integer> portList = getAvailablePort(clusterId, serviceList.size());
            for (int i = 0; i < serviceList.size(); i++) {
                ServiceDTO serviceDTO = serviceList.get(i);
                setServicePort(serviceDTO, ingressDTO.getMiddlewareType());
                if (!checkExternalService(serviceDTO)) {
                    continue;
                }
                if (StringUtils.isBlank(serviceDTO.getExposePort()) && !serviceDTO.getServiceName().contains("proxy")) {
                    serviceDTO.setExposePort(String.valueOf(portList.get(i)));
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
        int startPort = 30002;
        List<Integer> portList = new ArrayList<>();
        Set<Integer> usedPortSet = getUsedPortSet(clusterService.findById(clusterId), true);
        for (int i = 0; i < portNum; ) {
            if (!usedPortSet.contains(startPort)) {
                portList.add(startPort);
                i++;
            }
            startPort++;
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
            if (serviceName.contains("nameserver-proxy-svc")) {
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
                    serviceDTO.getServiceName().contains("nameserver-proxy-svc") || serviceDTO.getServiceName().endsWith("-master")
                            || serviceDTO.getServiceName().split("-")[serviceDTO.getServiceName().split("-").length - 2].equals("slave")).collect(Collectors.toList());
        } else if ("kafka".equals(ingressDTO.getMiddlewareType())) {
            serviceDTOList = serviceDTOList.stream().filter(serviceDTO ->
                    serviceDTO.getServiceName().contains("kafka-external-svc")).collect(Collectors.toList());
        }
        Map<String, ServicePortDTO> svcMap = serviceService.list(ingressDTO.getClusterId(), namespace)
                .stream().collect(Collectors.toMap(ServicePortDTO::getServiceName, Function.identity()));
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
            servicePortList.add(covertMQServicePort(serviceDTO, ingressDTO.getMiddlewareType(), svcMap));
            spec.setPorts(servicePortList);
            spec.setSelector(getMQSelector(middlewareName, ingressDTO.getMiddlewareType(), serviceName, svcMap));
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

    private Map<String, String> getMQSelector(String middlewareName, String type, String serviceName, Map<String, ServicePortDTO> svcMap) {
        Map<String, String> selector = new HashMap<>();
        if ("rocketmq".equals(type)) {
            if (svcMap.containsKey(serviceName)) {
                ServicePortDTO servicePortDTO = svcMap.get(serviceName);
                selector.putAll(servicePortDTO.getSelector());
            } else {
                String value;
                if (serviceName.endsWith("-master")) {
                    value = middlewareName + "-broker-" + serviceName.replace(middlewareName + "-", "").split("-")[0] + "-0";
                } else if (serviceName.split("-")[serviceName.split("-").length - 2].equals("slave")) {
                    value = middlewareName + "-broker-" + serviceName.replace(middlewareName + "-", "").split("-")[0]
                            + "-" + serviceName.split("-")[serviceName.split("-").length -1];
                } else {
                    value = middlewareName + "namesrv-proxy-" + serviceName.substring(serviceName.lastIndexOf("-") + 1);
                }
                selector.put("statefulset.kubernetes.io/pod-name", value);
            }
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
        intOrString.setValue(Integer.parseInt(serviceDTO.getTargetPort()));
        servicePort.setTargetPort(intOrString);

        return servicePort;
    }

    private ServicePort covertMQServicePort(ServiceDTO serviceDTO, String type, Map<String, ServicePortDTO> svcMap) {
        ServicePort servicePort = new ServicePort();
        if (StringUtils.isNotEmpty(serviceDTO.getExposePort())) {
            servicePort.setNodePort(Integer.parseInt(serviceDTO.getExposePort()));
        }
        servicePort.setProtocol(Protocol.TCP.getValue());
        if ("rocketmq".equals(type)) {
            if (svcMap.containsKey(serviceDTO.getServiceName())) {
                ServicePortDTO servicePortDTO = svcMap.get(serviceDTO.getServiceName());
                servicePort.setPort(Integer.parseInt(servicePortDTO.getPortDetailDtoList().get(0).getPort()));
                servicePort.setTargetPort(new IntOrString(Integer.parseInt(servicePortDTO.getPortDetailDtoList().get(0).getTargetPort())));
            } else {
                if (serviceDTO.getServiceName().contains("nameserver-proxy-svc")) {
                    servicePort.setPort(9876);
                    servicePort.setTargetPort(new IntOrString(9876));
                } else if (serviceDTO.getServiceName().endsWith("-master")
                        || serviceDTO.getServiceName().split("-")[serviceDTO.getServiceName().split("-").length - 2].equals("slave")) {
                    servicePort.setPort(10911);
                    servicePort.setTargetPort(new IntOrString(10911));
                } else {
                    throw new BusinessException(ErrorMessage.INGRESS_NODEPORT_PORT_NOT_NULL);
                }
            }
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
        List<ServiceDTO> serviceDTOList = new ArrayList<>(20);
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
        Middleware middleware = new Middleware(clusterId, namespace, crd.getSpec().getName(), middlewareCrTypeService.findTypeByCrType(crd.getSpec().getType()));
        resolveExternalSituationInTcp(svcName, middleware, ingressDTOList, tcpRoutineMap);
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


    private List<IngressRouteTcp> convertIngressRouteTCP(IngressDTO ingressDTO, String ingressName) {
        if (CollectionUtils.isEmpty(ingressDTO.getServiceList())) {
            throw new CaasRuntimeException(ErrorMessage.INGRESS_TCP_NOT_NULL);
        }
        List<IngressRouteTcp> routeTCPCRList = new ArrayList<>();
        for (ServiceDTO serviceDTO : ingressDTO.getServiceList()) {
            String ingressRouteTCPName = serviceDTO.getServiceName() + "-tcp-" + UUIDUtils.get8UUID();
            if (StringUtils.isNotBlank(ingressDTO.getName())) {
                ingressRouteTCPName = ingressDTO.getName();
            }
            Map<String, String> labels = getIngressTCPLabels(ingressDTO.getMiddlewareName(), ingressDTO.getMiddlewareType(), ingressName);
            IngressRouteTcp ingressRouteTcp = new IngressRouteTcp(ingressRouteTCPName, ingressDTO.getNamespace(),
                    ingressName + "-p" + serviceDTO.getExposePort(), serviceDTO.getServiceName(), Integer.parseInt(serviceDTO.getServicePort()), labels);


            routeTCPCRList.add(ingressRouteTcp);
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
    private Ingress convertK8sIngress(String namespace, IngressDTO ingressDTO, String ingressType) {
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
        if (ingressType.equals(IngressEnum.TRAEFIK.getName())){
            annotations.put("traefik.ingress.kubernetes.io/router.entrypoints", "web");
        }
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
                        httpIngressPath.setPathType("ImplementationSpecific");
                        
                        IngressServiceBackend ingressServiceBackend = new IngressServiceBackend();
                        ingressServiceBackend.setName(ingressHttpPath.getServiceName());
                        ingressServiceBackend
                            .setPort(new ServiceBackendPort(null, Integer.parseInt(ingressHttpPath.getServicePort())));

                        IngressBackend ingressBackend = new IngressBackend();
                        ingressBackend.setService(ingressServiceBackend);
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
        spec.setIngressClassName(ingressClassName);

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

    private List<IngressDTO> convertIngressDTOList(IngressComponentDto ingressDTO, IngressRouteTcpList ingressRouteTCPList, String aliasName) {
        if (ingressRouteTCPList == null || CollectionUtils.isEmpty(ingressRouteTCPList.getItems())) {
            return Collections.emptyList();
        }
        List<IngressDTO> ingressDTOList = new ArrayList<>();
        String address;
        if (StringUtils.isNotBlank(ingressDTO.getAddress())) {
            address = ingressDTO.getAddress();
        } else {
            address = null;
        }
        // 过滤掉不属于中间件的ingressRouteTCP CR
        List<IngressRouteTcp> items = ingressRouteTCPList.getItems().stream().filter(ingressRouteTcp
                -> ingressRouteTcp.getMetadata().getLabels() != null
                && ingressRouteTcp.getMetadata().getLabels().containsKey("middlewareType")).collect(Collectors.toList());

        String finalAddress = address;
        items.forEach(ingressRouteTcp -> {
            List<String> entryPoints = ingressRouteTcp.getSpec().getEntryPoints();
            if (!CollectionUtils.isEmpty(entryPoints) && !CollectionUtils.isEmpty(ingressRouteTcp.getSpec().getRoutes()) && !CollectionUtils.isEmpty(ingressRouteTcp.getSpec().getRoutes().get(0).getServices())) {
                String entryPoint = entryPoints.get(0);
                IngressRouteTcpSpecRoute ingressRouteTCPSpecRoute = ingressRouteTcp.getSpec().getRoutes().get(0);
                IngressRouteTcpSpecRouteService ingressRouteTCPSpecRouteService = ingressRouteTCPSpecRoute.getServices().get(0);
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
                ingress.setMiddlewareName(ingressRouteTcp.getMetadata().getLabels().get("middlewareName"));
                ingress.setMiddlewareType(ingressRouteTcp.getMetadata().getLabels().get("middlewareType"));
                ingress.setMiddlewareNickName(aliasName);
                ingress.setName(ingressRouteTcp.getMetadata().getName());
                ingress.setNamespace(ingressRouteTcp.getMetadata().getNamespace());
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
                        ingressHttpPath.setServiceName(ingressBackend.getService().getName());
                        ingressHttpPath.setServicePort(ingressBackend.getService().getPort().getNumber().toString());
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
    @TranslateAfterResult
    public List<IngressDTO> listAllIngress(String clusterId, String namespace, String keyword, String organId, String projectId) {
        // 获取所有ingress
        List<IngressDTO> ingressDTOLists = list(clusterId, namespace, null, organId, projectId);
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
                        //log.info("ingress信息：{}", ingress);
                        if (ingress.getMiddlewareType() != null) {
                            return ingress.getMiddlewareType().equals(key);
                        }
                        return false;
                    }))
                    .collect(Collectors.toList());
        }
        // 设置服务网络类型(4层或7层)和服务暴露名称
        setIngressExtralInfo(clusterId, ingressDTOLists);
        // 设置中间件图片
        for (IngressDTO ingressDTO : ingressDTOLists) {
            setMiddlewareImage(ingressDTO);
        }
        return ingressDTOLists;
    }

    @Override
    @TranslateAfterResult
    public List<IngressDTO> listAllMiddlewareIngress(String clusterId, String namespace, String keyword, String organId, String projectId) {
        return listAllIngress(clusterId, namespace, keyword, organId, projectId).stream().
                filter(ingressDTO -> !StringUtils.isEmpty(ingressDTO.getServicePurpose())).collect(Collectors.toList());
    }

    @Override
    public int getAvailablePort(String clusterId, String ingressClassName) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        //获取指定ingress
        IngressComponentDto ingressComponentDto = ingressComponentService.get(clusterId, ingressClassName);
        if (ingressComponentDto == null) {
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }
        if (IngressEnum.TRAEFIK.getName().equals(ingressComponentDto.getType())) {
            return getTraefikAvailableServicePort(cluster, ingressComponentDto);
        } else {
            return getNginxAvailableServicePort(cluster, ingressComponentDto);
        }
    }

    private int getNginxAvailableServicePort(MiddlewareClusterDTO cluster, IngressComponentDto ingressComponentDto) {
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

    private int getTraefikAvailableServicePort(MiddlewareClusterDTO cluster, IngressComponentDto ingressComponentDto) {
        // 获取已使用端口
        Set<Integer> traefikUsedPort = getTraefikUsedPort(cluster);
        // 获取可配置端口范围
        IngressComponentDto detail = ingressComponentService.detail(cluster.getId(), ingressComponentDto.getIngressClassName());
        List<TraefikPort> traefikPortList = detail.getTraefikPortList();
        for (TraefikPort traefikPort : traefikPortList){
            for (int i = traefikPort.getStartPort(); i < traefikPort.getEndPort(); ++i){
                if (!traefikUsedPort.contains(i)){
                    return i;
                }
            }
        }
        return 0;
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

    /**
     * 创建redis pod service
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param ingressDTO ingress对象
     */
    public void createRedisPodService(String clusterId, String namespace, String middlewareName, IngressDTO ingressDTO){
        List<ServiceDTO> serviceDTOList = ingressDTO.getServiceList();
        if(CollectionUtils.isEmpty(serviceDTOList)){
            return;
        }
        // ha版本redis哨兵服务暴露分为多端口和单端口，若未申明为多端口则返回
        if (ingressDTO.getExternalEnable() == null || !ingressDTO.getExternalEnable() ) {
            return;
        }
        // 非哨兵服务暴露返回
        if (serviceDTOList.stream().noneMatch(serviceDTO -> serviceDTO.getServiceName().equals(middlewareName + LINE + SENTINEL))){
            return;
        }
        // 判断服务是否是主机网络,是则返回
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        JSONObject values =
            helmChartService.getInstalledValues(middlewareName, namespace, cluster);
        if (values == null || !values.containsKey(REDIS) || values.getJSONObject(REDIS) == null
            || !values.getJSONObject(REDIS).containsKey("hostNetwork")
            || values.getJSONObject(REDIS).getBoolean("hostNetwork")) {
            return;
        }

        // 获取redisCluster,并封装ownerReferences
        RedisCluster rec = redisClusterWrapper.get(clusterId, namespace, middlewareName);
        if (rec == null){
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }
        OwnerReferencesDo ownerReferencesDo = new OwnerReferencesDo();
        ownerReferencesDo.setApiVersion(rec.getApiVersion());
        ownerReferencesDo.setController(true);
        ownerReferencesDo.setName(middlewareName);
        ownerReferencesDo.setBlockOwnerDeletion(false);
        ownerReferencesDo.setKind(rec.getKind());
        ownerReferencesDo.setUid(rec.getMetadata().getUid());

        // 初始化serviceDo
        PortDetailDTO portDetailDTO = new PortDetailDTO();
        portDetailDTO.setName(REDIS);
        portDetailDTO.setPort(values.getJSONObject(REDIS).getString(PORT));
        portDetailDTO.setTargetPort(values.getJSONObject(REDIS).getString(PORT));
        portDetailDTO.setProtocol("TCP");

        ServiceDo serviceDo = new ServiceDo();
        serviceDo.setClusterId(clusterId);
        serviceDo.setNamespace(namespace);
        serviceDo.setPortDetailDtoList(Collections.singletonList(portDetailDTO));

        // 创建service
        for (ServiceDTO serviceDTO : serviceDTOList){
            if (serviceDTO.getServiceName().equals(middlewareName + LINE + SENTINEL)){
                continue;
            }

            if (serviceService.get(clusterId, namespace, serviceDTO.getServiceName()) != null){
                continue;
            }
            Map<String, String> selector = new HashMap<>();
            selector.put("app", middlewareName);
            selector.put("component", middlewareName);
            selector.put("middleware", REDIS);
            selector.put("statefulset.kubernetes.io/pod-name", serviceDTO.getServiceName().replace(LINE + POD, ""));

            serviceDo.setName(serviceDTO.getServiceName());
            serviceDo.setOwnerReferencesDoList(Collections.singletonList(ownerReferencesDo));
            serviceDo.setSelector(selector);

            serviceService.create(clusterId, namespace, serviceDo);
        }

        // 更新redis配置，开启redis哨兵的集群外访问
        JSONObject externalAccess = values.getJSONObject(REDIS).getJSONObject("externalAccess");
        if (externalAccess == null){
            externalAccess = new JSONObject();
        }
        externalAccess.put("enabled", true);

        // 设置redis服务暴露地址
        String host = cluster.getHost();
        if (ingressDTO.getExposeType().equals(MIDDLEWARE_EXPOSE_NODEPORT)){
            host = cluster.getHost();
        } else if (ingressDTO.getExposeType().equals(MIDDLEWARE_EXPOSE_INGRESS)){
            List<String> ipList = listIngressIp(clusterId, ingressDTO.getIngressClassName());
            if (!CollectionUtils.isEmpty(ipList)){
                host = ipList.get(0);
            }
        }

        JSONObject addresses = new JSONObject();
        for (ServiceDTO serviceDTO : serviceDTOList){
            if (serviceDTO.getServiceName().equals(middlewareName + LINE + SENTINEL)){
                externalAccess.put(serviceDTO.getServiceName(), host + ":" + serviceDTO.getExposePort());
                continue;
            }
            // 设置pod名称
            String podName = serviceDTO.getServiceName().replace(LINE + POD, "");
            addresses.put(podName, host + ":" + serviceDTO.getExposePort());
        }
        externalAccess.put("addresses", addresses);
        // 开启哨兵模式集群外访问
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, ingressDTO.getMiddlewareType());
        middleware.setChartName(ingressDTO.getMiddlewareType());
        middleware.setChartVersion(helmChartService.getChartVersion(values, ingressDTO.getMiddlewareType()));
        // 记录跳过冲突端口
        if (ingressDTO.getSkipPortConflict() != null){
            values.put(SKIP_PORT_CONFLICT, ingressDTO.getSkipPortConflict());
        }
        helmChartService.upgrade(middleware, values, values, cluster);
    }
    
    public void resolveExternalSituation(Middleware middleware, List<IngressDTO> ingressDTOList){
        if (MiddlewareTypeEnum.REDIS.getType().equals(middleware.getType())){
            // 不包含哨兵服务暴露返回
            if (ingressDTOList.stream()
                .noneMatch(ingressDTO -> ingressDTO.getName().contains(middleware.getName() + LINE + SENTINEL + LINE + NODE_PORT))) {
                return;
            }
            // 判断服务是否是主机网络,是则返回
            JSONObject values = helmChartService.getInstalledValues(middleware.getName(), middleware.getNamespace(),
                clusterService.findById(middleware.getClusterId()));
            if (values == null || !values.containsKey(REDIS) || values.getJSONObject(REDIS) == null
                || !values.getJSONObject(REDIS).containsKey("hostNetwork")
                || values.getJSONObject(REDIS).getBoolean("hostNetwork")) {
                return;
            }
            // 将pod service合并进哨兵服务的ingress对象内
            for (IngressDTO ingressDTO : ingressDTOList) {
                if (ingressDTO.getName().contains(middleware.getName() + LINE + SENTINEL + LINE + NODE_PORT)) {
                    for (IngressDTO ing : ingressDTOList) {
                        if (ing.getName().contains(middleware.getName() + LINE)
                            && ing.getName().contains(LINE + POD + LINE + NODE_PORT)) {
                            ingressDTO.getServiceList().addAll(ing.getServiceList());
                            ingressDTO.setExternalEnable(true);
                        }
                    }
                    if (values.containsKey(SKIP_PORT_CONFLICT)){
                        ingressDTO.setSkipPortConflict(values.getBoolean(SKIP_PORT_CONFLICT));
                    }
                }
            }
            // 移除pod service的ingress对象
            ingressDTOList.removeIf(ing -> ing.getName().endsWith(LINE + POD));
        }
    }
    
    public void resolveExternalSituationInTcp(String svcName, Middleware middleware, List<IngressDTO> ingressDTOList,
        Map<String, List<ServiceDTO>> tcpRoutineMap) {
        if (MiddlewareTypeEnum.REDIS.getType().equals(middleware.getType())) {
            if (!svcName.equals(middleware.getName() + LINE + SENTINEL)){
                return;
            }
            // 判断服务是否是主机网络,是则返回
            JSONObject values = helmChartService.getInstalledValues(middleware.getName(), middleware.getNamespace(),
                    clusterService.findById(middleware.getClusterId()));
            if (values == null || !values.containsKey(REDIS) || values.getJSONObject(REDIS) == null
                    || !values.getJSONObject(REDIS).containsKey("hostNetwork")
                    || values.getJSONObject(REDIS).getBoolean("hostNetwork")) {
                return;
            }

            // 哨兵svc集群外访问 合并服务svc信息
            for (IngressDTO ingressDTO : ingressDTOList) {
                if (ingressDTO.getName().equals(getIngressTcpName(svcName, middleware.getNamespace()))) {
                    for (String key : tcpRoutineMap.keySet()) {
                        if (key.matches("^" + middleware.getNamespace() + SLASH + middleware.getName() + LINE + "[0-9]+"
                            + LINE + POD + "$")) {
                            ingressDTO.getServiceList().addAll(tcpRoutineMap.get(key));
                            ingressDTO.setExternalEnable(true);
                        }
                    }
                    if (values.containsKey(SKIP_PORT_CONFLICT)){
                        ingressDTO.setSkipPortConflict(values.getBoolean(SKIP_PORT_CONFLICT));
                    }
                }
            }
        }
    }

    public void resolveExternalSituationInTraefik(Middleware middleware, List<IngressDTO> ingressDTOList){
        if (MiddlewareTypeEnum.REDIS.getType().equals(middleware.getType())){
            // 判断服务是否是主机网络,是则返回
            JSONObject values = helmChartService.getInstalledValues(middleware.getName(), middleware.getNamespace(),
                    clusterService.findById(middleware.getClusterId()));
            if (values == null || !values.containsKey(REDIS) || values.getJSONObject(REDIS) == null
                    || !values.getJSONObject(REDIS).containsKey("hostNetwork")
                    || values.getJSONObject(REDIS).getBoolean("hostNetwork")) {
                return;
            }
            JSONObject redis = values.getJSONObject(REDIS);
            if (redis == null || redis.getJSONObject("externalAccess") == null
                || redis.getJSONObject("externalAccess").getBoolean("enabled") == null
                || !redis.getJSONObject("externalAccess").getBoolean("enabled")) {
                return;
            }
            // 哨兵模式服务暴露处理
            if (ingressDTOList.stream().anyMatch(ingressDTO -> ingressDTO.getName()
                .matches("^" + middleware.getName() + LINE + SENTINEL + LINE + TCP + LINE + ".+" + "$"))) {
//                // 根据values.yaml 获取端口范围
//                List<Integer> portList;
//                JSONObject addresses = redis.getJSONObject("externalAccess").getJSONObject("addresses");
//                portList = addresses.keySet().stream()
//                    .map(key -> Integer.parseInt(addresses.getString(key).split(":")[1])).collect(Collectors.toList());
//                // 根据端口范围，过滤获取需要处理的ingressList
//                ingressDTOList.removeIf(ing -> ing.getServiceList().stream()
//                    .noneMatch(serviceDTO -> portList.contains(Integer.parseInt(serviceDTO.getExposePort()))));

                // 获取符合哨兵模式服务暴露的ingress
                IngressDTO ingressDTO = ingressDTOList.stream()
                    .filter(dto -> dto.getName()
                        .matches("^" + middleware.getName() + LINE + SENTINEL + LINE + TCP + LINE + ".+" + "$"))
                    .collect(Collectors.toList()).get(0);
                ingressDTO.setExternalEnable(true);
                // 记录无需整合的临时IngressList
                List<IngressDTO> tempIngressList = new ArrayList<>();
                // 将pod service合并进哨兵服务的ingress对象内
                List<ServiceDTO> serviceList = new ArrayList<>();
                for (IngressDTO ing : ingressDTOList) {
                    if (ing.getName()
                        .matches("^" + middleware.getName() + LINE + SENTINEL + LINE + TCP + LINE + ".+" + "$")
                        || ing.getName().matches("^" + middleware.getName() + LINE + "[0-9]+" + LINE + POD + LINE + TCP
                            + LINE + ".+" + "$")) {
                        serviceList.addAll(ing.getServiceList());
                    } else {
                        tempIngressList.add(ing);
                    }
                }
                // 对serviceList进行排序， 将名称带有sentinel字段的排在第一个
                CollectionUtil.sort(serviceList, (s1, s2) -> {
                    String pattern = "^" + middleware.getName() + LINE + SENTINEL + "$";
                    if (s1.getServiceName().matches(pattern)) {
                        return -1;
                    }
                    if (s2.getServiceName().matches(pattern)) {
                        return 1;
                    }
                    // 对其余字符串进行字典顺序排序
                    return Comparator.comparing(ServiceDTO::getServiceName).compare(s1, s2);
                });
                ingressDTO.setServiceList(serviceList);
                // 设置是否包含跳过冲突端口
                if (values.containsKey(SKIP_PORT_CONFLICT)) {
                    ingressDTO.setSkipPortConflict(values.getBoolean(SKIP_PORT_CONFLICT));
                }
                ingressDTO.setServicePurpose("哨兵");

                ingressDTOList.clear();
                ingressDTOList.addAll(tempIngressList);
                ingressDTOList.add(ingressDTO);
                return;
            }

            // 集群模式服务暴露处理
            if (ingressDTOList.stream().anyMatch(ingressDTO -> ingressDTO.getName().matches("^" + middleware.getName()
                    + LINE + "[0-9]+" + LINE + POD + LINE + "16379" + LINE + TCP + LINE + ".+" + "$"))) {
                IngressDTO ingressDTO = ingressDTOList.get(0);
                ingressDTO.setExternalEnable(true);
                ingressDTOList.remove(0);
                for (IngressDTO ing : ingressDTOList) {
                    ingressDTO.getServiceList().addAll(ing.getServiceList());
                }
                if (values.containsKey(SKIP_PORT_CONFLICT)) {
                    ingressDTO.setSkipPortConflict(values.getBoolean(SKIP_PORT_CONFLICT));
                }
                ingressDTOList.clear();
                ingressDTOList.add(ingressDTO);
            }
        }
    }

    public Integer findNextExposePort(Set<Integer> usedPortSet, Integer exposePort){
        if (usedPortSet.contains(exposePort)){
            exposePort++;
            findNextExposePort(usedPortSet, exposePort);
        }
        return exposePort;
    }
}
