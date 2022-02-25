package com.harmonycloud.zeus.operator.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.CLUSTER;
import static com.harmonycloud.caas.common.constants.NameConstant.REDIS;
import static com.harmonycloud.caas.common.constants.NameConstant.REPLICAS;
import static com.harmonycloud.caas.common.constants.NameConstant.RESOURCES;
import static com.harmonycloud.caas.common.constants.NameConstant.SENTINEL;
import static com.harmonycloud.caas.common.constants.NameConstant.TYPE;


import com.harmonycloud.caas.common.model.middleware.CustomConfig;
import io.fabric8.kubernetes.api.model.ConfigMap;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareQuota;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.operator.api.RedisOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractRedisOperator;
import com.harmonycloud.tool.encrypt.PasswordUtils;

import java.util.*;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 处理redis逻辑
 */
@Operator(paramTypes4One = Middleware.class)
public class RedisOperatorImpl extends AbstractRedisOperator implements RedisOperator {

    @Override
    public void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 替换通用的值
        replaceCommonValues(middleware, cluster, values);

        // 资源配额
        MiddlewareQuota redisQuota = middleware.getQuota().get(middleware.getType());
        JSONObject redis = values.getJSONObject(REDIS);
        replaceCommonResources(redisQuota, redis.getJSONObject(RESOURCES));
        replaceCommonStorages(redisQuota, values);
        redis.put(REPLICAS, redisQuota.getNum());
        if (SENTINEL.equals(middleware.getMode())) {
            JSONObject sentinel = values.getJSONObject(SENTINEL);
            MiddlewareQuota sentinelQuota = middleware.getQuota().get(SENTINEL);
            if (sentinelQuota != null) {
                replaceCommonResources(sentinelQuota, sentinel.getJSONObject(RESOURCES));
                if (sentinelQuota.getNum() != null) {
                    sentinel.put(REPLICAS, sentinelQuota.getNum());
                }
            }
            values.put(TYPE, SENTINEL);
        } else {
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

        // 实例扩容
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
}
