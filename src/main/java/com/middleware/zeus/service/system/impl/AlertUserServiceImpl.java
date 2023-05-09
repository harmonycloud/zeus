package com.middleware.zeus.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.model.AlertUserDo;
import com.middleware.zeus.bean.BeanAlertUser;
import com.middleware.zeus.dao.BeanAlertUserMapper;
import com.middleware.zeus.service.system.AlertUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2023/5/8 11:01 上午
 */
@Service
@Slf4j
public class AlertUserServiceImpl implements AlertUserService {

    @Autowired
    private BeanAlertUserMapper beanAlertUserMapper;

    @Override
    public List<AlertUserDo> list(String clusterId, String namespace, String name, String alertType) {
        // init wrapper
        QueryWrapper<BeanAlertUser> wrapper = new QueryWrapper<>();
        // set condition
        if (StringUtils.isNotEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        if (StringUtils.isNotEmpty(namespace)) {
            wrapper.eq("namespace", namespace);
        }
        if (StringUtils.isNotEmpty(name)) {
            wrapper.eq("name", name);
        }
        if (StringUtils.isNotEmpty(alertType)) {
            wrapper.eq("alert_type", alertType);
        }
        // list
        List<BeanAlertUser> beanAlertUserList = beanAlertUserMapper.selectList(wrapper);
        // convert
        return beanAlertUserList.stream().map(beanAlertUser -> {
            AlertUserDo alertUserDo = new AlertUserDo();
            BeanUtils.copyProperties(beanAlertUser, alertUserDo);
            return alertUserDo;
        }).collect(Collectors.toList());
    }

    @Override
    public void add(AlertUserDo alertUserDo) {
        // init object
        BeanAlertUser beanAlertUser = new BeanAlertUser();
        // copy
        BeanUtils.copyProperties(alertUserDo, beanAlertUser);
        // insert
        beanAlertUserMapper.insert(beanAlertUser);
    }

    @Override
    public void delete(String username, String clusterId, String namespace, String name, String alertType) {
        // init wrapper
        QueryWrapper<BeanAlertUser> wrapper = new QueryWrapper<>();
        // set condition
        if (StringUtils.isNotEmpty(username)) {
            wrapper.eq("username", username);
        }
        if (StringUtils.isNotEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        if (StringUtils.isNotEmpty(namespace)) {
            wrapper.eq("namespace", namespace);
        }
        if (StringUtils.isNotEmpty(name)) {
            wrapper.eq("name", name);
        }
        if (StringUtils.isNotEmpty(alertType)) {
            wrapper.eq("alert_type", alertType);
        }
        // delete
        beanAlertUserMapper.delete(wrapper);
    }
}
