package com.middleware.zeus.service.user.abstractService;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.tool.encrypt.RSAUtils;
import com.middleware.zeus.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2023/3/21 5:26 下午
 */
public abstract class AbstractAuthService {

    @Value("${system.user.expire:0.5}")
    private Double expireTime;

    @Autowired
    protected UserService userService;

    public String decrypt(String password){
        String decryptPassword;
        try {
            decryptPassword = RSAUtils.decryptByPrivateKey(password);
        } catch (Exception e) {
            throw new BusinessException(ErrorMessage.RSA_DECRYPT_FAILED);
        }
        return decryptPassword;
    }

    /**
     * 组装返回结果
     * @param userName
     * @param isAdmin
     * @param token
     * @return
     */
    protected JSONObject convertResult(String userName, boolean isAdmin, String token){
        JSONObject res = new JSONObject();
        res.put("userName", userName);
        res.put("token", token);
        res.put("isAdmin", isAdmin);
        return res;
    }

    public JSONObject convertUserInfo(UserDto userDto){
        JSONObject user = new JSONObject();
        user.put("username", userDto.getUserName());
        user.put("roleId", userDto.getRoleId());
        user.put("aliasName", userDto.getAliasName());
        user.put("roleName", userDto.getRoleName());
        user.put("phone", userDto.getPhone());
        user.put("email", userDto.getEmail());
        user.put("isAdmin", userDto.getIsAdmin());

        return user;
    }

    /**
     * 生成token
     * @param userInfo 用户信息
     * @return
     */
    public String generateToken(JSONObject userInfo){
        long currentTime = System.currentTimeMillis();
        return JwtTokenComponent.generateToken("userInfo", userInfo,
                new Date(currentTime + (long)(expireTime * 3600000L)), new Date(currentTime - 300000L));
    }

}
