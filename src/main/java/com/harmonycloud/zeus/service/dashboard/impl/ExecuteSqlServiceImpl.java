package com.harmonycloud.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.model.dashboard.ExecuteSqlDto;
import com.harmonycloud.zeus.bean.BeanSqlExecuteRecord;
import com.harmonycloud.zeus.dao.BeanSqlExecuteRecordMapper;
import com.harmonycloud.zeus.service.dashboard.ExecuteSqlService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.ASCEND;

/**
 * @author xutianhong
 * @Date 2022/10/22 9:44 上午
 */
@Service
@Slf4j
public class ExecuteSqlServiceImpl implements ExecuteSqlService {

    @Autowired
    private BeanSqlExecuteRecordMapper beanSqlExecuteRecordMapper;

    @Override
    public void insert(BeanSqlExecuteRecord beanSqlExecuteRecord) {
        beanSqlExecuteRecordMapper.insert(beanSqlExecuteRecord);
    }

    @Override
    public PageInfo<ExecuteSqlDto> list(String keyword, Integer current, Integer size, String order) {
        PageHelper.startPage(current, size);
        QueryWrapper<BeanSqlExecuteRecord> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(keyword)) {
            wrapper.like("sqlStr", "%" + keyword + "%");
        }
        if (StringUtils.isNotEmpty(order)) {
            JSONObject or = JSONObject.parseObject(order);
            for (String key : or.keySet()) {
                wrapper.orderBy(true, or.getString(key).equals(ASCEND), key);
            }
        }
        List<BeanSqlExecuteRecord> recordList = beanSqlExecuteRecordMapper.selectList(wrapper);
        PageInfo<ExecuteSqlDto> executeSqlDtoPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(new PageInfo<>(recordList), executeSqlDtoPageInfo);
        executeSqlDtoPageInfo.setList(recordList.stream().map(record -> {
            ExecuteSqlDto executeSqlDto = new ExecuteSqlDto();
            executeSqlDto.setSql(record.getSqlStr());
            executeSqlDto.setMessage(record.getMessage());
            executeSqlDto.setDate(record.getExecDate());
            executeSqlDto.setTime(record.getExecTime());
            executeSqlDto.setDatabase(record.getTargetDatabase());
            executeSqlDto.setStatus(record.getStatus());
            return executeSqlDto;
        }).collect(Collectors.toList()));
        return executeSqlDtoPageInfo;
    }
}
