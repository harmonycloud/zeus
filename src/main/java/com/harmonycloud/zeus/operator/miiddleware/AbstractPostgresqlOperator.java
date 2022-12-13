package com.harmonycloud.zeus.operator.miiddleware;

import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.operator.AbstractBaseOperator;

/**
 * @author xutianhong
 * @Date 2022/6/7 2:54 下午
 */
public class AbstractPostgresqlOperator extends AbstractBaseOperator {

    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.POSTGRESQL == MiddlewareTypeEnum.findByType(middleware.getType());
    }

}
