package com.harmonycloud.zeus.interceptor;

import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.interceptor.Interceptor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * @author xutianhong
 * @Date 2022/10/12 3:37 下午
 */
public class MiddlewareApiInterceptor implements Interceptor {

    @Override
    public boolean beforeExecute(ForestRequest request) {
        // setToken
        HttpServletRequest servletRequest =
            ((ServletRequestAttributes)Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                .getRequest();
        String mwToken = servletRequest.getHeader("mwToken");
        request.addHeader("Authorization", mwToken);
        return true;
    }

    @Override
    public void onSuccess(Object data, ForestRequest request, ForestResponse response) {
        String mwToken = response.getHeaderValue("token");
        HttpServletResponse servletResponse =
            ((ServletRequestAttributes)Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                .getResponse();
        if (servletResponse != null) {
            servletResponse.addHeader("mwToken", mwToken);
        }
    }
}
