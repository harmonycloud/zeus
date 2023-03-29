package com.middleware.zeus.skyview.v2.client;

import com.alibaba.fastjson.JSONArray;
import com.dtflys.forest.annotation.*;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.zeus.config.ForestUnauthorizedSuccessCondition;
import com.middleware.zeus.config.SkyviewAddressSource;
import com.middleware.zeus.interceptor.SkyviewInterceptor;
import org.springframework.stereotype.Component;

/**
 * @author xutianhong
 * @Date 2023/3/21 7:09 下午
 */
@Component
@Address(source = SkyviewAddressSource.class)
@BaseRequest(interceptor = SkyviewInterceptor.class)
@Success(condition = ForestUnauthorizedSuccessCondition.class)
public interface V2UserServiceClient {


    @Get(url = "#{system.skyview.prefix}/user/users/{username}/details")
    CaasResult<JSONArray> getUser(@Var("username") String username);

    @Get(url = "#{system.skyview.prefix}/user/users/details")
    CaasResult<JSONArray> listUser(@Query(value = "param") String param);

}
