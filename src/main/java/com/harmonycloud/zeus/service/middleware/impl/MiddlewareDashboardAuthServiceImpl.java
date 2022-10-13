package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import com.harmonycloud.tool.encrypt.RSAUtils;
import com.harmonycloud.zeus.service.middleware.MiddlewareDashboardAuthService;
import com.harmonycloud.zeus.util.ApplicationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * @author xutianhong
 * @Date 2022/10/11 10:53 上午
 */
@Service
@Slf4j
public class MiddlewareDashboardAuthServiceImpl implements MiddlewareDashboardAuthService {
    @Override
    public JSONObject login(String username, String password, String type) {
        // 解密密码
        String decryptPassword;
        try {
            decryptPassword = RSAUtils.decryptByPrivateKey(password);
        } catch (Exception e) {
            throw new BusinessException(ErrorMessage.RSA_DECRYPT_FAILED);
        }
        // todo 根据类型前往不同的中间件尝试登录

        JSONObject res = new JSONObject();
        res.put("username", username);
        res.put("mwToken", "");
        return res;
    }
}
