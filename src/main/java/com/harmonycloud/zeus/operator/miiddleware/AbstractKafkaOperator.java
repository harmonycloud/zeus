package com.harmonycloud.zeus.operator.miiddleware;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.operator.AbstractBaseOperator;

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
