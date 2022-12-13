package com.harmonycloud.zeus.operator.miiddleware;

import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.operator.AbstractBaseOperator;

/**
 * @author liyinlong
 * @since 2021/10/22 3:22 下午
 */
public class AbstractZookeeperOperator extends AbstractBaseOperator {

    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.ZOOKEEPER == MiddlewareTypeEnum.findByType(middleware.getType());
    }
}
