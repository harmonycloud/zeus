package com.middleware.zeus.operator.miiddleware;

import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.model.middleware.Middleware;
import com.middleware.zeus.operator.AbstractBaseOperator;

/**
 * kafka通用处理
 * @author  liyinlong
 * @since 2021/9/9 2:13 下午
 */
public class AbstractKafkaOperator extends AbstractBaseOperator {

    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.KAFKA == MiddlewareTypeEnum.findByType(middleware.getType());
    }

}
