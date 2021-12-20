package com.harmonycloud.zeus.service.aspect.impl;

import java.util.Map;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
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
    public void operation(MiddlewareClusterDTO cluster, Middleware middleware, Map<String, Object> dynamicValues, JSONObject values){
        try {
            Object service = SpringContextUtils.getBean(name);
            dynamicValues.put("namespace", middleware.getNamespace());
            dynamicValues.put("name", middleware.getName());
            dynamicValues.put("middlewareType", middleware.getType());
            service.getClass().getMethod("operation", JSONObject.class, Map.class, JSONObject.class).invoke(service, JSONObject.parseObject(JSONObject.toJSONString(cluster)), dynamicValues, values);
        } catch (Exception e){
            log.error("调用外部服务失败");
            // throw new BusinessException(ErrorMessage.CALL_EXTERNAL_SERVICE_FAILED);
        }
    }
}
