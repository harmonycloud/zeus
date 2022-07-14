package com.harmonycloud.zeus.operator.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.*;
import static com.harmonycloud.caas.common.constants.NameConstant.MEMORY;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_EXPOSE_INGRESS;


import com.alibaba.fastjson.JSON;
import com.harmonycloud.caas.common.enums.Protocol;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.service.k8s.IngressComponentService;
import com.harmonycloud.zeus.service.k8s.ServiceService;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareServiceImpl;
import com.harmonycloud.zeus.util.ServiceNameConvertUtil;
import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.operator.api.RedisOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractRedisOperator;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 处理redis逻辑
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
        List<ServicePortDTO> servicePortDTOList = serviceService.list(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType());
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
        boolean successCreateService = false;
        int servicePort = 31000;
        while (!successCreateService) {
            serviceDTO.setExposePort(String.valueOf(servicePort));
            log.info("使用端口{}暴露服务", servicePort);
            try {
                ingressService.create(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), ingressDTO);
                successCreateService = true;
            } catch (Exception e) {
                log.error("使用ingress暴露服务出错了,端口：{}", servicePort, e);
                servicePort++;
                successCreateService = false;
            }
        }
    }


    private void executeCreateOpenService(Middleware middleware, MiddlewareServiceNameIndex middlewareServiceNameIndex) {
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
        MiddlewareServiceNameIndex middlewareServiceNameIndex = ServiceNameConvertUtil.convertRedis(middleware.getName());
        for (int i = 0; i < (60 * 10 * 60) && !success; i++) {
            Middleware detail = middlewareService.detail(middleware.getClusterId(), middleware.getNamespace(), middleware.getName(), middleware.getType());
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
            if (middleware.getReadWriteProxy() != null && middleware.getReadWriteProxy().getEnabled()){
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
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        convertRegistry(middleware,cluster);

        // 处理redis特有参数
        if (values != null) {
            middleware.setPassword(values.getString("redisPassword")).setPort(values.getInteger("port"));
            JSONObject redisQuota = values.getJSONObject(REDIS);
            convertResourcesByHelmChart(middleware, middleware.getType(), redisQuota.getJSONObject(RESOURCES));
            middleware.getQuota().get(middleware.getType()).setNum(redisQuota.getInteger(REPLICAS));
            if (SENTINEL.equals(middleware.getMode())) {
                JSONObject sentinelQuota = values.getJSONObject(SENTINEL);
                convertResourcesByHelmChart(middleware, SENTINEL, sentinelQuota.getJSONObject("resources"));
                middleware.getQuota().get(SENTINEL).setNum(sentinelQuota.getInteger(REPLICAS));
                Integer num = redisQuota.getInteger(REPLICAS);
                if (middleware.getReadWriteProxy() != null && middleware.getReadWriteProxy().getEnabled()) {
                    num = num / 2;
                }
                middleware.getQuota().get(middleware.getType()).setNum(num);
            }
        }

        return middleware;
    }

    @Override
    public void update(Middleware middleware, MiddlewareClusterDTO cluster) {
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
                sb.append("redis.replicas=").append(quota.getNum()).append(",");
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
                sb.append("sentinel.resources.requests.cpu=").append(sentinelQuota.getCpu()).append(",sentinel.resources.limits.cpu=")
                        .append(sentinelQuota.getLimitCpu()).append(",");
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
    public void editConfigMapData(CustomConfig customConfig, List<String> data){
        for (int i = 0; i < data.size(); ++i) {
            if (data.get(i).contains(customConfig.getName())) {
                String temp = StringUtils.substring(data.get(i), data.get(i).indexOf(" ") + 1, data.get(i).length());
                String test = data.get(i).replace(" ", "").replace(temp, "");
                if (data.get(i).replace(" ", "").replace(temp, "").equals(customConfig.getName())){
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
    public void replaceReadWriteProxyValues(ReadWriteProxy readWriteProxy, JSONObject values){
        JSONObject predixy = values.getJSONObject("predixy");
        predixy.put("enableProxy", readWriteProxy.getEnabled());

        JSONObject requests = new JSONObject();
        JSONObject limits = new JSONObject();

        requests.put(CPU, "200m");
        requests.put(MEMORY, "512Mi");
        limits.put(CPU, "200m");
        limits.put(MEMORY, "512Mi");
        predixy.put("replicas", 3);

        JSONObject resources = predixy.getJSONObject("resources");
        resources.put("requests", requests);
        resources.put("limits", limits);
    }
}
