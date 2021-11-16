package com.harmonycloud.zeus.service.aspect.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.QuestionYaml;
import com.harmonycloud.zeus.annotation.Dynamic;
import com.harmonycloud.zeus.service.aspect.AspectService;
import com.harmonycloud.zeus.util.SpringContextUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/11/12 11:40 上午
 */
@Service
@Slf4j
public class AspectServiceImpl implements AspectService {

    @Value("${dynamic.name:lianTongService}")
    private String name;

    @Override
    public QuestionYaml dynamic() {
        try {
            Object service = SpringContextUtils.getBean(name);
            JSONObject jsonObject = (JSONObject)service.getClass().getMethod("dynamic").invoke(service);
            return JSONObject.parseObject(JSONObject.toJSONString(jsonObject), QuestionYaml.class);
        }catch (Exception e){
            log.error("外部服务不存在");
            return null;
        }
    }

    @Override
    public void operation(String host, Middleware middleware, Map<String, String> dynamicValues, JSONObject values){
        try {
            Object service = SpringContextUtils.getBean(name);
            dynamicValues.put("namespace", middleware.getNamespace());
            dynamicValues.put("name", middleware.getName());
            dynamicValues.put("middlewareType", middleware.getType());
            service.getClass().getMethod("operation", String.class, Map.class, JSONObject.class).invoke(service, host, dynamicValues, values);
        } catch (Exception e){
            throw new BusinessException(ErrorMessage.CALL_EXTERNAL_SERVICE_FAILED);
        }
    }
}
