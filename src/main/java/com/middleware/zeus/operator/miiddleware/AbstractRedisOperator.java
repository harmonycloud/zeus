package com.middleware.zeus.operator.miiddleware;

import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.model.middleware.Middleware;
import com.middleware.zeus.operator.AbstractBaseOperator;

/**
 * @author dengyulong
 * @date 2021/03/23
 * redis通用处理
 */
public class AbstractRedisOperator extends AbstractBaseOperator {

    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.REDIS == MiddlewareTypeEnum.findByType(middleware.getType());
    }

}
