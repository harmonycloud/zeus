package com.harmonycloud.zeus.service.middleware;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.ServletRequest;

/**
 * @author xutianhong
 * @Date 2022/10/11 10:53 上午
 */
public interface MiddlewareDashboardAuthService {

    /**
     * 登录
     *
     * @param username 用户名
     * @param password 秘密
     * @param type 中间件类型
     * @return JSONObject
     **/
    JSONObject login(String username, String password, String type);

}
