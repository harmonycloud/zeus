package com.middleware.zeus.service.prometheus.impl;

import com.middleware.zeus.common.constants.NameConstant;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.PrometheusResponse;
import com.middleware.zeus.util.numeric.ResourceCalculationUtil;
import com.middleware.zeus.integration.cluster.PrometheusWrapper;
import com.middleware.zeus.service.prometheus.PrometheusResourceMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import static com.middleware.zeus.common.constants.CommonConstant.LINE;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.NAMESPACE;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.POD;

/**
 * @author xutianhong
 * @Date 2022/2/25 10:49 上午
 */
@Service
@Slf4j
public class PrometheusResourceMonitorServiceImpl implements PrometheusResourceMonitorService {

    @Autowired
    private PrometheusWrapper prometheusWrapper;
    
    @Override
    public Double queryAndConvert(String clusterId, String query){
        double res = 0.0;
        try {
            PrometheusResponse response = this.query(clusterId, query);
            if (!StringUtils.isEmpty(response.getData().getResult())) {
                res = ResourceCalculationUtil.roundNumber(
                    BigDecimal.valueOf(Double.parseDouble(response.getData().getResult().get(0).getValue().get(1))), 2,
                    RoundingMode.CEILING);
            }
        } catch (Exception e) {
            log.debug("集群:{} 执行语句:{} 失败", clusterId, query, e);
        }
        return res;
    }

    @Override
    public Map<String, Double> queryPvcs(String clusterId, String query) {
        Map<String, Double> map = new HashMap<>();
        try {
            PrometheusResponse response = this.query(clusterId, query);
            response.getData().getResult().forEach(res -> {
                res.getMetric().forEach((k, v) -> {
                    map.put(v.substring(v.length() - 1), ResourceCalculationUtil.roundNumber(
                            BigDecimal.valueOf(Double.parseDouble(res.getValue().get(1))), 2, RoundingMode.CEILING));
                });
            });
        } catch (Exception e) {
            log.debug("集群:{} 执行语句:{} 失败", clusterId, query, e);
        }
        return map;
    }

    @Override
    public PrometheusResponse query(String clusterId, String query) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("query", query);
        log.debug("开始执行语句: {}" , query);
        PrometheusResponse response = prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, map);
        if (CollectionUtils.isEmpty(response.getData().getResult())){
            log.debug("查询prometheus结果为空");
            throw new BusinessException(ErrorMessage.EMPTY_RESULT);
        }
        log.debug("执行语句: {} 成功", query);
        return response;
    }

    @Override
    public Map<String, Double> sumResponseByTarget(PrometheusResponse response, String target) {
        Map<String, Double> map = new HashMap<>();
        // 空值处理
        if (response == null || response.getData() == null || CollectionUtils.isEmpty(response.getData().getResult())) {
            return map;
        }
        response.getData().getResult().forEach(res -> {
            // 空值处理
            if (CollectionUtils.isEmpty(res.getMetric()) || res.getValue() == null || res.getValue().size() < 2) {
                return;
            }
            // 特殊字段处理
            if (!res.getMetric().containsKey(target) || !res.getMetric().containsKey(NAMESPACE)) {
                return;
            }
            // 获取pod名称
            String targetName = res.getMetric().get(target);
            // 获取命名空间
            String namespace = res.getMetric().get(NAMESPACE);
            // 拼接唯一key值
            String key = namespace + LINE + targetName;
            // 获取监控数据
            double value = ResourceCalculationUtil
                .roundNumber(BigDecimal.valueOf(Double.parseDouble(res.getValue().get(1))), 2, RoundingMode.CEILING);
            // 汇总数据
            if (map.containsKey(key)) {
                map.put(key, map.get(key) + value);
            } else {
                map.put(key, value);
            }
        });
        return map;
    }
}
