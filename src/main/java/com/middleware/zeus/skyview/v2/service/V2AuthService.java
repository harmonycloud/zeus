package com.middleware.zeus.skyview.v2.service;

import com.alibaba.fastjson.JSONObject;

/**
 * @author xutianhong
 * @Date 2023/3/21 7:07 下午
 */
public interface V2AuthService {

    JSONObject login(String username, String password);

}
