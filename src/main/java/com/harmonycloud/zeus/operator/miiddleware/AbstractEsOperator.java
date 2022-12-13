package com.harmonycloud.zeus.operator.miiddleware;

import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.operator.AbstractBaseOperator;

/**
 * @author dengyulong
 * @date 2021/03/23
 * es通用处理
 */
public class AbstractEsOperator extends AbstractBaseOperator {

    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.ELASTIC_SEARCH == MiddlewareTypeEnum.findByType(middleware.getType());
    }

}
