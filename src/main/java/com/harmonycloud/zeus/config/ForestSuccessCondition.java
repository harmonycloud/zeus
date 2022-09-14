package com.harmonycloud.zeus.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.callback.SuccessWhen;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.harmonycloud.caas.common.base.CaasResult;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.tool.encrypt.RSAUtils;
import com.harmonycloud.zeus.skyviewservice.Skyview2UserService;
import com.harmonycloud.zeus.util.CryptoUtils;

import com.harmonycloud.zeus.skyviewservice.Skyview2UserServiceClient;
import com.harmonycloud.zeus.util.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

// 自定义成功/失败条件实现类
// 需要实现 SuccessWhen 接口
@Slf4j
@Configuration
public class ForestSuccessCondition implements SuccessWhen {

    @Autowired
    private Skyview2UserService skyview2UserService;

    @Value("${system.skyview.encryptPassword:false}")
    private boolean encryptPassword;
    /**
     * 请求成功条件
     * @param req Forest请求对象
     * @param res Forest响应对象
     * @return 是否成功，true: 请求成功，false: 请求失败
     */
    @Override
    public boolean successWhen(ForestRequest req, ForestResponse res) {
        // req 为Forest请求对象，即 ForestRequest 类实例
        // res 为Forest响应对象，即 ForestResponse 类实例
        // 返回值为 ture 则表示请求成功，false 表示请求失败
        CurrentUser user = CurrentUserRepository.getUser();
        Map<String, String> attributes = user.getAttributes();
        log.info(res.getContent());
        if (res.getStatusCode() == 401) {
            String username = user.getUsername();
            String password = attributes.get("password");
            try {
                String decryptPassword = RSAUtils.decryptByPrivateKey(password);
                String tempPassword =  decryptPassword;
                if (encryptPassword) {
                    tempPassword = CryptoUtils.encrypt(decryptPassword);
                }
                CaasResult<JSONObject> loginResult = skyview2UserService.login(username, tempPassword, "ch");
                String token = loginResult.getStringVal("token");
                attributes.put("caastoken", token);
            } catch (Exception e) {
                log.error("登录失败", e);
            }
        }
        return res.noException();
    }

}