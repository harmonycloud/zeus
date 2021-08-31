package com.harmonycloud.zeus.service.prometheus.impl;

import java.util.Calendar;
import java.util.Date;

import com.harmonycloud.zeus.dao.BeanAlertRecordMapper;
import com.harmonycloud.zeus.integration.cluster.PrometheusWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.constants.DateStyle;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.DateUnitEnum;
import com.harmonycloud.zeus.bean.BeanAlertRecord;
import com.harmonycloud.zeus.service.prometheus.PrometheusWebhookService;
import com.harmonycloud.tool.date.DateUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/5/7 5:46 下午
 */
@Service
@Slf4j
public class PrometheusWebhookServiceImpl implements PrometheusWebhookService {

    @Autowired
    private BeanAlertRecordMapper beanAlertRecordMapper;
    @Autowired
    private PrometheusWrapper prometheusWrapper;

    @Override
    public void alert(String json) throws Exception {
        JSONObject object = JSONObject.parseObject(json);
        JSONArray alertsList = object.getJSONArray("alerts");
        for (int i = 0; i < alertsList.size(); ++i) {
            JSONObject alert = alertsList.getJSONObject(i);
            if ("resolved".equals(alert.getString("status"))) {
                continue;
            }
            JSONObject labels = alert.getJSONObject("labels");
            JSONObject annotations = alert.getJSONObject("annotations");

            BeanAlertRecord beanAlertRecord = new BeanAlertRecord();
            beanAlertRecord.setName(labels.getString("service"));
            beanAlertRecord.setNamespace(labels.getString("namespace"));
            beanAlertRecord.setType(labels.getString("middleware"));
            String clusterId = labels.getOrDefault("clusterId", "").toString();
            beanAlertRecord.setClusterId(clusterId);
            beanAlertRecord.setAlert(labels.getString("alertname"));
            beanAlertRecord.setLevel(labels.getString("severity"));
            beanAlertRecord.setSummary(annotations.getString("summary"));
            beanAlertRecord.setMessage(annotations.getString("message"));
            beanAlertRecord.setTime(DateUtils.parseDate(
                alert.getString("startsAt").replace(StringUtils.substring(alert.getString("startsAt"), -7, -1), ""),
                DateStyle.YYYY_MM_DD_T_HH_MM_SS_Z_SSS));
            beanAlertRecordMapper.insert(beanAlertRecord);
            // 设置通道沉默时间
            if (annotations.containsKey("silence") && StringUtils.isNotEmpty(clusterId)) {
                setSilence(alert, clusterId);
            }
        }
    }

    /**
     * 设置通道沉默时间
     */
    public void setSilence(JSONObject alert, String clusterId) throws Exception {
        JSONObject alertName = new JSONObject();
        alertName.put("name", "alertname");
        alertName.put("value", alert.getJSONObject("labels").getString("alertname"));
        alertName.put("isRegex", false);
        alertName.put("isEqual", true);

        JSONObject service = new JSONObject();
        service.put("name", "service");
        service.put("value", alert.getJSONObject("labels").getString("service"));
        service.put("isRegex", false);
        service.put("isEqual", true);

        JSONArray matchers = new JSONArray();
        matchers.add(alertName);
        matchers.add(service);

        Date now = DateUtils.addInteger(new Date(), Calendar.HOUR_OF_DAY, -8);
        JSONObject body = new JSONObject();
        body.put("matchers", matchers);
        body.put("createdBy", "admin");
        body.put("comment", "silence");
        body.put("startsAt", DateUtils.dateToString(now, DateStyle.YYYY_MM_DD_T_HH_MM_SS_Z_SSS));
        String silence = alert.getJSONObject("annotations").getString("silence");
        body.put("endsAt",
            DateUtils.dateToString(calculateEndTime(now, silence), DateStyle.YYYY_MM_DD_T_HH_MM_SS_Z_SSS));
        prometheusWrapper.setSilence(clusterId, NameConstant.ALERT_MANAGER_API_VERSION_SILENCES, body);
    }

    /**
     * 计算停止沉默时间
     */
    public Date calculateEndTime(Date now, String silence) {
        if (silence.contains(DateUnitEnum.SECOND.getUnit())) {
            return DateUtils.addInteger(now, Calendar.SECOND,
                Integer.parseInt(silence.split(DateUnitEnum.SECOND.getUnit())[0]));
        } else if (silence.contains(DateUnitEnum.MINUTE.getUnit())) {
            return DateUtils.addInteger(now, Calendar.MINUTE,
                Integer.parseInt(silence.split(DateUnitEnum.MINUTE.getUnit())[0]));
        } else if (silence.contains(DateUnitEnum.HOUR.getUnit())) {
            return DateUtils.addInteger(now, Calendar.HOUR_OF_DAY,
                Integer.parseInt(silence.split(DateUnitEnum.HOUR.getUnit())[0]));
        } else if (silence.contains(DateUnitEnum.DAY.getUnit())) {
            return DateUtils.addInteger(now, Calendar.DAY_OF_MONTH,
                Integer.parseInt(silence.split(DateUnitEnum.DAY.getUnit())[0]));
        } else if (silence.contains(DateUnitEnum.WEEK.getUnit())) {
            return DateUtils.addInteger(now, Calendar.WEEK_OF_MONTH,
                Integer.parseInt(silence.split(DateUnitEnum.WEEK.getUnit())[0]));
        }
        return new Date();
    }
}
