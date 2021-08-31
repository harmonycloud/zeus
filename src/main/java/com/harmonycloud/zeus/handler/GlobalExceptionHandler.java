package com.harmonycloud.zeus.handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.filters.exception.AuthRuntimeException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 2000;

    @ExceptionHandler(AuthRuntimeException.class)
    public BaseResult authRuntimeExceptionHandler(Throwable e) {
        return BaseResult.error(ErrorMessage.AUTH_FAILED, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResult illegalArgumentExceptionHandler(Throwable e) {
        return BaseResult.error(ErrorMessage.INVALID_PARAMETER, e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResult businessExceptionHandler(Throwable e) {
        BusinessException exception = (BusinessException) e;
        return BaseResult.error(exception);
    }

    @ExceptionHandler(CaasRuntimeException.class)
    public BaseResult caasExceptionHandler(Throwable e) {
        CaasRuntimeException exception = (CaasRuntimeException) e;
        // TODO 需要添加打印堆栈的调试模式配置
        if (exception.getCode() == ErrorMessage.UNKNOWN.getCode()) {
            return BaseResult.error(exception).setErrorStack(e.getStackTrace());
        }
        return BaseResult.error(exception);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResult exceptionHandler(Throwable e) {
        log.error(e.getMessage(), e);
        String errorMessage = e.getMessage();
        if (errorMessage != null && errorMessage.length() > ERROR_MESSAGE_MAX_LENGTH) {
            errorMessage = errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH) + "......";
        }
        return BaseResult.error(errorMessage, ErrorMessage.UNKNOWN);
    }
}
