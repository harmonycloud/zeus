package com.middleware.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.bean.BeanAlertRecord;
import com.middleware.zeus.common.model.AlertRecordDo;
import com.middleware.zeus.dao.BeanAlertRecordMapper;
import com.middleware.zeus.service.middleware.AlertRecordService;
import com.middleware.zeus.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2023/6/5 2:59 下午
 */
@Service
@Slf4j
public class AlertRecordServiceImpl implements AlertRecordService {

    @Value("${system.alert.record.keepDays:60}")
    private Integer alertRecordKeepDays;

    @Autowired
    private BeanAlertRecordMapper beanAlertRecordMapper;


    @Override
    public void insert(AlertRecordDo recordDo) {
        BeanAlertRecord beanAlertRecord = new BeanAlertRecord();
        beanAlertRecord.setName(recordDo.getTargetName());
        beanAlertRecord.setAliasName(recordDo.getTargetAliasName());
        beanAlertRecord.setNamespace(recordDo.getNamespace());
        beanAlertRecord.setType(recordDo.getMiddlewareType());
        beanAlertRecord.setLay(recordDo.getAlertType());
        beanAlertRecord.setClusterId(recordDo.getClusterId());
        beanAlertRecord.setAlert(recordDo.getAlertName());
        beanAlertRecord.setLevel(recordDo.getLevel());
        beanAlertRecord.setSummary(recordDo.getSummary());
        beanAlertRecord.setMessage(recordDo.getMessage());
        beanAlertRecord.setAlertTime(recordDo.getAlertTime());
        beanAlertRecord.setAlertReceiveTime(recordDo.getAlertReceiveTime());
        beanAlertRecordMapper.insert(beanAlertRecord);
    }

    @Override
    public void clear() {
        if (alertRecordKeepDays != null && alertRecordKeepDays > 0) {
            Date date = new Date();
            date = DateUtil.addDay(date, -alertRecordKeepDays);
            QueryWrapper<BeanAlertRecord> wrapper = new QueryWrapper<BeanAlertRecord>().le("alert_receive_time", date);
            beanAlertRecordMapper.delete(wrapper);
        }
    }
}
