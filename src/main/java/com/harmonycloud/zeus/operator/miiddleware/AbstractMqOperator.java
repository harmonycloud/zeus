package com.harmonycloud.zeus.operator.miiddleware;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.operator.AbstractBaseOperator;

/**
 * @author dengyulong
 * @date 2021/03/23
 * mq通用处理
 */
public class AbstractMqOperator extends AbstractBaseOperator {

    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.ROCKET_MQ == MiddlewareTypeEnum.findByType(middleware.getType());
    }

}
