package com.middleware.zeus.operator.miiddleware;

import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.common.model.middleware.Middleware;
import com.middleware.zeus.operator.AbstractBaseOperator;

/**
 * @author xutianhong
 * @Date 2024/12/9 4:51 PM
 */
public class AbstractMongodbOperator extends AbstractBaseOperator {
    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.MONGODB == MiddlewareTypeEnum.findByType(middleware.getType());
    }
}
