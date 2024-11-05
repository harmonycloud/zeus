package com.middleware.zeus.common.exception;

import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import lombok.Getter;

/**
 * @author chwetion
 * @date 2020/11/24
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {
    private int code;
    private String detail;

    public BusinessException(ErrorMessage errorMessage, String errorDetail) {
        super(errorMessage.getMsg());
        this.code = errorMessage.getCode();
        this.detail = errorDetail;
    }

    public BusinessException(ErrorMessage errorMessage) {
        super(errorMessage.getMsg());
        this.code = errorMessage.getCode();
    }
    
    public BusinessException(DictEnum dictEnum, ErrorMessage errorMessage) {
        super(dictEnum.phrase() + errorMessage.getMsg());
        this.code = errorMessage.getCode();
    }

    public BusinessException(DictEnum dictEnum, String name, ErrorMessage errorMessage) {
        super(dictEnum.phrase() + name + " " + errorMessage.getMsg());
        this.code = errorMessage.getCode();
    }

    public BusinessException(DictEnum dictEnum, ErrorMessage errorMessage, String errorDetail) {
        super(dictEnum.phrase() + errorMessage.getMsg());
        this.code = errorMessage.getCode();
        this.detail = errorDetail;
    }

    public BusinessException(DictEnum dictEnum, String name, ErrorMessage errorMessage, String errorDetail) {
        super(dictEnum.phrase() + name + errorMessage.getMsg());
        this.code = errorMessage.getCode();
        this.detail = errorDetail;
    }

    /**
     * 是否已存在
     */
    public boolean isAlreadyExist() {
        return detail.startsWith("response code: 409");
    }

    /**
     * 是否不存在
     */
    public boolean isNotExist() {
        return detail.startsWith("response code: 404");
    }

    /**
     * 是否未授权
     */
    public boolean isUnauthorized() {
        return detail.startsWith("response code: 401");
    }

    public int getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }
}
