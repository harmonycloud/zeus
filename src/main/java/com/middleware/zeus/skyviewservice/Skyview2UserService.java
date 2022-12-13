package com.middleware.zeus.skyviewservice;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;

/**
 * @author xutianhong
 * @Date 2022/9/13 10:16 上午
 */
public interface Skyview2UserService {

    /**
     * 登录
     *
     * @param username 用户名
     * @param password 密码
     * @param language 语言
     *
     * @return CaasResult<JSONObject>
     */
    CaasResult<JSONObject> login(String username, String password, String language);

}
