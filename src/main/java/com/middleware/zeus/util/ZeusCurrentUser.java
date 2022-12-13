package com.middleware.zeus.util;

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
        Map<String, String> attributes = currentUser.getAttributes();
        return attributes.get("caastoken");
    }

    public static boolean isAdmin() {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        Map<String, String> attributes = currentUser.getAttributes();
        return Boolean.parseBoolean(attributes.get("isAdmin"));
    }

    public static String getUserName(){
        return CurrentUserRepository.getUser().getUsername();
    }

    public static String getNickName(){
        return CurrentUserRepository.getUser().getNickname();
    }

}
