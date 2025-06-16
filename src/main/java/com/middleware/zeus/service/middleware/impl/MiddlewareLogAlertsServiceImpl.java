package com.middleware.zeus.service.middleware.impl;

import static com.middleware.zeus.common.constants.CommonConstant.DOT;
import static com.middleware.zeus.common.constants.CommonConstant.LINE;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.middleware.Middleware;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.MiddlewareLogAlertDo;
import com.middleware.zeus.common.model.middleware.MiddlewareLogAlertDto;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.ConfigMapService;
import com.middleware.zeus.service.middleware.MiddlewareLogAlertsService;
import com.middleware.zeus.service.registry.HelmChartService;

import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2025/5/14 下午5:22
 */
@Slf4j
@Service
public class MiddlewareLogAlertsServiceImpl implements MiddlewareLogAlertsService {

    @Autowired
    private ConfigMapService configMapService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;


    @Override
    public List<MiddlewareLogAlertDto> listRules(String clusterId, String namespace, String middlewareName, String type) {
        // 查询该中间件的config
        ConfigMap configMap = configMapService.get(clusterId, namespace, middlewareName + LINE + ALERT_RULE);
        if (configMap == null) {
            return new ArrayList<>();
        }
        List<MiddlewareLogAlertDto> middlewareLogAlertDtoList = new ArrayList<>();
        configMap.getData().forEach((k, v) -> {
            // 序列化
            Yaml yaml = new Yaml();
            JSONObject object = yaml.loadAs(v, JSONObject.class);
            MiddlewareLogAlertDo middlewareLogAlertDo = JSONObject.parseObject(object.toJSONString(), MiddlewareLogAlertDo.class);
            // 数据结构转化
            MiddlewareLogAlertDto logAlertDto = new MiddlewareLogAlertDto(middlewareLogAlertDo);
            // 放入list中
            middlewareLogAlertDtoList.add(logAlertDto);
        });
        return middlewareLogAlertDtoList;
    }

    @Override
    public void createRules(MiddlewareLogAlertDto middlewareLogAlertDto) {

        String clusterId = middlewareLogAlertDto.getClusterId();
        String namespace = middlewareLogAlertDto.getNamespace();
        String middlewareName = middlewareLogAlertDto.getMiddlewareName();
        String type = middlewareLogAlertDto.getType();

        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);

        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);
        if (values == null) {
            return;
        }
        JSONObject common = values.getJSONObject("common");
        if (common == null) {
            common = new JSONObject();
        }
        JSONObject customElasticAlert = common.getJSONObject("customElasticAlert");
        if (customElasticAlert == null) {
            customElasticAlert = new JSONObject();
        }

        JSONObject elasticAlert = values.getJSONObject("elasticAlert");
        if (elasticAlert == null) {
            elasticAlert = new JSONObject();
        }

        // 设置更新时间
        middlewareLogAlertDto.setUpdateTime(new Date());
        // 设置默认匹配规则
        if (middlewareLogAlertDto.getMatchRuleList() == null || middlewareLogAlertDto.getMatchRuleList().isEmpty()) {
            middlewareLogAlertDto.setMatchRuleList(new ArrayList<>());
        }
        middlewareLogAlertDto.getMatchRuleList().add(new MiddlewareLogAlertDto.MatchRule(
            MIDDLEWARE_NAME + DOT + KEYWORD, middlewareLogAlertDto.getMiddlewareName(), false));
        middlewareLogAlertDto.getMatchRuleList()
            .add(new MiddlewareLogAlertDto.MatchRule(K8S_POD_NAMESPACE, middlewareLogAlertDto.getNamespace(), false));
        // 数据结构转化
        MiddlewareLogAlertDo alertDo = new MiddlewareLogAlertDo(middlewareLogAlertDto);

        // 告警名称同名校验
        if (customElasticAlert.containsKey(alertDo.getAlertmanagerAlertname()) || elasticAlert.containsKey(alertDo.getAlertmanagerAlertname())) {
            throw new BusinessException(ErrorMessage.LOG_ALERT_NAME_EXISTS);
        }

        // 将对象转换为jsonObject，并添加进values
        customElasticAlert.put(alertDo.getAlertmanagerAlertname(),
            JSONObject.parseObject(JSONObject.toJSONString(alertDo)));

        common.put("customElasticAlert", customElasticAlert);
        values.put("common", common);
        // 更新helm values
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, type);
        middleware.setChartName(type);
        middleware.setChartVersion(helmChartService.getChartVersion(values, type));
        helmChartService.upgrade(middleware, values, values, cluster);
    }

    @Override
    public void updateRules(MiddlewareLogAlertDto middlewareLogAlertDto) {

        String clusterId = middlewareLogAlertDto.getClusterId();
        String namespace = middlewareLogAlertDto.getNamespace();
        String middlewareName = middlewareLogAlertDto.getMiddlewareName();
        String type = middlewareLogAlertDto.getType();

        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);
        if (values == null) {
            return;
        }
        JSONObject common = values.getJSONObject("common");
        if (common == null) {
            common = new JSONObject();
        }
        JSONObject customElasticAlert = common.getJSONObject("customElasticAlert");
        if (customElasticAlert == null) {
            customElasticAlert = new JSONObject();
        }

        JSONObject elasticAlert = common.getJSONObject("elasticAlert");
        if (elasticAlert == null) {
            elasticAlert = new JSONObject();
        }

        // 设置更新时间
        middlewareLogAlertDto.setUpdateTime(new Date());
        MiddlewareLogAlertDo alertDo = new MiddlewareLogAlertDo(middlewareLogAlertDto);

        // 更新原生告警规则
        elasticAlert.computeIfPresent(alertDo.getAlertmanagerAlertname(), (k, v) -> {
            // 序列化
            JSONObject rule = JSONObject.parseObject(JSONObject.toJSONString(v));
            rule.put("alertLevel", middlewareLogAlertDto.getLevel());
            rule.put("threshold", middlewareLogAlertDto.getNumEvents());
            rule.put("silence", middlewareLogAlertDto.getSilence());
            rule.put("interval", middlewareLogAlertDto.getTimeframe());
            // 返回rule
            return rule;
        });
        common.put("elasticAlert", elasticAlert);
        // 更新自定义告警规则
        customElasticAlert.computeIfPresent(alertDo.getAlertmanagerAlertname(), (k, v) -> {
            // 序列化
            JSONObject rule = JSONObject.parseObject(JSONObject.toJSONString(v));
            rule.put("alertLevel", middlewareLogAlertDto.getLevel());
            rule.put("threshold", middlewareLogAlertDto.getNumEvents());
            rule.put("silence", middlewareLogAlertDto.getSilence());
            rule.put("interval", middlewareLogAlertDto.getTimeframe());

            rule.put("type", alertDo.getType());
            rule.put("filter", JSONObject.parseArray(JSONObject.toJSONString(alertDo.getFilter())));
            rule.put("alertTextArgs", JSONObject.parseArray(JSONObject.toJSONString(alertDo.getAlertTextArgs())));
            rule.put("alertText", alertDo.getAlertText());
            if ("blacklist".equals(alertDo.getType())){
                rule.put("compare_key", alertDo.getCompareKey());
                rule.put("blacklist", JSONArray.parseArray(JSONObject.toJSONString(alertDo.getBlacklist())));
            }
            // 返回rule
            return rule;
        });
        common.put("customElasticAlert", customElasticAlert);

        values.put("common", common);
        // 更新helm values
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, type);
        middleware.setChartName(type);
        middleware.setChartVersion(helmChartService.getChartVersion(values, type));
        helmChartService.upgrade(middleware, values, values, cluster);
    }

    @Override
    public void deleteRules(String clusterId, String namespace, String middlewareName, String type, String alertName) {
        // 获取集群对象
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 获取helm values
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, cluster);
        if (values == null || values.getJSONObject("common") == null
            || values.getJSONObject("common").getJSONObject("customElasticAlert") == null) {
            return;
        }
        // 删除自定义告警规则
        values.getJSONObject("common").getJSONObject("customElasticAlert").remove(alertName);
        // 更新helm values
        helmChartService.upgrade(new Middleware(clusterId, namespace, middlewareName, type), values, values, cluster);
    }
}
