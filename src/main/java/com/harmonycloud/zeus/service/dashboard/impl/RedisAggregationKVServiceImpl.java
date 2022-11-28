package com.harmonycloud.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.dashboard.RedisClient;
import com.harmonycloud.zeus.service.dashboard.RedisKVService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.util.K8sServiceNameUtil;
import com.harmonycloud.zeus.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/11/23 11:24 上午
 */
@Service("RedisAggregationKVServiceImpl")
public class RedisAggregationKVServiceImpl implements RedisKVService {

    @Autowired
    private RedisClient redisClient;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;

    @Override
    public JSONArray getKeys(String clusterId, String namespace, String middlewareName, Integer db) {
        List<String> paths = listServicePath(clusterId, namespace, middlewareName);
        JSONArray jsonArray = new JSONArray();
        paths.forEach(path -> {
            jsonArray.addAll(this.getKeys(path, db));
        });
        return jsonArray;
    }

    @Override
    public JSONArray getKeysWithPattern(String clusterId, String namespace, String middlewareName, Integer db, String keyword) {
        List<String> paths = listServicePath(clusterId, namespace, middlewareName);
        JSONArray jsonArray = new JSONArray();
        paths.forEach(path -> {
            jsonArray.addAll(getKeysWithPattern(path, db, keyword));
        });
        return jsonArray;
    }

    public JSONArray getKeys(String host, Integer db) {
        JSONObject res = redisClient.getAllKeys(host, db);
        return RedisUtil.convertResult(res);
    }

    public JSONArray getKeysWithPattern(String host, Integer db, String keyword) {
        JSONObject res = redisClient.getKeys(host, db, keyword);
        return RedisUtil.convertResult(res);
    }

    private List<String> listServicePath(String clusterId, String namespace, String middlewareName) {
        JSONObject installedValues = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        String deployMod = RedisUtil.getRedisDeployMod(installedValues);
        if (deployMod.equalsIgnoreCase("sentinelProxy")) {
            return middlewareService.listMiddlewareService(clusterId, namespace, MiddlewareTypeEnum.REDIS.getType(), middlewareName).
                    stream().filter(middlewareInfo -> middlewareInfo.getName().contains("shard") && !middlewareInfo.getName().contains("readonly")).
                    collect(Collectors.toList()).stream().sorted(new MiddlewareInfoComparator()).map(middlewareInfo -> K8sServiceNameUtil.getServicePath(namespace, middlewareInfo.getName())).collect(Collectors.toList());
        } else {
            return middlewareService.listMiddlewarePod(clusterId, namespace, MiddlewareTypeEnum.REDIS.getType(), middlewareName).
                    stream().filter(middlewareInfo -> middlewareInfo.getType().equals("master")).collect(Collectors.toList()).
                    stream().sorted(new MiddlewareInfoComparator()).
                    map(middlewareInfo -> K8sServiceNameUtil.getServicePath(middlewareInfo.getName(), namespace, middlewareName)).collect(Collectors.toList());
        }
    }

    public static class MiddlewareInfoComparator implements Comparator<MiddlewareInfo> {
        @Override
        public int compare(MiddlewareInfo o1, MiddlewareInfo o2) {
            return o1.getName() == null ? 1
                    : o2.getName() == null ? 1 : o1.getName().compareTo(o2.getName());
        }
    }

}
