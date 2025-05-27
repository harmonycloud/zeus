package com.middleware.zeus.service.middleware.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.model.middleware.MiddlewareLogAlertDo;
import com.middleware.zeus.common.model.middleware.MiddlewareLogAlertDto;
import com.middleware.zeus.service.k8s.ConfigMapService;
import com.middleware.zeus.service.middleware.MiddlewareLogAlertsService;

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


    @Override
    public List<MiddlewareLogAlertDto> listRules(String clusterId, String namespace, String middlewareName, String type) {
        // 查询该中间件的config
        ConfigMap configMap = configMapService.get(clusterId, namespace, middlewareName + "-alertrule-code28000");

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

        // 获取对应configmap
        ConfigMap configMap = configMapService.get(clusterId, namespace, middlewareName + "-alertrule-code28000");
        if (configMap == null || configMap.getData() == null) {
            return;
        }
        // 设置更新时间
        middlewareLogAlertDto.setUpdateTime(new Date());
        // 设置默认匹配规则
        if (middlewareLogAlertDto.getMatchRuleList() == null || middlewareLogAlertDto.getMatchRuleList().isEmpty()) {
            middlewareLogAlertDto.setMatchRuleList(new ArrayList<>());
        }
        middlewareLogAlertDto.getMatchRuleList().add(new MiddlewareLogAlertDto.MatchRule("middleware_name.keyword",
            middlewareLogAlertDto.getMiddlewareName(), false));
        middlewareLogAlertDto.getMatchRuleList()
            .add(new MiddlewareLogAlertDto.MatchRule("k8s_pod_namesapce", middlewareLogAlertDto.getNamespace(), false));
        // 数据结构转化
        MiddlewareLogAlertDo alertDo = new MiddlewareLogAlertDo(middlewareLogAlertDto);

        // 告警名称同名校验
        if (configMap.getData().containsKey(alertDo.getName() + ".yaml")){
            throw new BusinessException(ErrorMessage.LOG_ALERT_NAME_EXISTS);
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // 使用块格式
        options.setPrettyFlow(true); // 美化输出

        // 创建 Yaml 实例
        Yaml yaml = new Yaml(options);

        // 将对象转换为 YAML 字符串，并添加进configmap的data中
        String yamlString = yaml.dump(JSONObject.toJSON(alertDo));
        configMap.getData().put(alertDo.getName() + ".yaml", yamlString);

        // 更新configmap
        configMapService.update(clusterId, namespace, configMap);
    }

    @Override
    public void updateRules(MiddlewareLogAlertDto middlewareLogAlertDto) {

        String clusterId = middlewareLogAlertDto.getClusterId();
        String namespace = middlewareLogAlertDto.getNamespace();
        String middlewareName = middlewareLogAlertDto.getMiddlewareName();

        // 获取对应configmap
        ConfigMap configMap = configMapService.get(clusterId, namespace, middlewareName + "-alertrule-code28000");
        if (configMap == null || configMap.getData() == null) {
            return;
        }

        // 设置更新时间
        middlewareLogAlertDto.setUpdateTime(new Date());
        MiddlewareLogAlertDo alertDo = new MiddlewareLogAlertDo(middlewareLogAlertDto);

        configMap.getData().computeIfPresent(alertDo.getName() + ".yaml", (k, v) -> {
            // 序列化
            Yaml yaml = new Yaml();
            JSONObject object = yaml.loadAs(v, JSONObject.class);
            MiddlewareLogAlertDo middlewareLogAlertDo = JSONObject.parseObject(object.toJSONString(), MiddlewareLogAlertDo.class);

            middlewareLogAlertDo.setTimeframe(alertDo.getTimeframe());
            middlewareLogAlertDo.setNumEvents(alertDo.getNumEvents());
            middlewareLogAlertDo.setBlacklist(alertDo.getBlacklist());
            middlewareLogAlertDo.setFilter(alertDo.getFilter());

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // 使用块格式
            options.setPrettyFlow(true); // 美化输出

            // 创建 Yaml 实例
            yaml = new Yaml(options);

            // 将对象转换为 YAML 字符串，并添加进configmap的data中
            return yaml.dump(JSONObject.toJSON(alertDo));
        });

        configMapService.update(clusterId, namespace, configMap);
    }

    @Override
    public void deleteRules(String clusterId, String namespace, String middlewareName, String type, String alertName) {
        // 获取对应configmap
        ConfigMap configMap = configMapService.get(clusterId, namespace, middlewareName + "-alertrule-code28000");
        // 遍历data
        if (configMap == null || configMap.getData() == null){
            return;
        }
        Yaml yaml = new Yaml();
        configMap.getData().computeIfPresent(alertName + ".yaml", (key, value) -> null);
        // 更新configmap
        configMapService.update(clusterId, namespace, configMap);
    }
}
