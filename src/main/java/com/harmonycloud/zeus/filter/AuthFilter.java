package com.harmonycloud.zeus.filter;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.filters.base.BaseResult;
import com.harmonycloud.caas.filters.exception.AuthRuntimeException;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.zeus.config.InitMiddlewareImage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author xutianhong
 * @Date 2021/9/13 3:45 下午
 */
@Slf4j
public class AuthFilter implements Filter {

    private static final String auth = "/auth";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        log.debug("auth filter is in calling");
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        String path = httpRequest.getRequestURI();
        if (!acceptPath(path)
            && StringUtils.isEmpty(httpRequest.getHeader("userToken"))
            && StringUtils.isEmpty(httpRequest.getHeader("Sec-WebSocket-Protocol"))) {
            httpResponse.setContentType("application/json; charset=UTF-8");
            PrintWriter out = httpResponse.getWriter();
            out.append(
                JSONObject.toJSONString(BaseResult.exception(HttpStatus.UNAUTHORIZED.value(), "auth failed", "用户未登录")));
            out.close();
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    public Boolean acceptPath(String path) {
        log.info(path);
        if (path.contains("/images")) {
            new InitMiddlewareImage().initIfNotExists(path);
            return true;
        }
        return path.contains(auth) || path.contains("/swagger") || path.contains("/v3")
                || path.contains("/swagger-ui") || path.contains("/images") || path.contains("/webhook")
                || path.contains("/export") || path.contains("/slowsql/file") || path.contains("/getPersonalConfig");
    }
}
