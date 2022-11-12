package com.harmonycloud.zeus.service.prometheus.impl;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.harmonycloud.caas.common.constants.AlertConstant;
import com.harmonycloud.caas.common.model.AlertSettingDTO;
import com.harmonycloud.zeus.bean.DingRobotInfo;
import com.harmonycloud.zeus.bean.BeanMailToUser;
import com.harmonycloud.zeus.bean.user.BeanUser;
import com.harmonycloud.zeus.dao.*;
import com.harmonycloud.zeus.dao.user.BeanUserMapper;
import com.harmonycloud.zeus.service.middleware.MiddlewareAlertsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.constants.DateStyle;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.DateUnitEnum;
import com.harmonycloud.caas.common.model.middleware.AlertInfoDto;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.bean.BeanAlertRecord;
import com.harmonycloud.zeus.bean.AlertRuleId;
import com.harmonycloud.zeus.integration.cluster.AlertManagerWrapper;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareAlertsServiceImpl;
import com.harmonycloud.zeus.service.prometheus.PrometheusWebhookService;
import com.harmonycloud.zeus.service.user.DingRobotService;
import com.harmonycloud.zeus.service.user.MailService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.mail.MessagingException;

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
    private DingRobotService dingRobotService;
    @Autowired
    private MailService mailService;
    @Autowired
    private AlertRuleIdMapper alertRuleIdMapper;
    @Autowired
    private MiddlewareAlertsServiceImpl middlewareAlertsServiceImpl;
    @Autowired
    private AlertManagerWrapper alertManagerWrapper;
    @Autowired
    private BeanMailToUserMapper beanMailToUserMapper;
    @Autowired
    private DingRobotMapper dingRobotMapper;
    @Autowired
    private BeanUserMapper beanUserMapper;
    @Autowired
    private MiddlewareAlertsService middlewareAlertsService;

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
            if ("Pod_all_cpu_usage".equals(labels.getString("alertname"))){
                continue;
            }

            String clusterId = labels.getOrDefault("clusterId", "").toString();
            String namespace = labels.getString("namespace");
            String middlewareName = labels.getString("service");

            BeanAlertRecord beanAlertRecord = new BeanAlertRecord();
            beanAlertRecord.setName(labels.getString("service"));
            beanAlertRecord.setNamespace(labels.getString("namespace"));
            beanAlertRecord.setType(labels.getString("middleware"));

            beanAlertRecord.setClusterId(clusterId);
            beanAlertRecord.setAlert(labels.getString("alertname"));
            beanAlertRecord.setLevel(labels.getString("severity"));
            beanAlertRecord.setSummary(annotations.getString("summary"));
            beanAlertRecord.setMessage(annotations.getString("message"));
            Date startDateTime = convertToUtcDate(alert.getString("startsAt"));
            beanAlertRecord.setTime(startDateTime);
            QueryWrapper<AlertRuleId> wrapper = new QueryWrapper<>();
            wrapper.eq("alert", labels.getString("alertname"))
                    .eq("namespace", labels.getString("namespace"))
                    .eq("middleware_name", labels.getString("service"))
                    .eq("cluster_id", clusterId);
            AlertRuleId alertInfo = new AlertRuleId();
            List<AlertRuleId> alertInfos = alertRuleIdMapper.selectList(wrapper);
            if (!CollectionUtils.isEmpty(alertInfos)) {
                alertInfo = alertInfos.get(0);
                beanAlertRecord.setLay(StringUtils.isNotEmpty(alertInfo.getLay()) ? alertInfo.getLay() : "service");
                beanAlertRecord.setAlertId(alertInfo.getAlertId());
                beanAlertRecord.setExpr(alertInfo.getDescription() + alertInfo.getSymbol() + alertInfo.getThreshold() + "%");
                beanAlertRecord.setContent(alertInfo.getContent() == null ? "" : alertInfo.getContent());
            } else {
                beanAlertRecord.setLay("service");
            }
            String lay = beanAlertRecord.getLay();
            beanAlertRecordMapper.insert(beanAlertRecord);
            // 设置通道沉默时间
            if (annotations.containsKey("silence") && StringUtils.isNotEmpty(clusterId)) {
                setSilence(alert, clusterId);
            }
            if (ObjectUtils.isEmpty(alertInfo)) {
                return;
            }
            //中间件告警信息
            AlertInfoDto alertInfoDto = new AlertInfoDto();
            //告警指标
            alertInfoDto.setClusterId(clusterId);

            //告警时间
            alertInfoDto.setAlertTime(startDateTime);
            HashMap alertLabels = JSON.parseObject(alertInfo.getLabels(), HashMap.class);
            if (alertLabels == null) {
                continue;
            }
            //告警等级
            alertInfoDto.setLevel((String) alertLabels.get("severity"));
            //规则描述
            alertInfoDto.setDescription(alertInfo.getDescription() + alertInfo.getSymbol() + alertInfo.getThreshold() + "%");
            //告警内容
            alertInfoDto.setContent(StringUtils.isBlank(alertInfo.getContent()) ? "/" : alertInfo.getContent());
            //实际监测
            alertInfoDto.setMessage(annotations.getString("summary"));
            //设置中间件名称
            alertInfoDto.setMiddlewareName(alertInfo.getMiddlewareName());
            //ip
            QueryWrapper<AlertRuleId> queryWrapper = new QueryWrapper<>();
            queryWrapper.isNotNull("ip");
            List<AlertRuleId> alertRuleIds = alertRuleIdMapper.selectList(queryWrapper);
            if (!CollectionUtils.isEmpty(alertRuleIds)) {
                alertInfo.setIp(alertRuleIds.get(0).getIp());
            }
            // 发送告警信息
            sendAlertMessage(lay, clusterId, namespace, middlewareName, alertInfo, alertInfoDto);
        }
    }

    private void sendAlertMessage(String lay, String clusterId, String namespace, String middlewareaName, AlertRuleId alertInfo, AlertInfoDto alertInfoDto) {
        AlertSettingDTO alertSettingDTO;
        if (AlertConstant.LAY_SYSTEM.equals(lay)) {
            alertSettingDTO = middlewareAlertsService.queryAlertSetting();
        } else {
            alertSettingDTO = middlewareAlertsService.queryAlertSetting(clusterId, namespace, middlewareaName);
        }
        if (alertSettingDTO == null){
            log.error("集群{} 未查到相关告警设置", clusterId);
            return;
        }

        if (Boolean.TRUE.equals(alertSettingDTO.getEnableDingAlert())) {
            //钉钉发送
            List<DingRobotInfo> dings = dingRobotMapper.selectList(new QueryWrapper<DingRobotInfo>());
            dings.forEach(dingRobotInfo -> {
                dingRobotService.send(alertInfoDto, dingRobotInfo);
            });
        }
        if (Boolean.TRUE.equals(alertSettingDTO.getEnableMailAlert())) {
            //邮箱发送
            alertSettingDTO.getUserList().forEach(mailToUser -> {
                QueryWrapper<BeanUser> userQueryWrapper = new QueryWrapper<>();
                userQueryWrapper.eq("id", mailToUser.getId());
                BeanUser beanUser = beanUserMapper.selectOne(userQueryWrapper);
                try {
                    mailService.sendHtmlMail(alertInfoDto, beanUser);
                } catch (IOException | MessagingException e) {
                    e.printStackTrace();
                }
            });
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
     * 转换时间格式
     */
    public Date convertTime(String time) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date date = sdf.parse(time);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR,calendar.get(Calendar.HOUR)+8);
        return calendar.getTime();
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
