package com.harmonycloud.zeus.service.user;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

import static com.harmonycloud.caas.filters.base.GlobalKey.SET_TOKEN;
import static com.harmonycloud.caas.filters.base.GlobalKey.USER_TOKEN;

/**
 * @author liyinlong
 * @since 2022/6/9 3:18 下午
 */
public abstract class AbstractAuthService implements AuthService{

    @Value("${system.user.expire:0.5}")
    private Double expireTime;

    /**
     * 生成token
     * @param admin
     * @return
     */
    public String generateToken(JSONObject admin){
        long currentTime = System.currentTimeMillis();
        return JwtTokenComponent.generateToken("userInfo", admin,
                new Date(currentTime + (long)(expireTime * 3600000L)), new Date(currentTime - 300000L));
    }

    /**
     * 组装返回结果
     * @param userName
     * @param isAdmin
     * @param token
     * @return
     */
    public JSONObject convertResult(String userName,boolean isAdmin,String token){
        JSONObject res = new JSONObject();
        res.put("userName", userName);
        res.put("token", token);
        res.put("isAdmin", isAdmin);
        return res;
    }

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


}
