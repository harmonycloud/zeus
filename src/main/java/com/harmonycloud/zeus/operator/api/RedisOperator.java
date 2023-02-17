package com.harmonycloud.zeus.operator.api;

import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.operator.BaseOperator;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface RedisOperator extends BaseOperator {


    void createOpenService(Middleware middleware);
}
