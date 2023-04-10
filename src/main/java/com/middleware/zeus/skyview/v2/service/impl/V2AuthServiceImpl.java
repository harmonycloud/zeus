package com.middleware.zeus.skyview.v2.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.zeus.skyview.v2.client.V2AuthServiceClient;
import com.middleware.zeus.skyview.v2.service.V2AuthService;
import com.middleware.zeus.util.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author xutianhong
 * @Date 2023/3/21 7:07 下午
 */
@Service
@Slf4j
public class V2AuthServiceImpl implements V2AuthService {

    @Value("${system.skyview.verifyCode:false}")
    private Boolean verifyCode;
    @Value("${system.skyview.encryptPassword:false}")
    private boolean encryptPassword;


    @Autowired
    private V2AuthServiceClient v2AuthServiceClient;

    @Override
    public JSONObject login(String username, String password) {
        if (encryptPassword){
            password = CryptoUtils.encrypt(password);
        }
        CaasResult<JSONObject> res;
        if (verifyCode){
            res = v2AuthServiceClient.loginWithVerify(username, password, null);
        } else {
            res = v2AuthServiceClient.login(username, password, null);
        }
        return res.getData();
    }
}
