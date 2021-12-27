package com.harmonycloud.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.YamlCheck;
import com.harmonycloud.zeus.service.k8s.YamlService;
import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

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
            // yaml为configmap
            if (object.containsKey(KIND) && object.getString(KIND).equals(CONFIGMAP)){
                try {
                    JSONObject.parseObject(JSONObject.toJSONString(object), ConfigMap.class);
                } catch (Exception e){
                    log.error(ErrorMessage.PARSE_OBJECT_TO_CONFIGMAP_FAILED.getZhMsg(), e);
                    msg.add(ErrorMessage.PARSE_OBJECT_TO_CONFIGMAP_FAILED.getZhMsg());
                }
            }
            // else
        }
        return new YamlCheck().setFlag(CollectionUtils.isEmpty(msg)).setMsgList(msg);
    }

}
