package com.harmonycloud.zeus.service.user.impl;

import static com.harmonycloud.caas.filters.base.GlobalKey.SET_TOKEN;
import static com.harmonycloud.caas.filters.base.GlobalKey.USER_TOKEN;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.harmonycloud.caas.common.model.LdapConfigDto;
import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.encrypt.RSAUtils;
import com.harmonycloud.zeus.service.user.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import org.springframework.util.CollectionUtils;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Value("${system.user.expire:0.5}")
    private Double expireTime;

    @Autowired
    private AuthManager4Ldap authManager4Ldap;
    @Autowired
    private LdapService ldapService;

    @Override
    public JSONObject login(String userName, String password, HttpServletResponse response) throws Exception {
        //解密密码
        String decryptPassword;
        try {
            decryptPassword = RSAUtils.decryptByPrivateKey(password);
            decryptPassword = password;
        } catch (Exception e) {
            throw new BusinessException(ErrorMessage.RSA_DECRYPT_FAILED);
        }
        //md5加密
        String md5Password = PasswordUtils.md5(decryptPassword);
        // 获取ldap配置信息
        LdapConfigDto ldapConfigDto = ldapService.queryLdapDetail();

        UserDto userDto;
        if (userName.equals("admin")) {
            userDto = userService.getUserDto(userName, true);
        } else if (isLdapOn(ldapConfigDto)) {
            userDto = authManager4Ldap.auth(userName, password, ldapConfigDto);
        } else {
            userDto = userService.getUserDto(userName, true);
        }

        //校验用户权限
        checkAuth(userDto);
        //校验密码
        if (!md5Password.equals(userDto.getPassword())) {
            throw new BusinessException(ErrorMessage.AUTH_FAILED);
        }
        JSONObject admin = convertUserInfo(userDto);
        long currentTime = System.currentTimeMillis();
        String token = JwtTokenComponent.generateToken("userInfo", admin,
            new Date(currentTime + (long)(expireTime * 3600000L)), new Date(currentTime - 300000L));
        response.setHeader(SET_TOKEN, token);
        JSONObject res = new JSONObject();
        res.put("userName", userName);
        res.put("token", token);
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
        String token = request.getHeader(USER_TOKEN);
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("token is null");
        }
        JSONObject json = JwtTokenComponent.getClaimsFromToken("userInfo", token);
        String userName = json.getString("username");
        response.setHeader(SET_TOKEN, "0");
        return userName;
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
    public void checkAuth(UserDto userDto){
        if (userDto.getRoleId() == null){
            throw new BusinessException(ErrorMessage.USER_ROLE_NOT_EXIT);
        }
        List<ResourceMenuDto> menuDtoList = roleService.listMenuByRoleId(String.valueOf(userDto.getRoleId()));
        if (CollectionUtils.isEmpty(menuDtoList)){
            throw new BusinessException(ErrorMessage.ROLE_PERMISSION_IS_EMPTY);
        }
    }

    private boolean isLdapOn(LdapConfigDto ldapConfigDto) {
        if (ldapConfigDto != null && ldapConfigDto.getIsOn() != null && ldapConfigDto.getIsOn() == 1) {
            return true;
        }
        return false;
    }
}
