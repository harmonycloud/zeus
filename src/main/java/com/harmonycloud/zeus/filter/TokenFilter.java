package com.harmonycloud.zeus.filter;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.filters.base.BaseResult;
import com.harmonycloud.caas.filters.exception.AuthRuntimeException;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.zeus.util.ApplicationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.harmonycloud.caas.filters.base.GlobalKey.USER_TOKEN;

/**
 * @author xutianhong
 * @Date 2021/9/28 10:23 上午
 */
@Slf4j
public class TokenFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        log.debug("token filter is in calling");
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        String authType = httpRequest.getHeader("authType");
        if (authType != null && !"0".equals(authType)) {
            boolean var14 = false;

            CurrentUser currentUser;
            label166:
            {
                try {
                    var14 = true;
                    this.dealToken(httpRequest.getHeader("userToken"), httpResponse);
                    chain.doFilter(request, response);
                    var14 = false;
                    break label166;
                } catch (AuthRuntimeException var16) {
                    httpResponse.setContentType("application/json; charset=UTF-8");
                    PrintWriter out = httpResponse.getWriter();
                    out.append(JSONObject.toJSONString(BaseResult.exception(
                        var16.getErrorCode() != null ? var16.getErrorCode() : HttpStatus.UNAUTHORIZED.value(),
                        var16.getErrorMessage(), var16.getExtMessage())));
                    out.close();
                    var14 = false;
                } finally {
                    if (var14) {
                        CurrentUser currentUser1 = CurrentUserRepository.getUserExistNull();
                        if (currentUser1 != null && currentUser1.getNewToken() != null) {
                            httpResponse.setHeader("setToken", currentUser1.getNewToken());
                        }

                        CurrentUserRepository.clear();
                    }
                }

                currentUser = CurrentUserRepository.getUserExistNull();
                if (currentUser != null && currentUser.getNewToken() != null) {
                    httpResponse.setHeader("setToken", currentUser.getNewToken());
                }

                CurrentUserRepository.clear();
                return;
            }

            currentUser = CurrentUserRepository.getUserExistNull();
            if (currentUser != null && currentUser.getNewToken() != null) {
                httpResponse.setHeader("setToken", currentUser.getNewToken());
            }

            CurrentUserRepository.clear();
        } else {
            String upgrade = httpRequest.getHeader("upgrade");
            if (!org.springframework.util.StringUtils.isEmpty(upgrade) && "websocket".equals(upgrade)) {
                String token = httpRequest.getHeader("Sec-WebSocket-Protocol");
                if (!StringUtils.isEmpty(token) && !"null".equalsIgnoreCase(token)) {
                    try {
                        this.dealToken(token, httpResponse);
                    } catch (AuthRuntimeException var15) {
                        httpResponse.setContentType("application/json; charset=UTF-8");
                        PrintWriter out = httpResponse.getWriter();
                        out.append(JSONObject.toJSONString(BaseResult.exception(
                            var15.getErrorCode() != null ? var15.getErrorCode() : HttpStatus.UNAUTHORIZED.value(),
                            var15.getErrorMessage(), var15.getExtMessage())));
                        out.close();
                        return;
                    }
                }
            }

            chain.doFilter(request, response);
        }

    }

    private void dealToken(String token, HttpServletResponse httpResponse) {
        JwtTokenComponent.JWTResultEnum resultEnum = JwtTokenComponent.checkToken(token);
        if (JwtTokenComponent.JWTResultEnum.SUCCESS.getCode() != resultEnum.getCode()) {
            throw new AuthRuntimeException(resultEnum.getMessage(), (Integer)null, resultEnum.getDetailMessage());
        } else {
            log.debug("Token 认证通过：{}", token);
            JSONObject userMap = resultEnum.getValue();
            Map<String,String> attributes = new HashMap<>();
            if (userMap.get("attributes") == null) {
                userMap.remove("attributes");
            } else {
                JSONObject caasJson = (JSONObject) userMap.get("attributes");
                attributes.put("caastoken", caasJson.getString("caastoken"));
                attributes.put("isAdmin", caasJson.getString("isAdmin"));
            }
            long currentTime = System.currentTimeMillis();
            httpResponse.setHeader(USER_TOKEN, JwtTokenComponent.generateToken("userInfo", userMap,
                new Date(currentTime + (long)(ApplicationUtil.getExpire() * 3600000L)), new Date(currentTime - 300000L)));
            CurrentUser currentUser = (new CurrentUser()).setUsername(userMap.getString("username"))
                .setNickname(userMap.getString("realName")).setToken(token).setAttributes(attributes);
            CurrentUserRepository.setUser(currentUser);
        }
    }

    @Override
    public void destroy() {}

}
