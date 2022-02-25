package com.harmonycloud.zeus.service.prometheus.impl;

import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.PrometheusResponse;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;
import com.harmonycloud.zeus.integration.cluster.PrometheusWrapper;
import com.harmonycloud.zeus.service.prometheus.PrometheusResourceMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

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
            return res;
        } catch (Exception e) {
            log.error("集群:{} 执行语句:{} 失败", clusterId, query, e);
        }
        return res;
    }

    @Override
    public PrometheusResponse query(String clusterId, String query) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("query", query);
        log.info("开始执行语句: {}" , query);
        PrometheusResponse response = prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION, map);
        if (CollectionUtils.isEmpty(response.getData().getResult())){
            log.error("查询prometheus结果为空");
            throw new BusinessException(ErrorMessage.EMPTY_RESULT);
        }
        log.info("执行语句: {} 成功", query);
        return response;
    }
}
