package com.middleware.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.model.YamlCheck;
import com.middleware.zeus.service.k8s.YamlService;
import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author xutianhong
 * @Date 2021/12/23 3:24 下午
 */
@Service
@Slf4j
public class YamlServiceImpl implements YamlService {

    @Override
    public YamlCheck check(String yamlContent) {
        if (yamlContent.contains("\\n")){
            yamlContent = yamlContent.replace("\\n", "\n");
        }
        Yaml yaml = new Yaml();
        List<String> msg = new ArrayList<>();
        JSONObject object = null;
        try {
            object = yaml.loadAs(yamlContent, JSONObject.class);
        } catch (Exception e) {
            log.error(ErrorMessage.YAML_FORMAT_WRONG.getZhMsg(), e);
            msg.add(ErrorMessage.YAML_FORMAT_WRONG.getZhMsg());
        }
        // yaml为k8s资源
        if (object != null && yamlContent.contains(API_VERSION)) {
            // 校验基本结构
            msg.addAll(baseCheck(object));
            // yaml为configmap
            if (object.containsKey(KIND) && object.getString(KIND).equals(CONFIGMAP)) {
                configmap(object, msg);
            }
            // else
        }
        return new YamlCheck().setFlag(CollectionUtils.isEmpty(msg)).setMsgList(msg);
    }

    public List<String> baseCheck(JSONObject object){
        List<String> msg = new ArrayList<>();
        if (!object.containsKey(KIND)){
            log.error(ErrorMessage.RESOURCE_KIND_NOT_FOUND.getZhMsg());
            msg.add(ErrorMessage.RESOURCE_KIND_NOT_FOUND.getZhMsg());
            return msg;
        }
        if (!object.containsKey(METADATA)){
            log.error(ErrorMessage.RESOURCE_METADATA_NOT_FOUND.getZhMsg());
            msg.add(ErrorMessage.RESOURCE_METADATA_NOT_FOUND.getZhMsg());
            return msg;
        }
        JSONObject metadata = object.getJSONObject(METADATA);
        if (!metadata.containsKey(NAME)){
            log.error(ErrorMessage.RESOURCE_NAME_NOT_FOUND.getZhMsg());
            msg.add(ErrorMessage.RESOURCE_NAME_NOT_FOUND.getZhMsg());
        }
        if (!metadata.containsKey(NAMESPACE)){
            log.error(ErrorMessage.RESOURCE_NAMESPACE_NOT_FOUND.getZhMsg());
            msg.add(ErrorMessage.RESOURCE_NAMESPACE_NOT_FOUND.getZhMsg());
        }
        return msg;
    }

    /**
     * 校验configmap
     **/
    public void configmap(JSONObject object, List<String> msg){
        List<String> configmapKeyList =
                Arrays.asList("apiVersion", "kind", "metadata", "binaryData", "data", "immutable");
        for (String key : object.keySet()) {
            if (configmapKeyList.stream().noneMatch(configmapKey -> configmapKey.equals(key))) {
                msg.add("存在非法字段");
            }
        }
        try {
            JSONObject.parseObject(JSONObject.toJSONString(object), ConfigMap.class);
        } catch (Exception e) {
            log.error(ErrorMessage.PARSE_OBJECT_TO_CONFIGMAP_FAILED.getZhMsg(), e);
            msg.add(ErrorMessage.PARSE_OBJECT_TO_CONFIGMAP_FAILED.getZhMsg());
        }
    }

}
