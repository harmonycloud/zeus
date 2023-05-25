package com.middleware.zeus.util.middleware;

import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;

/**
 * @author liyinlong
 * @since 2023/5/25 2:03 下午
 */
public class MiddlewareModeUtil {

    /**
     * 检查中间件模式是否是双活支持的模式
     * @param type
     * @param mode
     * @return
     */
    public static Boolean activeActiveModeCheck(String type,String mode){
        if(MiddlewareTypeEnum.MYSQL.getType().equals(type)){
            return !"1m-0s".equals(mode);
        }else if(MiddlewareTypeEnum.POSTGRESQL.getType().equals(type)){
            return !"1m-0s".equals(mode);
        }else if(MiddlewareTypeEnum.REDIS.getType().equals(type)){
            return "sentinel".equals(mode);
        }
        return false;
    }
}
