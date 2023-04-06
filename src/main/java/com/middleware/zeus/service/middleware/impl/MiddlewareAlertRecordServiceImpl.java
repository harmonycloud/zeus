package com.middleware.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.middleware.zeus.bean.BeanAlertRecord;
import com.middleware.zeus.dao.BeanAlertRecordMapper;
import com.middleware.zeus.service.middleware.MiddlewareAlertRecordService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/4/6 3:07 下午
 */
@Service
public class MiddlewareAlertRecordServiceImpl implements MiddlewareAlertRecordService {

    @Autowired
    private BeanAlertRecordMapper alertRecordMapper;

    @Override
    public List<BeanAlertRecord> list(String clusterId, String namespace, String middlewareName, Integer current, Integer size, String keyword, String level, Boolean normalTimeOrder) {
        QueryWrapper<BeanAlertRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId);
        if (StringUtils.isNotEmpty(namespace)) {
            wrapper.eq("namespace", namespace);
        }
        if (StringUtils.isNotEmpty(level)) {
            wrapper.eq("level", level);
        }
        wrapper.orderByAsc("time");
        if (normalTimeOrder != null && !normalTimeOrder) {
            wrapper.orderByDesc("time");
        }
        wrapper.eq("name", middlewareName);
        if (StringUtils.isNotEmpty(keyword)) {
            wrapper.like("expr", keyword);
        }
        PageHelper.startPage(current, size);
        List<BeanAlertRecord> recordList = alertRecordMapper.selectList(wrapper);
        return recordList;
    }

}
