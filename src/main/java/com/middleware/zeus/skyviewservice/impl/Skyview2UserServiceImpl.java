package com.middleware.zeus.skyviewservice.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.zeus.skyviewservice.Skyview2UserService;
import com.middleware.zeus.skyviewservice.client.Skyview2UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author xutianhong
 * @Date 2022/9/13 10:14 上午
 */
@Service("skyview2UserService")
@Slf4j
public class Skyview2UserServiceImpl implements Skyview2UserService {

    @Value("${system.skyview.verifyCode:false}")
    private Boolean verifyCode;

    @Autowired
    private Skyview2UserServiceClient skyview2UserServiceClient;

    @Override
    public CaasResult<JSONObject> login(String username, String password, String language) {
        if (!verifyCode){
            return skyview2UserServiceClient.login(username, password, language);
        }else {
            return skyview2UserServiceClient.loginWithVerify(username, password, language);
        }
    }
}