package com.harmonycloud.zeus.service.user.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.LdapConfigDto;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import com.harmonycloud.zeus.service.user.AbstractAuthService;
import com.harmonycloud.zeus.service.user.AuthManager4Ldap;
import com.harmonycloud.zeus.service.user.LdapService;
import com.harmonycloud.zeus.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

import static com.harmonycloud.caas.filters.base.GlobalKey.SET_TOKEN;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Service
@ConditionalOnProperty(value="system.usercenter",havingValue = "zeus")
public class AuthServiceImpl extends AbstractAuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthManager4Ldap authManager4Ldap;
    @Autowired
    private LdapService ldapService;

    @Override
    public JSONObject login(String userName, String password, HttpServletResponse response) throws Exception {
        //解密密码
        String decryptPassword = password;
//        try {
//            decryptPassword = RSAUtils.decryptByPrivateKey(password);
//        } catch (Exception e) {
//            throw new BusinessException(ErrorMessage.RSA_DECRYPT_FAILED);
//        }
        //md5加密
        String md5Password = PasswordUtils.md5(decryptPassword);
        // 获取ldap配置信息
        LdapConfigDto ldapConfigDto = ldapService.queryLdapDetail();

        UserDto userDto;
        if (userName.equals("admin")) {
            userDto = userService.getUserDto(userName);
        } else if (isLdapOn(ldapConfigDto)) {
            userDto = authManager4Ldap.auth(userName, decryptPassword, ldapConfigDto);
        } else {
            userDto = userService.getUserDto(userName);
        }

        //校验用户权限
        Boolean isAdmin = checkAuth(userDto);
        //校验密码
        if (!md5Password.equals(userDto.getPassword())) {
            throw new BusinessException(ErrorMessage.AUTH_FAILED);
        }

        JSONObject admin = convertUserInfo(userDto);
        String token = generateToken(admin);
        response.setHeader(SET_TOKEN, token);
        JSONObject res = convertResult(userName, isAdmin, token);
        //校验密码日期
        if (userDto.getPasswordTime() != null) {
            long passwordTime = DateUtils.getIntervalDays(new Date(), userDto.getPasswordTime()) / 3600 / 24 / 1000;
            if (passwordTime > 165) {
                res.put("rePassword", passwordTime);
            }
        }
        return res;
    }

    @Override
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        return super.logout(request, response);
    }

    public JSONObject convertUserInfo(UserDto userDto){
        JSONObject admin = new JSONObject();
        admin.put("username", userDto.getUserName());
        admin.put("roleId", userDto.getRoleId());
        admin.put("aliasName", userDto.getAliasName());
        admin.put("roleName", userDto.getRoleName());
        admin.put("phone", userDto.getPhone());
        admin.put("email", userDto.getEmail());
        return admin;
    }

    /**
     * 校验用户角色权限
     */
    public Boolean checkAuth(UserDto userDto){
        if (CollectionUtils.isEmpty(userDto.getUserRoleList())){
            throw new BusinessException(ErrorMessage.USER_ROLE_NOT_EXIT);
        }
        return userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getRoleId() == 1);
    }

    private boolean isLdapOn(LdapConfigDto ldapConfigDto) {
        if (ldapConfigDto != null && ldapConfigDto.getIsOn() != null && ldapConfigDto.getIsOn() == 1) {
            return true;
        }
        return false;
    }
}
