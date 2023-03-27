package com.middleware.zeus.skyview.v2.client;

import com.dtflys.forest.annotation.*;
import com.middleware.zeus.config.ForestUnauthorizedSuccessCondition;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.zeus.config.SkyviewAddressSource;
import com.middleware.zeus.interceptor.SkyviewInterceptor;

/**
 * @author xutianhong
 * @Date 2023/3/21 7:09 下午
 */
@Component
@Address(source = SkyviewAddressSource.class)
@BaseRequest(interceptor = SkyviewInterceptor.class)
@Success(condition = ForestUnauthorizedSuccessCondition.class)
public interface V2AuthServiceClient {

    @Post("#{system.skyview.prefix}/user/auth/login")
    CaasResult<JSONObject> login(@Query("username") String username, @Query("password") String password, @Query(value = "language", defaultValue = "ch") String language);

    @Post("#{system.skyview.prefix}/user/auth/openapi/login")
    CaasResult<JSONObject> loginWithVerify(@Query("username") String username, @Query("password") String password, @Query(value = "language", defaultValue = "ch") String language);

}
