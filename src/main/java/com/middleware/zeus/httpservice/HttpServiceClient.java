package com.middleware.zeus.httpservice;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.Address;
import com.dtflys.forest.annotation.Get;
import com.dtflys.forest.annotation.Header;
import com.dtflys.forest.annotation.Var;

/**
 * @author liyinlong
 * @description http工具类
 * @since 2022/10/6 3:37 下午
 */
public interface HttpServiceClient {

    /**
     * 获取所有es版本
     * @param protocol
     * @param host
     * @param port
     * @param token
     * @return
     */
    @Get("/")
    @Address(scheme = "{0}", host = "{1}", port = "{2}")
    JSONObject clusterInfo(@Var("protocol") String protocol, @Var("host") String host, @Var("port") String port, @Header("Authorization") String token);

}
