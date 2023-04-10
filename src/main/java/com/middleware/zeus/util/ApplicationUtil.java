package com.middleware.zeus.util;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
/**
 * @author xutianhong
 * @Date 2022/2/15 2:00 下午
 */
@Component
public class ApplicationUtil {

    public static Double expire;

    @Value("${system.user.expire:0.5}")
    public void setExpire(Double expire) {
        ApplicationUtil.expire = expire;
    }

    public static Double getExpire(){
        return expire;
    }

}
