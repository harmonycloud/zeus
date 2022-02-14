package com.harmonycloud.zeus.service.prometheus.impl;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.harmonycloud.zeus.bean.DingRobotInfo;
import com.harmonycloud.zeus.bean.MailToUser;
import com.harmonycloud.zeus.bean.user.BeanUser;
import com.harmonycloud.zeus.dao.DingRobotMapper;
import com.harmonycloud.zeus.dao.MailToUserMapper;
import com.harmonycloud.zeus.dao.user.BeanUserMapper;
import com.harmonycloud.zeus.util.DateUtil;
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
import com.harmonycloud.zeus.bean.MiddlewareAlertInfo;
import com.harmonycloud.zeus.dao.BeanAlertRecordMapper;
import com.harmonycloud.zeus.dao.MiddlewareAlertInfoMapper;
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
    private MiddlewareAlertInfoMapper middlewareAlertInfoMapper;
    @Autowired
    private MiddlewareAlertsServiceImpl middlewareAlertsServiceImpl;
    @Autowired
    private AlertManagerWrapper alertManagerWrapper;
    @Autowired
    private MailToUserMapper mailToUserMapper;
    @Autowired
    private DingRobotMapper dingRobotMapper;
    @Autowired
    private BeanUserMapper beanUserMapper;

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
            Date startDateTime = convertToUtcDate(alert.getString("startsAt"));
            beanAlertRecord.setTime(startDateTime);
            QueryWrapper<MiddlewareAlertInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("alert",labels.getString("alertname"))
                    .eq("namespace",labels.getString("namespace"))
                    .eq("middleware_name",labels.getString("service"))
                    .eq("cluster_id",clusterId);
            MiddlewareAlertInfo alertInfo = new MiddlewareAlertInfo();
            List<MiddlewareAlertInfo> alertInfos = middlewareAlertInfoMapper.selectList(wrapper);
            if (!CollectionUtils.isEmpty(alertInfos)) {
                alertInfo = alertInfos.get(0);
            }
            if (ObjectUtils.isEmpty(alertInfo)) {
                beanAlertRecord.setLay("service");
            } else {
                beanAlertRecord.setLay(alertInfo.getLay());
                beanAlertRecord.setAlertId(alertInfo.getAlertId());
                beanAlertRecord.setExpr(alertInfo.getDescription()+alertInfo.getSymbol()+alertInfo.getThreshold()+"%");
                beanAlertRecord.setContent(alertInfo.getContent());
            }
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
            //告警等级
            alertInfoDto.setLevel((String) JSON.parseObject(alertInfo.getLabels(), HashMap.class).get("severity"));
            //规则描述
            alertInfoDto.setDescription(alertInfo.getDescription()+alertInfo.getSymbol()+alertInfo.getThreshold()+"%");
            //告警内容
            alertInfoDto.setContent(alertInfo.getContent());
            //实际监测
            String ruleId = "";
            alertInfoDto.setMessage(annotations.getString("summary"));
            if (alertInfo.getAlertId() != null && beanAlertRecord.getId() != null) {
                ruleId = middlewareAlertsServiceImpl.calculateID(alertInfo.getAlertId()) + "-"
                        + middlewareAlertsServiceImpl.createId(beanAlertRecord.getId());
            }
            //告警ID
            alertInfoDto.setRuleID(ruleId);
            //ip
            alertInfoDto.setIp(alertInfo.getIp());
            //钉钉发送
            List<DingRobotInfo> dings = dingRobotMapper.selectList(new QueryWrapper<DingRobotInfo>());
            if ("ding".equals(alertInfo.getDing())) {
                dings.stream().forEach(dingRobotInfo -> {
                    dingRobotService.send(alertInfoDto,dingRobotInfo);
                });
            }
            //邮箱发送
            QueryWrapper<MailToUser> mailToUserQueryWrapper = new QueryWrapper<>();
            mailToUserQueryWrapper.eq("alert_rule_id",alertInfo.getAlertId());
            List<MailToUser> users = mailToUserMapper.selectList(mailToUserQueryWrapper);
            if ("mail".equals(alertInfo.getMail())) {
                users.stream().forEach(mailToUser -> {
                    QueryWrapper<BeanUser> userQueryWrapper = new QueryWrapper<>();
                    userQueryWrapper.eq("id",mailToUser.getUserId());
                    BeanUser beanUser = beanUserMapper.selectOne(userQueryWrapper);
                    try {
                        mailService.sendHtmlMail(alertInfoDto,beanUser);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                });
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
        date = DateUtils.addInteger(date, Calendar.HOUR_OF_DAY, 14);
        log.info("告警记录日期转换, time={}, date={}", time, date);
        return date;
    }

}
