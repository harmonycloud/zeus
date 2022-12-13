package com.middleware.zeus.operator.impl;

import static com.middleware.caas.common.constants.NameConstant.*;
import static com.middleware.caas.common.constants.NameConstant.MEMORY;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_EXPOSE_INGRESS;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.NODE_AFFINITY;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.PREDIXY;


import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.Protocol;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.model.AffinityDTO;
import com.middleware.caas.common.model.IngressComponentDto;
import com.middleware.caas.common.model.MiddlewareServiceNameIndex;
import com.middleware.tool.collection.JsonUtils;
import com.middleware.tool.numeric.ResourceCalculationUtil;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.service.k8s.IngressComponentService;
import com.middleware.zeus.service.k8s.K8sExecService;
import com.middleware.zeus.service.k8s.PodService;
import com.middleware.zeus.service.k8s.ServiceService;
import com.middleware.zeus.service.middleware.impl.MiddlewareServiceImpl;
import com.middleware.zeus.util.K8sConvert;
import com.middleware.zeus.util.RedisUtil;
import com.middleware.zeus.util.ServiceNameConvertUtil;
import com.middleware.caas.common.model.middleware.*;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.NodeAffinity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.operator.api.RedisOperator;
import com.middleware.zeus.operator.miiddleware.AbstractRedisOperator;
import com.middleware.tool.encrypt.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dengyulong
 * @date 2021/03/23 处理redis逻辑
 */
@Slf4j
@Operator(paramTypes4One = Middleware.class)
public class RedisOperatorImpl extends AbstractRedisOperator implements RedisOperator {

    @Autowired
    private IngressComponentService ingressComponentService;
    @Autowired
    private MiddlewareServiceImpl middlewareService;
    @Autowired
    private ServiceService serviceService;
    @Autowired
    private K8sExecService k8sExecService;
    @Autowired
    private PodService podService;

    public void createIngressService(Middleware middleware) {
        List<IngressComponentDto> ingressComponentList = ingressComponentService.list(middleware.getClusterId());
        if (CollectionUtils.isEmpty(ingressComponentList)) {
            return;
        }
        IngressComponentDto ingressComponentDto = ingressComponentList.get(0);
        String ingressClassName = ingressComponentDto.getIngressClassName();
        IngressDTO ingressDTO = new IngressDTO();
        ingressDTO.setIngressClassName(ingressClassName);
        ingressDTO.setExposeType(MIDDLEWARE_EXPOSE_INGRESS);
        ingressDTO.setProtocol(Protocol.TCP.getValue());
        ingressDTO.setMiddlewareType(middleware.getType());
        List<ServicePortDTO> servicePortDTOList = serviceService
            .list(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType())
            .stream().filter(servicePortDTO -> {
                return !(servicePortDTO.getServiceName().contains("readonly")
                    || servicePortDTO.getServiceName().contains("sentinel"));
            }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(servicePortDTOList)) {
            return;
        }
        ServicePortDTO servicePortDTO = servicePortDTOList.get(0);
        if (CollectionUtils.isEmpty(servicePortDTO.getPortDetailDtoList())) {
            return;
        }
        // 设置需要暴露的服务信息
        PortDetailDTO portDetailDTO = servicePortDTO.getPortDetailDtoList().get(0);
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setTargetPort(portDetailDTO.getTargetPort());
        serviceDTO.setServicePort(portDetailDTO.getPort());
        serviceDTO.setServiceName(servicePortDTO.getServiceName());
        List<ServiceDTO> serviceDTOS = new ArrayList<>();
        serviceDTOS.add(serviceDTO);

        ingressDTO.setServiceList(serviceDTOS);
        int servicePort = ingressService.getAvailablePort(middleware.getClusterId(), ingressClassName);
        serviceDTO.setExposePort(String.valueOf(servicePort));
        ingressService.create(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), ingressDTO);
    }

    private void executeCreateOpenService(Middleware middleware,
        MiddlewareServiceNameIndex middlewareServiceNameIndex) {
        List<IngressComponentDto> ingressComponentList = ingressComponentService.list(middleware.getClusterId());
        log.info("开始为{}创建对外服务，参数：{}", middleware.getName(), middleware);
        if (CollectionUtils.isEmpty(ingressComponentList)) {
            log.info("不存在ingress，使用NodePort暴露服务");
            super.createOpenService(middleware, middlewareServiceNameIndex);
        } else {
            log.info("存在ingress，使用ingress暴露服务");
            createIngressService(middleware);
        }
    }

    public void createOpenService(Middleware middleware) {
        boolean success = false;
        ReadWriteProxy readWriteProxy = middleware.getReadWriteProxy();
        String mod = "proxy";
        if ("sentinel".equals(middleware.getMode()) && !readWriteProxy.getEnabled()) {
            mod = "sentinel";
        }
        MiddlewareServiceNameIndex middlewareServiceNameIndex = ServiceNameConvertUtil.convertRedis(middleware.getName(), mod);

        for (int i = 0; i < (60 * 10 * 60) && !success; i++) {
            Middleware detail = middlewareService.detail(middleware.getClusterId(), middleware.getNamespace(),
                middleware.getName(), middleware.getType());
            log.info("为实例：{}创建对外服务：状态：{},已用时：{}s", detail.getName(), detail.getStatus(), i);
            if (detail != null) {
                if (detail.getStatus() != null && "Running".equals(detail.getStatus())) {
                    executeCreateOpenService(middleware, middlewareServiceNameIndex);
                    success = true;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 替换通用的值
        replaceCommonValues(middleware, cluster, values);

        // 资源配额
        MiddlewareQuota redisQuota = middleware.getQuota().get(middleware.getType());
        JSONObject redis = values.getJSONObject(REDIS);
        replaceCommonResources(redisQuota, redis.getJSONObject(RESOURCES));
        replaceCommonStorages(redisQuota, values);
        if (SENTINEL.equals(middleware.getMode())) {
            JSONObject sentinel = values.getJSONObject(SENTINEL);
            MiddlewareQuota sentinelQuota = middleware.getQuota().get(SENTINEL);
            if (sentinelQuota != null) {
                replaceCommonResources(sentinelQuota, sentinel.getJSONObject(RESOURCES));
                if (sentinelQuota.getNum() != null) {
                    sentinel.put(REPLICAS, sentinelQuota.getNum());
                }
            }
            Integer num = redisQuota.getNum();
            if (middleware.getReadWriteProxy() != null && middleware.getReadWriteProxy().getEnabled()) {
                num = num / 2;
            }
            redis.put(REPLICAS, num);
            values.put(TYPE, SENTINEL);
        } else {
            redis.put(REPLICAS, redisQuota.getNum());
            values.put(TYPE, CLUSTER);
        }
        // 计算pod最大内存
        String mem = calculateMem(redisQuota.getLimitMemory(), "0.8", "mb");
        values.put("redisMaxMemory", mem);

        // 密码
        if (StringUtils.isBlank(middleware.getPassword())) {
            middleware.setPassword(PasswordUtils.generateCommonPassword(10));
        }
        values.put("redisPassword", middleware.getPassword());
        // 端口
        if (middleware.getPort() != null) {
            values.put("redisServicePort", middleware.getPort());
        }

        //主机网络配置
        if (middleware.getRedisParam() != null && middleware.getRedisParam().getHostNetwork() != null) {
            values.getJSONObject("redis").put("hostNetwork", middleware.getRedisParam().getHostNetwork());
            values.getJSONObject("sentinel").put("hostNetwork", middleware.getRedisParam().getHostNetwork());
            values.getJSONObject("predixy").put("hostNetwork", middleware.getRedisParam().getHostNetwork());
        }

        // 设置双活参数
        checkAndSetActiveActive(values, middleware);
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        convertRegistry(middleware, cluster);

        // 处理redis特有参数
        if (values != null) {
            if (checkUserAuthority(MiddlewareTypeEnum.REDIS.getType())){
                middleware.setPassword(values.getString("redisPassword"));
            }
            middleware.setPort(values.getInteger("port"));
            JSONObject redisQuota = values.getJSONObject(REDIS);
            convertResourcesByHelmChart(middleware, middleware.getType(), redisQuota.getJSONObject(RESOURCES));
            middleware.getQuota().get(middleware.getType()).setNum(redisQuota.getInteger(REPLICAS));
            // 读写分离
            if (values.containsKey("predixy")) {
                ReadWriteProxy readWriteProxy = new ReadWriteProxy();
                readWriteProxy.setEnabled(values.getJSONObject("predixy").getBoolean("enableProxy"));
                middleware.setReadWriteProxy(readWriteProxy);
            } else {
                ReadWriteProxy readWriteProxy = new ReadWriteProxy();
                readWriteProxy.setEnabled(false);
                middleware.setReadWriteProxy(readWriteProxy);
            }
            // 哨兵
            if (SENTINEL.equals(middleware.getMode())) {
                JSONObject sentinelQuota = values.getJSONObject(SENTINEL);
                convertResourcesByHelmChart(middleware, SENTINEL, sentinelQuota.getJSONObject("resources"));
                middleware.getQuota().get(SENTINEL).setNum(sentinelQuota.getInteger(REPLICAS));
                Integer num = redisQuota.getInteger(REPLICAS);
                if (middleware.getReadWriteProxy() != null && middleware.getReadWriteProxy().getEnabled()) {
                    num = num * 2;
                }
                middleware.getQuota().get(middleware.getType()).setNum(num);
            }
        }

        // 主机网络配置
        RedisParam redisParam = new RedisParam();
        JSONObject predixy = values.getJSONObject("predixy");
        if (predixy != null) {
            redisParam.setHostNetwork(predixy.getBoolean("hostNetwork"));
        }
        middleware.setRedisParam(redisParam);

        return middleware;
    }

    @Override
    public void update(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values =
            helmChartService.getInstalledValues(middleware.getName(), middleware.getNamespace(), cluster);
        if (cluster == null) {
            cluster = clusterService.findById(middleware.getClusterId());
        }
        StringBuilder sb = new StringBuilder();

        // redis实例扩容
        if (middleware.getQuota() != null && middleware.getQuota().get(middleware.getType()) != null) {
            MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
            // 设置limit的resources
            setLimitResources(quota);
            // 实例规格扩容
            // cpu
            if (StringUtils.isNotBlank(quota.getCpu())) {
                sb.append("redis.resources.requests.cpu=").append(quota.getCpu()).append(",redis.resources.limits.cpu=")
                    .append(quota.getLimitCpu()).append(",");
            }
            // memory
            if (StringUtils.isNotBlank(quota.getMemory())) {
                sb.append("redis.resources.requests.memory=").append(quota.getMemory())
                    .append(",redis.resources.limits.memory=").append(quota.getLimitMemory()).append(",");
                // 计算pod最大内存
                String mem = calculateMem(quota.getLimitMemory(), "0.8", "mb");
                sb.append("redisMaxMemory=").append(mem).append(",");
            }
            // 实例模式扩容
            if (quota.getNum() != null) {
                if ("sentinel".equals(values.getString("mode")) && values.containsKey("predixy")
                    && values.getJSONObject("predixy").getBoolean("enableProxy")) {
                    sb.append("redis.replicas=").append(quota.getNum() / 2).append(",");
                } else {
                    sb.append("redis.replicas=").append(quota.getNum()).append(",");
                }
            }
        }

        // sentinel实例扩容
        if (middleware.getQuota() != null && middleware.getQuota().get(SENTINEL) != null) {
            MiddlewareQuota sentinelQuota = middleware.getQuota().get(SENTINEL);
            // 设置limit的resources
            setLimitResources(sentinelQuota);
            // 实例规格扩容
            // cpu
            if (StringUtils.isNotBlank(sentinelQuota.getCpu())) {
                sb.append("sentinel.resources.requests.cpu=").append(sentinelQuota.getCpu())
                    .append(",sentinel.resources.limits.cpu=").append(sentinelQuota.getLimitCpu()).append(",");
            }
            // memory
            if (StringUtils.isNotBlank(sentinelQuota.getMemory())) {
                sb.append("sentinel.resources.requests.memory=").append(sentinelQuota.getMemory())
                    .append(",sentinel.resources.limits.memory=").append(sentinelQuota.getLimitMemory()).append(",");
            }
            // 实例模式扩容
            if (sentinelQuota.getNum() != null) {
                sb.append("sentinel.replicas=").append(sentinelQuota.getNum()).append(",");
            }
        }

        // 密码
        if (StringUtils.isNotBlank(middleware.getPassword())) {
            sb.append("redisPassword=").append(middleware.getPassword()).append(",");
        }

        // 更新通用字段
        super.updateCommonValues(sb, middleware);

        // 没有修改，直接返回
        if (sb.length() == 0) {
            return;
        }
        // 去掉末尾的逗号
        sb.deleteCharAt(sb.length() - 1);
        // 更新helm
        helmChartService.upgrade(middleware, sb.toString(), cluster);
    }

    @Override
    public List<String> getConfigmapDataList(ConfigMap configMap) {
        return new ArrayList<>(Arrays.asList(configMap.getData().get("redis.conf").split("\n")));
    }

    @Override
    public void editConfigMapData(CustomConfig customConfig, List<String> data) {
        for (int i = 0; i < data.size(); ++i) {
            if (data.get(i).contains(customConfig.getName())) {
                String temp = StringUtils.substring(data.get(i), data.get(i).indexOf(" ") + 1, data.get(i).length());
                String test = data.get(i).replace(" ", "").replace(temp, "");
                if (data.get(i).replace(" ", "").replace(temp, "").equals(customConfig.getName())) {
                    data.set(i, data.get(i).replace(temp, customConfig.getValue()));
                }
            }
        }
    }

    /**
     * 转换data为map形式
     */
    @Override
    public Map<String, String> configMap2Data(ConfigMap configMap) {
        String dataString = configMap.getData().get("redis.conf");
        Map<String, String> dataMap = new HashMap<>();
        String[] datalist = dataString.split("\n");
        for (String data : datalist) {
            // 特殊处理
            if (data.contains("slaveof")) {
                dataMap.put("slaveof", data.replace("slaveof ", " "));
                continue;
            }
            if ("save".equals(data.split(" ")[0])) {
                dataMap.put("save", data.replace("save ", ""));
                continue;
            }
            if (data.contains("client-output-buffer-limit")) {
                dataMap.put("client-output-buffer-limit", data.replace("client-output-buffer-limit ", ""));
                continue;
            }
            String[] keyValue = data.split(" ");
            dataMap.put(keyValue[0], keyValue[1]);
        }
        return dataMap;
    }

    /**
     * 转换data为map形式
     */
    @Override
    public void updateConfigData(ConfigMap configMap, List<String> data) {
        // 构造新configmap
        StringBuilder temp = new StringBuilder();
        for (String str : data) {
            {
                temp.append(str).append("\n");
            }
        }
        configMap.getData().put("redis.conf", temp.toString());
    }

    @Override
    public void replaceReadWriteProxyValues(Middleware middleware, JSONObject values) {
        ReadWriteProxy readWriteProxy = middleware.getReadWriteProxy();
        JSONObject predixy = new JSONObject();
        predixy.put("enableProxy", readWriteProxy.getEnabled());

        JSONObject requests = new JSONObject();
        JSONObject limits = new JSONObject();

        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        String memory = calculateProxyResource(quota.getMemory().replace("Gi", ""));
        if (Double.parseDouble(memory) < 0.256) {
            memory = String.valueOf(0.256);
        } else if (Double.parseDouble(memory) > 2) {
            memory = String.valueOf(2);
        }

        requests.put(CPU, "1");
        requests.put(MEMORY, memory + "Gi");
        limits.put(CPU, "1");
        limits.put(MEMORY, memory + "Gi");
        Integer num = quota.getNum();
        predixy.put("replicas", num / 2 == 1 ? num : num / 2);

        JSONObject resources = new JSONObject();
        resources.put("requests", requests);
        resources.put("limits", limits);

        predixy.put("resources", resources);
        values.put("predixy", predixy);
    }

    @Override
    protected void replaceNodeAffinity(Middleware middleware, JSONObject values) {
        JSONObject affinity;
        if (values.containsKey(REDIS) && values.getJSONObject(REDIS).containsKey(NODE_AFFINITY)) {
            affinity = values.getJSONObject(REDIS).getJSONObject(NODE_AFFINITY);
        } else if (values.containsKey(NODE_AFFINITY)) {
            affinity = values.getJSONObject(NODE_AFFINITY);
        } else {
            affinity = new JSONObject();
            values.put(NODE_AFFINITY, affinity);
        }
        if (!CollectionUtils.isEmpty(middleware.getNodeAffinity())) {
            // convert to k8s model
            JSONObject nodeAffinity = K8sConvert.convertNodeAffinity2Json(middleware.getNodeAffinity());
            if (nodeAffinity != null) {
                affinity.putAll(nodeAffinity);
            }
        }
    }

    @Override
    public void convertNodeAffinity(Middleware middleware, JSONObject values) {
        JSONObject affinity;
        if (values.containsKey(REDIS) && values.getJSONObject(REDIS).containsKey(NODE_AFFINITY)) {
            affinity = values.getJSONObject(REDIS).getJSONObject("nodeAffinity");
        } else if (JsonUtils.isJsonObject(values.getString("nodeAffinity"))) {
            affinity = values.getJSONObject("nodeAffinity");
        } else {
            return;
        }
        if (!CollectionUtils.isEmpty(affinity)) {
            List<AffinityDTO> dto = K8sConvert.convertNodeAffinity(
                JSONObject.parseObject(affinity.toJSONString(), NodeAffinity.class), AffinityDTO.class);
            middleware.setNodeAffinity(dto);
        }
    }

    @Override
    public void checkAndSetActiveActive(JSONObject values, Middleware middleware) {
        if (namespaceService.checkAvailableDomain(middleware.getClusterId(), middleware.getNamespace())) {
            super.setActiveActiveConfig("redis", values);
            super.setActiveActiveToleration(middleware, values);
        }
    }

    @Override
    public Double calculateCpuRequest(JSONObject values) {
        double cpu = 0.0;
        double sentinelCpu = 0.0;
        JSONObject resources = values.getJSONObject(REDIS).getJSONObject(RESOURCES);
        JSONObject request = resources.getJSONObject("requests");
        cpu = ResourceCalculationUtil.getResourceValue(request.getString(CPU), CPU, "") * getReplicas(values);

        if (SENTINEL.equals(values.getString(MODE))) {
            JSONObject sentinel = values.getJSONObject(SENTINEL);
            Integer sentinelNum = sentinel.getInteger(REPLICAS);
            JSONObject sentinelResources = values.getJSONObject(SENTINEL).getJSONObject(RESOURCES);
            JSONObject sentinelRequest = sentinelResources.getJSONObject("requests");
            sentinelCpu = ResourceCalculationUtil.getResourceValue(sentinelRequest.getString(CPU), CPU, "") * sentinelNum;
            cpu += sentinelCpu;
        }
        return cpu;
    }

    @Override
    public Integer getReplicas(JSONObject values) {
        JSONObject redis = values.getJSONObject(REDIS);
        Integer num = redis.getInteger(REPLICAS);
        if (SENTINEL.equals(values.getString(MODE)) && values.containsKey(PREDIXY) &&
                values.getJSONObject(PREDIXY).getBooleanValue("enableProxy")) {
            num *= 2;
        }
        return num;
    }

    @Override
    public void switchMiddleware(Middleware middleware, String slaveName) {
        MiddlewareClusterDTO cluster = clusterService.findById(middleware.getClusterId());
        // 获取数据库密码
        JSONObject values = helmChartService.getInstalledValues(middleware.getName(), middleware.getNamespace(), cluster);
        String password = values.getString("redisPassword");
        // 获取端口
        String port = values.getString("redisServicePort");
        MiddlewareCR cr = middlewareCRService.getCR(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());

        //获取从节点信息
        JSONObject status = JSONObject.parseObject(cr.getMetadata().getAnnotations().get("status"));
        JSONArray conditions = status.getJSONArray("conditions");
        if (CollectionUtil.isEmpty(conditions)) {
            throw new BusinessException(DictEnum.POD, ErrorMessage.NOT_FOUND);
        }
        JSONObject slavePod = null;
        for (Object condition : conditions) {
            JSONObject con = (JSONObject) condition;
            if (slaveName.equals(con.getString("name")) && "slave".equals(con.getString("type"))) {
                slavePod = con;
                break;
            }
        }
        if (slavePod == null) {
            throw new BusinessException(ErrorMessage.NODE_NOT_FOUND);
        }
        // 获取slaveIP
        String slaveIP = slavePod.getString("instance").split(":")[0];
        //从节点执行命令
        String execCommand = MessageFormat.format(
                "kubectl exec {0} -n {1} -c redis-cluster --server={2} --token={3} --insecure-skip-tls-verify=true " +
                        "-- bash -c \"redis-cli -h {4} -a {5} cluster failover\"",
                slaveName, middleware.getNamespace(), cluster.getAddress(), cluster.getAccessToken(),
                slaveIP, password);
        k8sExecService.exec(execCommand);

    }

    @Override
    public List<IngressDTO> listHostNetworkAddress(String clusterId, String namespace, String middlewareName, String type) {
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        if (values == null) {
            return Collections.emptyList();
        }
        JSONObject redis = values.getJSONObject("redis");
        if (redis != null && redis.containsKey("hostNetwork") && values.containsKey("type") && redis.getBoolean("hostNetwork")) {
            List<PodInfo> podInfoList = podService.listMiddlewarePods(clusterId, namespace, middlewareName, MiddlewareTypeEnum.REDIS.getType());
            String deployMod = RedisUtil.getRedisDeployMod(values);
            switch (deployMod) {
                case "cluster":
                    break;
                case "clusterProxy":
                case "sentinelProxy":
                    podInfoList = podInfoList.stream().filter(podInfo -> "proxy".equals(podInfo.getRole())).collect(Collectors.toList());
                    break;
                case "sentinel":
                    podInfoList = podInfoList.stream().filter(podInfo -> "sentinel".equals(podInfo.getRole())).collect(Collectors.toList());
                    break;
            }
            return podInfoList.stream().map(podInfo -> {
                IngressDTO ingressDTO = new IngressDTO();
                ingressDTO.setServicePurpose(podInfo.getPodName());
                ingressDTO.setExposeIP(podInfo.getHostIp());
                ingressDTO.setExposePort(RedisUtil.getServicePort(podInfo.getRole()));
                return ingressDTO;
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
