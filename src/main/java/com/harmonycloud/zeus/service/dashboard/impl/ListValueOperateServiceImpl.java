package com.harmonycloud.zeus.service.dashboard.impl;

import com.harmonycloud.caas.common.enums.RedisDataTypeEnum;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.dashboard.redis.KeyValueDto;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.dashboard.RedisValueOperateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author liyinlong
 * @since 2022/10/28 3:35 下午
 */
@Slf4j
@Service
@Operator(paramTypes4One = String.class)
public class ListValueOperateServiceImpl implements RedisValueOperateService {

    @Override
    public boolean support(String dataType) {
        return RedisDataTypeEnum.TYPE_LIST.getType().equals(dataType);
    }

    @Override
    public void delete(String key, KeyValueDto keyValueDto) {
        
    }

    @Override
    public void update(String key, KeyValueDto keyValueDto) {
        
    }

}
