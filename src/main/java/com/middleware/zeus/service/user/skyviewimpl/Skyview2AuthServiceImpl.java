package com.middleware.zeus.service.user.skyviewimpl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.enums.CaasErrorMessage;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.tool.encrypt.RSAUtils;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.service.user.AuthService;
import com.middleware.zeus.service.user.abstractService.AbstractAuthService;
import com.middleware.zeus.skyview.Skyview2UserService;
import com.middleware.zeus.skyview.client.Skyview2UserServiceClient;
import com.middleware.zeus.skyview.v2.service.V2AuthService;
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
@Skyview(target = "skyview2")
public class Skyview2AuthServiceImpl extends AbstractAuthService implements AuthService {

    @Autowired
    private V2AuthService v2AuthService;

    @Value("${system.skyview.encryptPassword:false}")
    private boolean encryptPassword;

    @Override
    public JSONObject login(String userName, String password, HttpServletResponse response) throws Exception {

        // decrypt password
        password = decrypt(password);
        // login
        JSONObject data = v2AuthService.login(userName, password);
        // 获取token
        String caasToken = data.getString("token");
        // 获取用户详情
        UserDto userDto = userService.getUserDto(userName, true);
        // convert info
        JSONObject userInfo = convertUserInfo(userDto);
        userInfo.put("caasToken", caasToken);

        String token = generateToken(userInfo);
        response.setHeader(SET_TOKEN, token);
        return convertResult(userName, userDto.getIsAdmin(), token);
    }

    @Override
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

}
