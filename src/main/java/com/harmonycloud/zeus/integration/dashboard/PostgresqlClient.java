package com.harmonycloud.zeus.integration.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.dtflys.forest.http.ForestResponse;
import com.harmonycloud.zeus.interceptor.MiddlewareApiInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;

/**
 * @author xutianhong
 * @Date 2022/10/11 3:08 下午
 */
@Component
@Address
@BaseRequest(interceptor = MiddlewareApiInterceptor.class)
public interface PostgresqlClient {

    /**
     * 查询database列表
     */
    @Get(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases")
    JSONObject listDatabases(@Var("name") String name, @Var("port") String port);

    /**
     * 查询user列表
     */
    @Get(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/user")
    JSONObject listUsers(@Var("name") String name, @Var("port") String port);

    /**
     * 创建用户
     */
    @Post(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/user")
    JSONObject addUser(@Var("name") String name, @Var("port") String port, @Body("username") String username,
        @Body("password") String password);

    /**
     * 创建用户
     */
    @Delete(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/user/{username}")
    JSONObject dropUser(@Var("name") String name, @Var("port") String port, @Var("username") String username);

}
