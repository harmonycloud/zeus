package com.middleware.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.model.dashboard.redis.ScanResult;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;
import com.middleware.zeus.integration.dashboard.RedisClient;
import com.middleware.zeus.service.dashboard.RedisKVService;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.middleware.MiddlewareService;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.util.K8sServiceNameUtil;
import com.middleware.zeus.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Value("${system.middleware-api.redis.port:6379}")
    private String port;

    @Override
    public JSONArray getKeys(String clusterId, String namespace, String middlewareName, Integer db) {
        List<String> paths = listServicePath(clusterId, namespace, middlewareName, true);
        JSONArray jsonArray = new JSONArray();
        paths.forEach(path -> {
            jsonArray.addAll(this.getKeys(path, db));
        });
        return jsonArray;
    }

    @Override
    public JSONArray getKeysWithPattern(String clusterId, String namespace, String middlewareName, Integer db, String keyword) {
        List<String> paths = listServicePath(clusterId, namespace, middlewareName, true);
        JSONArray jsonArray = new JSONArray();
        paths.forEach(path -> {
            jsonArray.addAll(getKeysWithPattern(path, db, keyword));
        });
        return jsonArray;
    }

    @Override
    public Integer dbSize(String clusterId, String namespace, String middlewareName, Integer db) {
        List<String> paths = listServicePath(clusterId, namespace, middlewareName, false);
        AtomicInteger dbSize = new AtomicInteger(0);
        paths.forEach(path -> {
            try {
                dbSize.getAndSet(redisClient.DBSize(path, port, db).getInteger("data") + dbSize.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return dbSize.get();
    }

    @Override
    public ScanResult scan(String clusterId, String namespace, String middlewareName, Integer db, String keyword, Integer cursor, Integer count, String pod) {
        // 哨兵代理，集群，集群代理
        List<String> servicePaths = listServicePath(clusterId, namespace, middlewareName, true);
        boolean startScan = StringUtils.isEmpty(pod);
        ScanResult scanResult = new ScanResult();
        scanResult.setKeys(new ArrayList<>());
        for (String servicePath : servicePaths) {
            boolean continueScan = true;
            if (StringUtils.isEmpty(pod)) {
                continueScan = scanAndConvert(servicePath, db, keyword, cursor, count, scanResult);
            } else {
                if (!startScan) {
                    if (servicePath.equals(pod)) {
                        startScan = true;
                        continueScan = scanAndConvert(servicePath, db, keyword, cursor, count, scanResult);
                    }
                } else {
                    continueScan = scanAndConvert(servicePath, db, keyword, cursor, count, scanResult);
                }
            }
            if (startScan) {
                cursor = 0;
            }
            if (!continueScan) {
                break;
            }
        }
        return scanResult;
    }

    @Override
    public JSONObject execCMD(String clusterId, String namespace, String middlewareName, Integer db, String cmd) {
        String firstCMD = cmd.trim().split(" ")[0];
        if (firstCMD.equalsIgnoreCase("keys")) {
            return execKeysCMD(clusterId, namespace, middlewareName, db, cmd);
        } else if(firstCMD.equalsIgnoreCase("dbsize")){
            return execDBSizeCMD(clusterId, namespace, middlewareName, db);
        }else{
            return redisClient.execCMD(K8sServiceNameUtil.getServicePath(namespace, middlewareName), port, db, cmd);
        }
    }

    private JSONObject execKeysCMD(String clusterId, String namespace, String middlewareName, Integer db, String cmd) {
        List<String> paths = listServicePath(clusterId, namespace, middlewareName, true);
        JSONObject res = new JSONObject();
        List<String> keys = new ArrayList<>();
        AtomicInteger time = new AtomicInteger();
        paths.forEach(path -> {
            try {
                JSONObject resObj = redisClient.execCMD(K8sServiceNameUtil.getServicePath(namespace, middlewareName), port, db, cmd);
                JSONArray data = resObj.getJSONArray("data");
                time.getAndAdd(resObj.getInteger("execTime"));
                keys.addAll(data.stream().map(Object::toString).collect(Collectors.toList()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        res.put("execTime", time.get());
        res.put("data", keys);
        return res;
    }

    private JSONObject execDBSizeCMD(String clusterId, String namespace, String middlewareName, Integer db) {
        Integer size = this.dbSize(clusterId, namespace, middlewareName, db);
        JSONObject res = new JSONObject();
        res.put("execTime", "0");
        res.put("data", size);
        return res;
    }

    private boolean scanAndConvert(String pod, Integer db, String keyword, Integer cursor, Integer count, ScanResult scanResult) {
        JSONObject res = redisClient.scan(pod, port, db, keyword, cursor, count - scanResult.getKeys().size());
        ScanResult result = RedisUtil.convertScanResult(res);
        scanResult.getKeys().addAll(result.getKeys());
        scanResult.setPod(pod);
        scanResult.setCursor(result.getCursor());
        return scanResult.getKeys().size() != count;
    }

    public JSONArray getKeys(String host, Integer db) {
        JSONObject res = redisClient.getAllKeys(host, port, db);
        return RedisUtil.convertResult(res);
    }

    public JSONArray getKeysWithPattern(String host, Integer db, String keyword) {
        JSONObject res = redisClient.getKeys(host, port, db, keyword);
        return RedisUtil.convertResult(res);
    }

    private List<String> listServicePath(String clusterId, String namespace, String middlewareName, Boolean aggregateQuery) {
        JSONObject installedValues = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        String deployMod = RedisUtil.getRedisDeployMod(installedValues);
        if (deployMod.equalsIgnoreCase("sentinelProxy")) {
            return middlewareService.listMiddlewareService(clusterId, namespace, MiddlewareTypeEnum.REDIS.getType(), middlewareName).
                    stream().filter(middlewareInfo -> middlewareInfo.getName().contains("shard") && !middlewareInfo.getName().contains("readonly")).
                    collect(Collectors.toList()).stream().sorted(new MiddlewareInfoComparator()).map(middlewareInfo -> K8sServiceNameUtil.getServicePath(namespace, middlewareInfo.getName())).collect(Collectors.toList());
        } else {
            if (aggregateQuery) {
                return middlewareService.listMiddlewarePod(clusterId, namespace, MiddlewareTypeEnum.REDIS.getType(), middlewareName).
                        stream().filter(middlewareInfo -> middlewareInfo.getType().equals("master")).collect(Collectors.toList()).
                        stream().sorted(new MiddlewareInfoComparator()).
                        map(middlewareInfo -> K8sServiceNameUtil.getServicePath(middlewareInfo.getName(), namespace, middlewareName)).collect(Collectors.toList());
            }
            return Collections.singletonList(K8sServiceNameUtil.getServicePath(namespace, middlewareName));
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
