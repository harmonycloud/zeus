package com.harmonycloud.zeus.interceptor;

import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.interceptor.Interceptor;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * @author xutianhong
 * @Date 2022/10/12 3:37 下午
 */
@Slf4j
public class MiddlewareApiInterceptor implements Interceptor {

    @Override
    public boolean beforeExecute(ForestRequest request) {
        // setToken
        HttpServletRequest servletRequest =
            ((ServletRequestAttributes)Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                .getRequest();
        String mwToken = servletRequest.getHeader("mwToken");
        if (StringUtils.isNotEmpty(mwToken)){
            request.addHeader("Authorization", mwToken);
        }
        return true;
    }

    @Override
    public void onSuccess(Object data, ForestRequest request, ForestResponse response) {
        String mwToken = response.getHeaderValue("token");
        log.info("打印接口执行时间: {}", response.getTimeAsMillisecond());
        HttpServletResponse servletResponse =
            ((ServletRequestAttributes)Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                .getResponse();
        if (servletResponse != null) {
            servletResponse.addHeader("mwToken", mwToken);
        }
    }

    @Override
    public void onError(ForestRuntimeException ex, ForestRequest req, ForestResponse res) {
        log.info(res.getContent());
        throw new BusinessException(ErrorMessage.MIDDLEWARE_API_REQUEST_ERROR, ex.getMessage());
    }

}
