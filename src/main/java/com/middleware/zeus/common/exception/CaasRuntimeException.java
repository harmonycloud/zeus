package com.middleware.zeus.common.exception;

import com.middleware.zeus.common.enums.ErrorMessage;
import lombok.Getter;

/**
 * @author dengyulong
 * @date 2020/11/24
 * 内部异常（非业务异常）
 */
@Getter
public class CaasRuntimeException extends RuntimeException {

    private Integer code;
    private String errorMsg;
    private String errorDetail;

    public CaasRuntimeException() {
    }

    public CaasRuntimeException(String message) {
        super(message);
        this.errorMsg = message;
    }

    public CaasRuntimeException(Integer errorCode) {
        this.code = errorCode;
    }

    public CaasRuntimeException(Throwable cause) {
        super(cause);
    }

    public CaasRuntimeException(String message, Throwable cause) {
        super(message, cause);
        this.errorMsg = message;
    }

    public CaasRuntimeException(ErrorMessage errorMessage) {
        this.code = errorMessage.getCode();
        this.errorMsg = errorMessage.getMsg();
    }

    public CaasRuntimeException(ErrorMessage errorMessage, String errorDetail) {
        this.code = errorMessage.getCode();
        this.errorMsg = errorMessage.getMsg();
        this.errorDetail = errorDetail;
    }

}
