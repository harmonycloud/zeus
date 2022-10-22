package com.harmonycloud.zeus.service.dashboard.impl;

import com.harmonycloud.caas.common.model.dashboard.ExecuteSqlDto;
import com.harmonycloud.zeus.bean.BeanSqlExecuteRecord;
import com.harmonycloud.zeus.dao.BeanSqlExecuteRecordMapper;
import com.harmonycloud.zeus.service.dashboard.ExecuteSqlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public void insert(ExecuteSqlDto executeSqlDto) {
        BeanSqlExecuteRecord beanSqlExecuteRecord = new BeanSqlExecuteRecord();
        BeanUtils.copyProperties(executeSqlDto, beanSqlExecuteRecord);
        beanSqlExecuteRecordMapper.insert(beanSqlExecuteRecord);
    }
}
