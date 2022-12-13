package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.tool.encrypt.RSAUtils;
import com.harmonycloud.zeus.service.AbstractBaseService;
import com.harmonycloud.zeus.service.dashboard.BaseMiddlewareApiService;
import com.harmonycloud.zeus.service.middleware.MiddlewareDashboardAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author xutianhong
 * @Date 2022/10/11 10:53 上午
 */
@Service
@Slf4j
public class MiddlewareDashboardAuthServiceImpl extends AbstractBaseService implements MiddlewareDashboardAuthService {

    @Override
    public JSONObject login(String clusterId, String namespace, String middlewareName, String username, String password,
        String type) {
        // 解密密码
        String decryptPassword;
        try {
            decryptPassword = RSAUtils.decryptByPrivateKey(password);
        } catch (Exception e) {
            throw new BusinessException(ErrorMessage.RSA_DECRYPT_FAILED);
        }
        // 根据类型前往不同的中间件尝试登录
        BaseMiddlewareApiService service =
            getOperator(BaseMiddlewareApiService.class, BaseMiddlewareApiService.class, type);
        String token = service.login(clusterId, namespace, middlewareName, username, decryptPassword);

        JSONObject res = new JSONObject();
        res.put("username", username);
        res.put("mwToken", token);
        return res;
    }

    @Override
    public void logout(String clusterId, String namespace, String middlewareName, String type) {
        // 根据类型前往不同的中间件尝试登录
        BaseMiddlewareApiService service =
                getOperator(BaseMiddlewareApiService.class, BaseMiddlewareApiService.class, type);
        service.logout(clusterId, namespace, middlewareName);
    }
}
