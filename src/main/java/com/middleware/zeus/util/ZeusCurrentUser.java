package com.middleware.zeus.util;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 中间件平台当前用户
 * @author liyinlong
 * @since 2022/6/20 11:47 上午
 */
public class ZeusCurrentUser {

    public static String getCaasToken() {
        CurrentUser currentUser = CurrentUserRepository.getUserExistNull();
        if (currentUser == null){
            return "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlckluZm8iOiJ7XCJyZWFsTmFtZVwiOlwiYWRtaW5cIixcInBob25lXCI6XCIxNTAwMDAwMDAwMFwiLFwicm9sZUlkXCI6XCIxXCIsXCJhdHRyaWJ1dGVzXCI6e30sXCJsYW5ndWFnZVwiOlwiY2hcIixcImlkXCI6XCIxXCIsXCJpc0FkbWluXCI6MSxcImVtYWlsXCI6XCJhZG1pbkBhZG1pbi5jb21cIixcInVzZXJuYW1lXCI6XCJhZG1pblwifSIsImV4cCI6MzMyMTE3NTY3NjIsImlhdCI6MTY3NTc1NTU2Mn0.2YQ5Yh99UHg-NrsW10thNbXuSU4ZqPlPfLyHQmnvBgg";
        }
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
