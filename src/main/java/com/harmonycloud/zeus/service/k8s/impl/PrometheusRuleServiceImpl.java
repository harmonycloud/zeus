package com.harmonycloud.zeus.service.k8s.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.zeus.integration.cluster.PrometheusRuleWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.harmonycloud.zeus.service.k8s.PrometheusRuleService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/4/27 10:45 上午
 */
@Service
@Slf4j
public class PrometheusRuleServiceImpl implements PrometheusRuleService {

    @Autowired
    private PrometheusRuleWrapper prometheusRuleWrapper;

    @Override
    public PrometheusRule get(String clusterId, String namespace, String name) {
        try {
            return prometheusRuleWrapper.get(clusterId, namespace, name);
        } catch (Exception e) {
            log.error("集群{} 分区{} 中间件{} 获取告警规则失败", clusterId, namespace, name);
            throw new CaasRuntimeException(ErrorMessage.PROMETHEUS_RULES_NOT_EXIST);
        }
    }

    @Override
    public void update(String clusterId, PrometheusRule prometheusRule) {
        try {
            prometheusRuleWrapper.update(clusterId, prometheusRule);
        } catch (Exception e) {
            log.error("集群{} 分区{} 告警规则更新失败", clusterId, prometheusRule.getMetadata().getNamespace());
            throw new CaasRuntimeException(ErrorMessage.UPDATE_RULES_FAILED);
        }
    }
}
