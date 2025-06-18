package com.middleware.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.middleware.zeus.common.model.AlertDTO;
import com.middleware.zeus.common.model.AlertRecordQueryDto;
import com.middleware.zeus.bean.BeanAlertRecord;
import com.middleware.zeus.dao.BeanAlertRecordMapper;
import com.middleware.zeus.service.middleware.MiddlewareAlertRecordService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.middleware.zeus.common.constants.AlertConstant.SERVICE;
import static com.middleware.zeus.common.constants.CommonConstant.ASC;
import static com.middleware.zeus.common.constants.CommonConstant.DESC;

/**
 * @author liyinlong
 * @since 2023/4/6 3:07 下午
 */
@Service
public class MiddlewareAlertRecordServiceImpl implements MiddlewareAlertRecordService {

    @Autowired
    private BeanAlertRecordMapper alertRecordMapper;

    @Override
    public PageInfo<AlertDTO> list(String clusterId, String namespace, String middlewareName, AlertRecordQueryDto queryDto) {
        QueryWrapper<BeanAlertRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId);
        if (StringUtils.isNotEmpty(namespace)) {
            wrapper.eq("namespace", namespace);
        }
        if (StringUtils.isNotEmpty(middlewareName)){
            wrapper.eq("name", middlewareName);
        }
        if (StringUtils.isNotEmpty(queryDto.getAlertLevel())) {
            wrapper.eq("level", queryDto.getAlertLevel());
        }
        if (StringUtils.isNotEmpty(queryDto.getAlertType())) {
            wrapper.eq("lay", queryDto.getAlertType());
        }
        // 根据告警时间排序
        if (StringUtils.isNotEmpty(queryDto.getAlertTime())) {
            if (queryDto.getAlertTime().equals(ASC)) {
                wrapper.orderByAsc("alert_time");
            } else if (queryDto.getAlertTime().equals(DESC)) {
                wrapper.orderByDesc("alert_time");
            }
        }
        // 根据告警接收时间排序
        if (StringUtils.isNotEmpty(queryDto.getReceiveTime()) && queryDto.getReceiveTime().equals(ASC)) {
            wrapper.orderByAsc("alert_receive_time");
        } else {
            wrapper.orderByDesc("alert_receive_time");
        }
        // keyword根据告警信息进行查询
        if (StringUtils.isNotEmpty(queryDto.getKeyword())){
            wrapper.like("message", "%" + queryDto.getKeyword() + "%");
        }
        PageHelper.startPage(queryDto.getCurrent(), queryDto.getSize());
        // 查询告警记录数据
        List<BeanAlertRecord> alertRecordList = alertRecordMapper.selectList(wrapper);
        // 初始化返回数据
        PageInfo<AlertDTO> alertDtoPageInfo = new PageInfo<>();
        // 对数据库返回结果进行page封装
        PageInfo<BeanAlertRecord> alertRecordPageInfo = new PageInfo<>(alertRecordList);

        // 封装数据
        alertDtoPageInfo.setTotal(alertRecordPageInfo.getTotal());
        alertDtoPageInfo.setPages(alertRecordPageInfo.getPages());
        alertDtoPageInfo.setPageNum(alertRecordPageInfo.getPageNum());
        alertDtoPageInfo.setPageSize(alertRecordPageInfo.getPageSize());
        alertDtoPageInfo.setEndRow(alertRecordPageInfo.getEndRow());
        alertDtoPageInfo.setStartRow(alertRecordPageInfo.getStartRow());

        List<AlertDTO> alertDTOList = new ArrayList<>();
        alertRecordList.forEach(alertRecord -> {
            AlertDTO alertDTO = new AlertDTO();
            BeanUtils.copyProperties(alertRecord, alertDTO);
            alertDTO.setAlertType(alertDTO.getLay());
            alertDTOList.add(alertDTO);
        });
        alertDtoPageInfo.setList(alertDTOList);
//        BeanUtils.copyProperties(new PageInfo<>(alertRecordList), alertDtoPageInfo);
//        for (AlertDTO alertDTO : alertDtoPageInfo.getList()) {
//            alertDTO.setAlertType(alertDTO.getLay());
//        }
        return alertDtoPageInfo;
    }

}
