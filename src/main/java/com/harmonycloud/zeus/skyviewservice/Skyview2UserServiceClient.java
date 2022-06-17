package com.harmonycloud.zeus.skyviewservice;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.harmonycloud.caas.common.base.CaasResult;

@Address(host = "10.10.103.188", port = "80")
public interface Skyview2UserServiceClient {

    @Post("/user/auth/login")
    CaasResult<JSONObject> login(@Query("username") String username, @Query("password") String password, @Query(value = "language", defaultValue = "ch") String anguage);

    @Get(url = "/caas/users/current", headers = {"Authorization: ${token}"})
    CaasResult<JSONObject> current(@Var("token") String token, @Query(value = "isLogin", defaultValue = "true") Boolean isLogin);

    @Get(url = "/user/users/details", headers = {"Authorization: ${token}"})
    CaasResult<JSONArray> listUser(@Var("token") String token, @Query(value = "param") String param);

}

