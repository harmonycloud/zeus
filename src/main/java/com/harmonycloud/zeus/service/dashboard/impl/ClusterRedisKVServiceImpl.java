package com.harmonycloud.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.dashboard.RedisClient;
import com.harmonycloud.zeus.service.dashboard.RedisKVService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.util.K8sServiceNameUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/11/23 11:24 上午
 */
@Service("ClusterRedisKVServiceImpl")
public class ClusterRedisKVServiceImpl implements RedisKVService {

    @Autowired
    private RedisClient redisClient;
    @Autowired
    private MiddlewareService middlewareService;

    @Override
    public JSONArray getKeys(String clusterId, String namespace, String middlewareName, Integer db) {
        List<MiddlewareInfo> pods = listMasterPod(clusterId, namespace, middlewareName);
        JSONArray jsonArray = new JSONArray();
        pods.forEach(pod -> {
            JSONObject res = redisClient.getAllKeys(K8sServiceNameUtil.getServicePath(pod.getName(), middlewareName, namespace), db);
            if (res.getJSONObject("err") != null || !"".equals(res.getString("err"))) {
                throw new BusinessException(ErrorMessage.FAILED_TO_QUERY_KEY, res.getString("err"));
            }
            if (res.getJSONObject("data") != null) {
                jsonArray.addAll(res.getJSONArray("data"));
            }
        });
        return jsonArray;
    }

    @Override
    public JSONArray getKeysWithPattern(String clusterId, String namespace, String middlewareName, Integer db, String keyword) {
        List<MiddlewareInfo> pods = listMasterPod(clusterId, namespace, middlewareName);
        JSONArray jsonArray = new JSONArray();
        pods.forEach(pod -> {
            JSONObject res = redisClient.getKeys(K8sServiceNameUtil.getServicePath(pod.getName(), middlewareName, namespace), db, keyword);
            if (res.getJSONObject("err") != null || !"".equals(res.getString("err"))) {
                throw new BusinessException(ErrorMessage.FAILED_TO_QUERY_KEY, res.getString("err"));
            }
            JSONArray data = res.getJSONArray("data");
            if (!CollectionUtils.isEmpty(data)) {
                jsonArray.addAll(data);
            }
        });
        return jsonArray;
    }

    private List<MiddlewareInfo> listMasterPod(String clusterId, String namespace, String middlewareName) {
        return middlewareService.listMiddlewarePod(clusterId, namespace, MiddlewareTypeEnum.REDIS.getType(), middlewareName).
                stream().filter(middlewareInfo -> middlewareInfo.getType().equals("master")).collect(Collectors.toList()).
                stream().sorted(new MiddlewareInfoComparator()).collect(Collectors.toList());
    }

    public static class MiddlewareInfoComparator implements Comparator<MiddlewareInfo> {
        @Override
        public int compare(MiddlewareInfo o1, MiddlewareInfo o2) {
            return o1.getName() == null ? 1
                    : o2.getName() == null ? 1 : o1.getName().compareTo(o2.getName());
        }
    }

}
