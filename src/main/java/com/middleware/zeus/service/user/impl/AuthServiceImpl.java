package com.middleware.zeus.service.user.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.LdapConfigDto;
import com.middleware.zeus.common.model.user.SystemConfigDto;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.util.encrypt.PasswordUtils;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.LicenseService;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.service.user.AuthManager4Ldap;
import com.middleware.zeus.service.user.AuthService;
import com.middleware.zeus.service.user.LdapService;
import com.middleware.zeus.service.user.abstractService.AbstractAuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

import static com.middleware.caas.filters.base.GlobalKey.SET_TOKEN;
import static com.middleware.caas.filters.base.GlobalKey.USER_TOKEN;
import static com.middleware.zeus.common.constants.user.UserConstant.ADMIN;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Slf4j
@Service
@Skyview(target = "zeus")
public class AuthServiceImpl extends AbstractAuthService implements AuthService {


    @Value("${system.user.account.expire:180}")
    private Double accountExpireDay;
    @Value("${system.user.account.alert:15}")
    private Double accountExpireAlertDay;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private LicenseService licenseService;

    @Autowired
    private AuthManager4Ldap authManager4Ldap;
    @Autowired
    private LdapService ldapService;

    @Value("${system.user.superUserName:admin}")
    private String superUserName;

    @Override
    public JSONObject login(String userName, String password, HttpServletResponse response) throws Exception {
        //解密密码
        String decryptPassword = decrypt(password);
        //md5加密
        String md5Password = PasswordUtils.md5(decryptPassword);
        // 获取ldap配置信息
        LdapConfigDto ldapConfigDto = ldapService.queryLdapDetail();

        UserDto userDto;
        if (userName.equals(ADMIN)) {
            userDto = userService.getUserDto(userName, true);
        } else if (isLdapOn(ldapConfigDto)) {
            userDto = authManager4Ldap.auth(userName, decryptPassword, ldapConfigDto);
        } else {
            userDto = userService.getUserDto(userName, true);
        }

        //校验用户权限
        Boolean isAdmin = checkAuth(userDto);
        //校验密码
        if (!md5Password.equals(userDto.getPassword())) {
            throw new BusinessException(ErrorMessage.LOGIN_FAILED);
        }

        // 查看平台灾备切换状态
        checkDisasterRecovery(isAdmin);

        JSONObject userInfo = convertUserInfo(userDto);
        String token = generateToken(userInfo);
        response.setHeader(SET_TOKEN, token);
        JSONObject res = convertResult(userName, isAdmin, token);
        //校验密码日期
        if (!superUserName.equals(userDto.getUserName()) && userDto.getPasswordTime() != null) {
            double passwordUsedDay = DateUtils.getIntervalDays(new Date(), userDto.getPasswordTime()) / 3600d / 24d ;
            int passwordRemindCode = getPasswordRemindCode(passwordUsedDay);
            res.put("passwordRemindCode", passwordRemindCode);
            if (passwordRemindCode != 1) {
                res.put("passwordUsedDay", (int) Math.round(passwordUsedDay));
            }
        } else {
            res.put("passwordRemindCode", 1);
        }
        return res;
    }

    private void checkDisasterRecovery(Boolean isAdmin) {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        if (values == null){
            return;
        }
        Boolean switched = values.getJSONObject("args").getBoolean("disasterRecoverySwitched");
        boolean isMaster = "master-slave".equals(values.getString("type"));
        if (!isAdmin && !isMaster) {
            throw new BusinessException(ErrorMessage.DISASTER_ONLY_ADMIN_CAN_LOGIN);
        }
        // 主平台切换后无法登陆
        if (switched != null && switched && !isMaster) {
            log.error("当前集群已由平台灾备功能进行切换，无法进行登录");
            throw new BusinessException(ErrorMessage.SWITCH_TO_BACKUP_PLATFORM);
        }
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

    /**
     * 获取密码提醒code 1：正常，0：该修改密码了，2：密码已过期，必须修改密码了
     * @param passwordUsedDay
     */
    private Integer getPasswordRemindCode(double passwordUsedDay) {
        SystemConfigDto systemConfigDto = userService.getPasswordExpiredDay();
        double expiredDay = Long.parseLong(systemConfigDto.getConfigValue());
        double remindDay = expiredDay / 3;
        double leftDay = expiredDay - passwordUsedDay;
        if (leftDay <= 0) {
            log.info("密码已过期");
            return -1;
        }
        if (leftDay < remindDay) {
            log.info("该修改密码了");
            return 0;
        }
        return 1;
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
        return ldapConfigDto != null && ldapConfigDto.getEnable() != null && ldapConfigDto.getEnable();
    }

}
