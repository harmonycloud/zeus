package com.middleware.zeus.service.user.skyviewimpl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.enums.CaasErrorMessage;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.tool.encrypt.RSAUtils;
import com.middleware.zeus.service.user.impl.AuthServiceImpl;
import com.middleware.zeus.skyviewservice.Skyview2UserService;
import com.middleware.zeus.skyviewservice.client.Skyview2UserServiceClient;
import com.middleware.zeus.util.CaasResponseUtil;
import com.middleware.zeus.util.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.middleware.caas.filters.base.GlobalKey.SET_TOKEN;

/**
 * @author liyinlong
 * @since 2022/6/9 11:16 上午
 */
@Slf4j
@Service
@ConditionalOnProperty(value="system.usercenter",havingValue = "skyview2")
public class Skyview2AuthServiceImpl extends AuthServiceImpl {

    @Autowired
    private Skyview2UserServiceClient skyview2UserServiceClient;
    @Autowired
    private Skyview2UserService skyview2UserService;

    @Value("${system.skyview.encryptPassword:false}")
    private boolean encryptPassword;

    @Override
    public JSONObject login(String userName, String password, HttpServletResponse response) throws Exception {
        //解密密码
        String decryptPassword;
        try {
            decryptPassword = RSAUtils.decryptByPrivateKey(password);
        } catch (Exception e) {
            throw new BusinessException(ErrorMessage.RSA_DECRYPT_FAILED);
        }
        String tempPassword =  decryptPassword;
        if (encryptPassword) {
            tempPassword = CryptoUtils.encrypt(decryptPassword);
        }
        // 连接观云台，登录
        CaasResult<JSONObject> loginResult = skyview2UserService.login(userName, tempPassword, "ch");

        // 检查账号是否可用
        if (CaasResponseUtil.fitError(loginResult, CaasErrorMessage.AUTH_FAIL)) {
            throw new BusinessException(ErrorMessage.AUTH_FAILED);
        }
        // 检查账号是否密码过期
        if (CaasResponseUtil.fitError(loginResult, CaasErrorMessage.PASSWORD_IS_EXPIRED)) {
            throw new BusinessException(ErrorMessage.PASSWORD_IS_EXPIRED);
        }

        String caasToken = loginResult.getStringVal("token");

        CaasResult<JSONObject> currentResult = skyview2UserServiceClient.current(caasToken, true);

        Boolean isAdmin = currentResult.getBooleanVal("isAdmin");
        String realName = currentResult.getStringVal("realName");
        String userId =  currentResult.getStringVal("userId");

        JSONObject admin = convertUserInfo(userName, realName, userId, caasToken, isAdmin, password);
        log.info("用户信息：{}", admin);
        String token = generateToken(admin);
        response.setHeader(SET_TOKEN, token);
        convertResult(userName, isAdmin, token);
        //校验密码日期
        return convertResult(userName, isAdmin, token);
    }

    @Override
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        return super.logout(request, response);
    }

    public JSONObject convertUserInfo(String username, String realName, String userId, String caastoken, boolean isAdmin,String password) {
        JSONObject admin = new JSONObject();
        admin.put("username", username);
        admin.put("realName", realName);
        admin.put("aliasName", realName);
        admin.put("userId", userId);
        admin.put("phone", "");
        JSONObject attributes = new JSONObject();
        attributes.put("caastoken", caastoken);
        attributes.put("isAdmin", isAdmin);
        attributes.put("password", password);
        admin.put("attributes", attributes);
        return admin;
    }

}
