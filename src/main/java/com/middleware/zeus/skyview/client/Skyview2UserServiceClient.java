package com.middleware.zeus.skyview.client;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.middleware.zeus.common.base.CaasResult;
import com.middleware.zeus.config.ForestSuccessCondition;
import com.middleware.zeus.config.ForestUnauthorizedSuccessCondition;
import com.middleware.zeus.config.SkyviewAddressSource;

@Address(source = SkyviewAddressSource.class)
public interface Skyview2UserServiceClient {

    @Success(condition = ForestUnauthorizedSuccessCondition.class)
    @Post("#{system.skyview.prefix}/user/auth/login")
    CaasResult<JSONObject> login(@Query("username") String username, @Query("password") String password, @Query(value = "language", defaultValue = "ch") String anguage);

    @Success(condition = ForestUnauthorizedSuccessCondition.class)
    @Post("#{system.skyview.prefix}/user/auth/openapi/login")
    CaasResult<JSONObject> loginWithVerify(@Query("username") String username, @Query("password") String password, @Query(value = "language", defaultValue = "ch") String anguage);

    @Get(url = "#{system.skyview.prefix}/caas/users/current", headers = {"Authorization: ${token}"})
    CaasResult<JSONObject> current(@Var("token") String token, @Query(value = "isLogin", defaultValue = "true") Boolean isLogin);

    @Success(condition = ForestSuccessCondition.class)
    @Get(url = "#{system.skyview.prefix}/caas/users/current", headers = {"Authorization: ${token}"})
    void currentWithHandleException(@Var("token") String token, @Query(value = "isLogin", defaultValue = "true") Boolean isLogin);

}

