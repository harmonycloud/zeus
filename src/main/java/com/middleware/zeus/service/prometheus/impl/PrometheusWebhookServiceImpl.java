package com.middleware.zeus.service.prometheus.impl;

import java.util.*;

import com.middleware.zeus.common.model.AlertRecordDo;
import com.middleware.zeus.common.model.AlertUserDo;
import com.middleware.zeus.bean.BeanSystemConfig;
import com.middleware.zeus.dao.*;
import com.middleware.zeus.dao.BeanAlertRecordMapper;
import com.middleware.zeus.service.middleware.AlertRecordService;
import com.middleware.zeus.service.middleware.MiddlewareAlertsService;
import com.middleware.zeus.bean.BeanAlertRecord;
import com.middleware.zeus.service.system.AlertNotifierService;
import com.middleware.zeus.service.system.AlertService;
import com.middleware.zeus.service.system.AlertUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.constants.DateStyle;
import com.middleware.zeus.common.constants.NameConstant;
import com.middleware.zeus.common.enums.DateUnitEnum;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.integration.cluster.AlertManagerWrapper;
import com.middleware.zeus.service.prometheus.PrometheusWebhookService;
import com.middleware.zeus.service.user.DingRobotService;
import com.middleware.zeus.service.user.MailService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import static com.middleware.zeus.common.constants.AlertConstant.BACKUP;
import static com.middleware.zeus.common.constants.AlertConstant.SERVICE;

/**
 * @author xutianhong
 * @Date 2021/5/7 5:46 下午
 */
@Service
@Slf4j
public class PrometheusWebhookServiceImpl implements PrometheusWebhookService {

    @Value("${system.alert.silent:1h}")
    private String silentTime;

    @Autowired
    private AlertRecordService alertRecordService;
    @Autowired
    private MailService mailService;
    @Autowired
    private AlertManagerWrapper alertManagerWrapper;
    @Autowired
    private MiddlewareAlertsService middlewareAlertsService;
    @Autowired
    private BeanSystemConfigMapper beanSystemConfigMapper;
    @Autowired
    private AlertUserService alertUserService;
    @Autowired
    private AlertNotifierService alertNotifierService;

    @Value("${system.alert.sms.enable:false}")
    private Boolean enableSMS;

    @Override
    public void alert(String json) throws Exception {
        JSONObject object = JSONObject.parseObject(json);
        JSONArray alertsList = object.getJSONArray("alerts");
        for (int i = 0; i < alertsList.size(); ++i) {
            JSONObject alert = alertsList.getJSONObject(i);
            // 过滤resolved通知
            if ("resolved".equals(alert.getString("status"))) {
                continue;
            }
            // 获取labels和annotation
            JSONObject labels = alert.getJSONObject("labels");
            JSONObject annotations = alert.getJSONObject("annotations");
            // 过滤labels或annotation为null的告警通知
            if (labels == null || annotations == null) {
                continue;
            }
            // 过滤集群id
            if (!labels.containsKey("clusterId")) {
                continue;
            }
            // init object
            AlertRecordDo alertRecordDo = new AlertRecordDo();

            // 区分集群/平台 和 服务 告警类型
            if(annotations.containsKey("target_type") && annotations.containsKey("target_name") && annotations.containsKey("target_alias_name")){
                alertRecordDo.setAlertType(annotations.getString("target_type"));
                alertRecordDo.setTargetName(annotations.getString("target_name"));
                alertRecordDo.setTargetAliasName(annotations.getString("target_alias_name"));
            } else if (labels.containsKey(SERVICE)) {
                if (annotations.containsKey("target_type")) {
                    alertRecordDo.setBackupAlert(true);
                }
                alertRecordDo.setAlertType(SERVICE);
                alertRecordDo.setTargetName(labels.getString(SERVICE));
                alertRecordDo.setTargetAliasName(labels.getString(SERVICE));
                alertRecordDo.setMiddlewareType(labels.getString("middleware"));
            }

            alertRecordDo.setClusterId(labels.getString("clusterId"));
            alertRecordDo.setNamespace(labels.getString("namespace"));
            alertRecordDo.setLevel(labels.getString("severity"));
            alertRecordDo.setAlertName(labels.getString("alertname"));
            alertRecordDo.setMessage(annotations.getString("message"));
            if (StringUtils.isEmpty(alertRecordDo.getMessage())) {
                if (annotations.containsKey("description")) {
                    alertRecordDo.setMessage(annotations.getString("description"));
                } else if (annotations.containsKey("summary")) {
                    alertRecordDo.setMessage(annotations.getString("summary"));
                }
            }
            alertRecordDo.setSummary(annotations.getString("summary"));
            alertRecordDo.setAlertTime(convertToUtcDate(alert.getString("startsAt")));
            alertRecordDo.setAlertReceiveTime(new Date());

            // 数据库记录告警记录
            alertRecordService.insert(alertRecordDo);

            // 设置通道沉默时间
            if (StringUtils.isNotEmpty(alertRecordDo.getClusterId())) {
                setSilence(alert, alertRecordDo.getClusterId());
            }
            // 如果集群id不为空，发送告警通知
            if (StringUtils.isNotEmpty(alertRecordDo.getClusterId())) {
                sendAlertMessage(alertRecordDo);
            }
        }
    }

    private void sendAlertMessage(AlertRecordDo alertRecordDo) {
        if (StringUtils.isNoneEmpty(alertRecordDo.getClusterId(), alertRecordDo.getNamespace(), alertRecordDo.getTargetName(), alertRecordDo.getAlertType())){
            // 备份告警是否开启判断
            if (alertRecordDo.getBackupAlert() != null && alertRecordDo.getBackupAlert()) {
                if (!middlewareAlertsService.getBackupAlert(alertRecordDo.getClusterId(), alertRecordDo.getNamespace(),
                    alertRecordDo.getTargetName())) {
                    return;
                } else {
                    alertRecordDo.setAlertType(SERVICE);
                }
            }
            // 获取告警通知用户
            List<AlertUserDo> alertUserDoList = alertUserService.listWithUserInfo(alertRecordDo.getClusterId(), alertRecordDo.getNamespace(), alertRecordDo.getTargetName(), alertRecordDo.getAlertType());

            // todo 钉钉通知

            // 邮件通知
            try {
                mailService.sendHtmlMail(alertRecordDo, alertUserDoList);
            } catch (Exception e){
                log.error("集群{} 发送告警{} 失败", alertRecordDo.getClusterId(), alertRecordDo.getAlertName(), e);
            }

            // 短信通知
            if (enableSMS) {
                alertNotifierService.sendSMSAlertMessage(alertRecordDo, alertUserDoList);
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

        JSONArray matchers = new JSONArray();
        JSONObject service = new JSONObject();
        if (alert.getJSONObject("labels").containsKey("service")) {
            service.put("name", "service");
            service.put("value", alert.getJSONObject("labels").getString("service"));
            service.put("isRegex", false);
            service.put("isEqual", true);
            matchers.add(service);
        }
        matchers.add(alertName);

        Date now = DateUtils.addInteger(new Date(), Calendar.HOUR_OF_DAY, -8);
        JSONObject body = new JSONObject();
        body.put("matchers", matchers);
        body.put("createdBy", "admin");
        body.put("comment", "silence");
        body.put("startsAt", DateUtils.dateToString(now, DateStyle.YYYY_MM_DD_T_HH_MM_SS_Z_SSS));
        String silence = alert.getJSONObject("annotations").getString("silence");
        if (silence == null) {
            QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("config_name", "Alertmanager_SilentTime");
            List<BeanSystemConfig> beanSystemConfigs = beanSystemConfigMapper.selectList(wrapper);
            if (!CollectionUtils.isEmpty(beanSystemConfigs)) {
                silence = beanSystemConfigs.get(0).getConfigValue();
            }
        }
        if (silence == null) {
            silence = silentTime;
        }
        body.put("endsAt",
            DateUtils.dateToString(calculateEndTime(now, silence), DateStyle.YYYY_MM_DD_T_HH_MM_SS_Z_SSS));
        alertManagerWrapper.setSilence(clusterId, NameConstant.ALERT_MANAGER_API_VERSION_SILENCES, body);
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

    /**
     * 将时间转为utc时间
     * @param time
     * @return
     */
    public static Date convertToUtcDate(String time) {
        String[] dateTimes = time.split("\\.");
        Date date = DateUtils.parseDate(dateTimes[0], "yyyy-MM-dd'T'HH:mm:ss");
        date = DateUtils.addInteger(date, Calendar.HOUR_OF_DAY, 8);
        return date;
    }

}
