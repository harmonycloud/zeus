package com.harmonycloud.zeus.service.dashboard;

import com.harmonycloud.caas.common.model.dashboard.redis.KeyValueDto;
import com.harmonycloud.zeus.service.ingress.AbstractBaseOperator;

/**
 * @author liyinlong
 * @since 2022/10/28 3:31 下午
 */
public interface RedisValueOperateService {

    boolean support(String dataType);

    void delete(String key, KeyValueDto keyValueDto);
    
    void update(String key, KeyValueDto keyValueDto);

}
