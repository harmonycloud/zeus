package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupName;
import com.harmonycloud.zeus.dao.BeanMiddlewareBackupNameMapper;
import com.harmonycloud.zeus.dao.BeanProjectBackupServerMapper;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupNameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/2/9 2:51 下午
 */
@Service
public class MiddlewareBackupNameImpl implements MiddlewareBackupNameService {

    @Autowired
    private BeanMiddlewareBackupNameMapper middlewareBackupNameMapper;

    @Override
    public List<BeanMiddlewareBackupName> listByPositionId(Integer positionId) {
        QueryWrapper<BeanMiddlewareBackupName> wrapper = new QueryWrapper<>();
        wrapper.eq("position_id", positionId);
        return middlewareBackupNameMapper.selectList(wrapper);
    }

}
