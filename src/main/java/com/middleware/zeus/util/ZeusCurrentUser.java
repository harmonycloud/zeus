package com.middleware.zeus.util;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;

import java.util.Map;

/**
 * 中间件平台当前用户
 * @author liyinlong
 * @since 2022/6/20 11:47 上午
 */
public class ZeusCurrentUser {

    public static String getCaasToken() {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        JSONObject userInfo = JwtTokenComponent.getClaimsFromToken("userInfo", currentUser.getToken());
        return userInfo.getString("caasToken");
    }

    public static Boolean isAdmin(){
        CurrentUser currentUser = CurrentUserRepository.getUser();
        JSONObject userInfo = JwtTokenComponent.getClaimsFromToken("userInfo", currentUser.getToken());
        return userInfo.getBoolean("isAdmin");
    }

    public static String getUserName(){
        return CurrentUserRepository.getUser().getUsername();
    }

    public static String getNickName(){
        return CurrentUserRepository.getUser().getNickname();
    }

}
