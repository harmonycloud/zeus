package com.middleware.zeus.service.user;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
public interface AuthService {

    /**
     * 登录
     *
     * @param userName 用户名
     * @param password 密码
     * @param response 响应
     * @return
     */
    JSONObject login(String userName, String password, HttpServletResponse response) throws Exception;

    /**
     * 登出
     *
     * @param request  请求
     * @param response 响应
     * @return
     */
    String logout(HttpServletRequest request, HttpServletResponse response);

}
