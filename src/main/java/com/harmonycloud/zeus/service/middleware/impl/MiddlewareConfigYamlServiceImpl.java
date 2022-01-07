package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.YamlCheck;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.integration.cluster.ConfigMapWrapper;
import com.harmonycloud.zeus.service.k8s.YamlService;
import com.harmonycloud.zeus.service.middleware.MiddlewareConfigYamlService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2021/12/23 2:23 下午
 */
@Service
@Slf4j
public class MiddlewareConfigYamlServiceImpl implements MiddlewareConfigYamlService {

    @Autowired
    private ConfigMapWrapper configMapWrapper;
    @Autowired
    private YamlService yamlService;
    @Autowired
    private MiddlewareService middlewareService;

    @Override
    public List<String> nameList(String clusterId, String namespace, String name, String type, String chartVersion) {
        List<ConfigMap> configMapList = configMapWrapper.list(clusterId, namespace).stream()
            .filter(configMap -> configMap.getMetadata().getAnnotations().containsKey("meta.helm.sh/release-name")
                && configMap.getMetadata().getAnnotations().get("meta.helm.sh/release-name").equals(name))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(configMapList)) {
            return new ArrayList<>();
        }
        return configMapList.stream().map(configMap -> configMap.getMetadata().getName()).collect(Collectors.toList());
    }

    @Override
    public String yaml(String clusterId, String namespace, String configMapName) {
        List<ConfigMap> configMapList = configMapWrapper.list(clusterId, namespace);
        if (StringUtils.isNotEmpty(configMapName)) {
            configMapList =
                configMapList.stream().filter(configMap -> configMap.getMetadata().getName().equals(configMapName))
                    .collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(configMapList)) {
            return null;
        }
        Yaml yaml = new Yaml();
        return yaml.dumpAsMap(configMapList.get(0));
    }

    @Override
    public void update(String clusterId, String namespace, String configMapName, String config) {
        if (config.contains("\\n")){
            config = config.replace("\\n", "\n");
        }
        YamlCheck yamlCheck = yamlService.check(config);
        if (!yamlCheck.getFlag()){
            throw new BusinessException(ErrorMessage.YAML_FORMAT_WRONG);
        }
        Yaml yaml = new Yaml();
        ConfigMap configMap = yaml.loadAs(config, ConfigMap.class);
        try {
            configMapWrapper.update(clusterId, namespace, configMap);
        } catch (Exception e){
            log.error(ErrorMessage.UPDATE_CONFIGMAP_FAILED.getZhMsg(), e);
            throw new BusinessException(ErrorMessage.UPDATE_CONFIGMAP_FAILED);
        }
    }
}
