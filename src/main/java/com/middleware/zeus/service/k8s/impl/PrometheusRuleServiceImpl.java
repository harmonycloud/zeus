package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.common.model.middleware.MiddlewareAlertsDTO;
import com.middleware.zeus.service.k8s.PrometheusRuleService;
import com.middleware.zeus.util.date.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.CaasRuntimeException;
import com.middleware.zeus.integration.cluster.PrometheusRuleWrapper;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRule;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.middleware.zeus.common.constants.AlertConstant.SILENCE;

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
    public List<PrometheusRule> list(String clusterId, String namespace, Map<String, String> labels) {
        try {
            return prometheusRuleWrapper.list(clusterId, namespace, labels);
        } catch (Exception e) {
            log.error("集群{} 获取告警规则列表失败", clusterId);
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

    @Override
    public void create(String clusterId, PrometheusRule prometheusRule) {
        try {
            prometheusRuleWrapper.create(clusterId, prometheusRule);
        } catch (Exception e) {
            log.error("集群{} 分区{} 告警规则创建失败", clusterId, prometheusRule.getMetadata().getNamespace());
            throw new CaasRuntimeException(ErrorMessage.CREATE_RULES_FAILED);
        }
    }

    @Override
    public void delete(String clusterId, String namespace, String name) {
        try {
            prometheusRuleWrapper.delete(clusterId, namespace, name);
        } catch (Exception e) {
            log.error("集群{} 分区{} 告警规则删除失败", clusterId, namespace);
            throw new CaasRuntimeException(ErrorMessage.DELETE_RULES_FAILED);
        }
    }

    @Override
    public List<MiddlewareAlertsDTO> convertPrometheusRule(PrometheusRule prometheusRule) {
        if (prometheusRule == null) {
            return new ArrayList<>();
        }
        List<MiddlewareAlertsDTO> middlewareAlertsDTOList = new ArrayList<>();
        prometheusRule.getSpec().getGroups().forEach(prometheusRuleGroups -> {
            prometheusRuleGroups.getRules().forEach(prometheusRules -> {
                if (StringUtils.isEmpty(prometheusRules.getAlert())){
                    return;
                }
                MiddlewareAlertsDTO middlewareAlertsDTO = new MiddlewareAlertsDTO();
                middlewareAlertsDTO.setAlert(prometheusRules.getAlert());
                middlewareAlertsDTO.setLabels(prometheusRules.getLabels());
                middlewareAlertsDTO.setAnnotations(prometheusRules.getAnnotations());
                middlewareAlertsDTO.getAnnotations().put("group", prometheusRuleGroups.getName());
                middlewareAlertsDTO.setExpr(prometheusRules.getExpr());
                middlewareAlertsDTO.setTime(prometheusRules.getTime());
                middlewareAlertsDTO.setName(middlewareAlertsDTO.getMiddlewareName());
                middlewareAlertsDTO.setDescription(prometheusRules.getAlert());
                middlewareAlertsDTO.setLevel(prometheusRules.getLabels().get("severity"));
                if (prometheusRules.getAnnotations() != null) {
                    if (prometheusRules.getAnnotations().containsKey(SILENCE)) {
                        middlewareAlertsDTO.setSilence(prometheusRules.getAnnotations().get(SILENCE));
                    }
                    middlewareAlertsDTO.setUnit(prometheusRules.getAnnotations().getOrDefault("unit", ""));
                    if (prometheusRules.getAnnotations().containsKey("createTime")) {
                        middlewareAlertsDTO.setCreateTime(DateUtils.parseDate(
                            prometheusRules.getAnnotations().get("createTime"), DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z));
                    }
                }
                // 将文件创建时间设置为告警规则时间
                if (middlewareAlertsDTO.getCreateTime() == null) {
                    middlewareAlertsDTO
                        .setCreateTime(DateUtils.parseUTCDate(prometheusRule.getMetadata().getCreationTimestamp()));
                }
                // 设置是否为自定义告警规则
                if ("custom-alert-rules".equals(prometheusRuleGroups.getName())){
                    middlewareAlertsDTO.setCustom(true);
                }
                middlewareAlertsDTOList.add(middlewareAlertsDTO);
            });
        });
        return middlewareAlertsDTOList;
    }
}
