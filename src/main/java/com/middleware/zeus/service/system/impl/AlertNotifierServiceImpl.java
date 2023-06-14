package com.middleware.zeus.service.system.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.model.AlertRecordDo;
import com.middleware.zeus.common.model.AlertUserDo;
import com.middleware.zeus.service.system.AlertNotifierService;
import com.middleware.zeus.util.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author liyinlong
 * @since 2023/5/16 3:58 下午
 */
@Slf4j
@Service
public class AlertNotifierServiceImpl implements AlertNotifierService {

    @Value("${system.alert.smsServiceName:smsService}")
    private String alertNotifierService;

    @Value("${system.alert.alertNotifierMethod:sendAlertMessage}")
    private String alertNotifierMethod;

    @Value("${system.alert.smsUrl:http://localhost:8099/sms/sendsms}")
    private String smsUrl;

    @Override
    public Object sendSMSAlertMessage(AlertRecordDo alertRecordDo, List<AlertUserDo> alertUserDoList) {
        Object res = new Object();
        try {
            Object smsService = SpringContextUtils.getBean(alertNotifierService);
            Method method = smsService.getClass().getMethod(alertNotifierMethod, String.class, JSONObject.class, JSONArray.class);
            // 将参数转为json类型
            JSONObject alertRecord = (JSONObject) JSON.toJSON(alertRecordDo);
            JSONArray alertUsers = (JSONArray) JSON.toJSON(alertUserDoList);
            // 调用方法
            res = method.invoke(smsService, smsUrl, alertRecord, alertUsers);
        } catch (Exception e) {
            log.error("调用外部告警服务smsComponent失败");
            log.debug("调用外部告警服务smsComponent失败", e);
        }
        return res;
    }

}
