package com.middleware.zeus.service.middleware.impl;

import static com.middleware.zeus.common.constants.CommonConstant.DOT;
import static com.middleware.zeus.common.constants.CommonConstant.LINE;
import static com.middleware.zeus.common.constants.NameConstant.COMMON;
import static com.middleware.zeus.common.constants.NameConstant.UPDATE_TIME;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.ConfigMapService;
import com.middleware.zeus.service.middleware.MiddlewareLogAlertsService;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.util.date.DateUtils;

import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2025/5/14 下午5:22
 */
@Slf4j
@Service
public class MiddlewareLogAlertsServiceImpl implements MiddlewareLogAlertsService {

    private static final String CUSTOM_ELASTIC_ALERT = "customElasticAlert";
    private static final String ELASTIC_ALERT = "elasticAlert";

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

        // 查询该中间件部署配置
        JSONObject values =
            helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        if (values != null && values.getJSONObject(COMMON) != null) {
            // 获取自定义日志告警规则内容
            JSONObject customElasticAlert = values.getJSONObject(COMMON).getJSONObject(CUSTOM_ELASTIC_ALERT);
            // 获取原生日志告警规则内容
            JSONObject elasticAlert = values.getJSONObject(COMMON).getJSONObject(ELASTIC_ALERT);
            // 遍历当前入职告警规则，完善filter，content，updateTime字段
            for (MiddlewareLogAlertDto middlewareLogAlertDto : middlewareLogAlertDtoList) {
                // 获取values中的告警配置信息，无论是自定义还是默认
                JSONObject alert = new JSONObject();
                if (customElasticAlert != null
                    && customElasticAlert.getJSONObject(middlewareLogAlertDto.getAlert()) != null) {
                    alert = customElasticAlert.getJSONObject(middlewareLogAlertDto.getAlert());
                }
                if (elasticAlert != null && elasticAlert.getJSONObject(middlewareLogAlertDto.getAlert()) != null) {
                    alert = elasticAlert.getJSONObject(middlewareLogAlertDto.getAlert());
                }
                // 反序列化数据结构
                MiddlewareLogAlertHelmDo middlewareLogAlertHelmDo = JSONObject.parseObject(JSONObject.toJSONString(alert), MiddlewareLogAlertHelmDo.class);
                // 数据结构转化
                MiddlewareLogAlertDto alertDto = new MiddlewareLogAlertDto(middlewareLogAlertHelmDo);
                // 补充内容
                if (alertDto.getMatchRuleList() != null) {
                    middlewareLogAlertDto.setMatchRuleList(alertDto.getMatchRuleList());
                }
                if (alertDto.getContent() != null){
                    middlewareLogAlertDto.setContent(alertDto.getContent());
                }
                if (alertDto.getUpdateTime() != null){
                    middlewareLogAlertDto.setUpdateTime(alertDto.getUpdateTime());
                }
                // 特殊处理silence时间为60m的情况，转换为1h
                if (middlewareLogAlertDto.getSilence() != null && middlewareLogAlertDto.getSilence().equals("60m")) {
                    middlewareLogAlertDto.setSilence("1h");
                }
            }
        }
        // 排序
        middlewareLogAlertDtoList.sort(Comparator.comparing(MiddlewareLogAlertDto::getUpdateTime,
            Comparator.nullsLast(Comparator.reverseOrder())));

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
        JSONObject common = values.getJSONObject(COMMON);
        if (common == null) {
            common = new JSONObject();
        }
        JSONObject customElasticAlert = common.getJSONObject(CUSTOM_ELASTIC_ALERT);
        if (customElasticAlert == null) {
            customElasticAlert = new JSONObject();
        }

        JSONObject elasticAlert = values.getJSONObject(ELASTIC_ALERT);
        if (elasticAlert == null) {
            elasticAlert = new JSONObject();
        }

        // 设置更新时间
        middlewareLogAlertDto.setUpdateTime(new Date());
        // 设置默认匹配规则
        if (middlewareLogAlertDto.getMatchRuleList() == null || middlewareLogAlertDto.getMatchRuleList().isEmpty()) {
            middlewareLogAlertDto.setMatchRuleList(new ArrayList<>());
        }
        // 数据结构转化
        String alertName = middlewareLogAlertDto.getAlert();
        MiddlewareLogAlertHelmDo middlewareLogAlertHelmDo = new MiddlewareLogAlertHelmDo(middlewareLogAlertDto);

        // 告警名称同名校验
        if (customElasticAlert.containsKey(alertName) || elasticAlert.containsKey(alertName)) {
            throw new BusinessException(ErrorMessage.LOG_ALERT_NAME_EXISTS);
        }

        // 将对象转换为jsonObject，并添加进values
        customElasticAlert.put(alertName,
            JSONObject.parseObject(JSONObject.toJSONString(middlewareLogAlertHelmDo)));

        common.put(CUSTOM_ELASTIC_ALERT, customElasticAlert);
        values.put(COMMON, common);
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
        JSONObject common = values.getJSONObject(COMMON);
        if (common == null) {
            common = new JSONObject();
        }
        JSONObject customElasticAlert = common.getJSONObject(CUSTOM_ELASTIC_ALERT);
        if (customElasticAlert == null) {
            customElasticAlert = new JSONObject();
        }

        JSONObject elasticAlert = common.getJSONObject(ELASTIC_ALERT);
        if (elasticAlert == null) {
            elasticAlert = new JSONObject();
        }

        // 设置更新时间
        middlewareLogAlertDto.setUpdateTime(new Date());
        MiddlewareLogAlertHelmDo middlewareLogAlertHelmDo = new MiddlewareLogAlertHelmDo(middlewareLogAlertDto);

        String alertName = middlewareLogAlertDto.getAlert();
        // 更新原生告警规则
        elasticAlert.computeIfPresent(alertName, (k, v) -> {
            // 序列化
            JSONObject rule = JSONObject.parseObject(JSONObject.toJSONString(v));
            rule.put("alertLevel", middlewareLogAlertDto.getLevel());
            rule.put("threshold", middlewareLogAlertDto.getNumEvents());
            rule.put("silence", middlewareLogAlertDto.getSilence());
            rule.put("interval", middlewareLogAlertDto.getTimeframe());
            // 返回rule
            return rule;
        });
        common.put(ELASTIC_ALERT, elasticAlert);
        // 更新自定义告警规则
        customElasticAlert.computeIfPresent(alertName, (k, v) -> {
            // 序列化
            // 返回rule
            return JSONObject.parseObject(JSONObject.toJSONString(middlewareLogAlertHelmDo));
        });
        common.put(CUSTOM_ELASTIC_ALERT, customElasticAlert);

        values.put(COMMON, common);
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
        if (values == null || values.getJSONObject(COMMON) == null
            || values.getJSONObject(COMMON).getJSONObject(CUSTOM_ELASTIC_ALERT) == null) {
            return;
        }
        // 删除自定义告警规则
        values.getJSONObject(COMMON).getJSONObject(CUSTOM_ELASTIC_ALERT).remove(alertName);
        // 更新helm values
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, type);
        middleware.setChartName(type);
        middleware.setChartVersion(helmChartService.getChartVersion(values, type));
        helmChartService.upgrade(middleware, values, values, cluster);
    }
}
