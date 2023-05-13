package com.middleware.zeus.common.base;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.exception.CaasRuntimeException;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

@Accessors(chain = true)
@Data
@ApiModel(description = "返回基类")
public class BaseResult<T> {
    private static final long serialVersionUID = 1L;
    public static final Integer SUCCESS_CODE = 0;
    @ApiModelProperty(value = "返回编码")
    private Integer code;
    @ApiModelProperty(value = "错误信息")
    private String errorMsg;
    @ApiModelProperty(value = "错误详情")
    private String errorDetail;
    @ApiModelProperty(value = "是否成功")
    private Boolean success;
    @ApiModelProperty(value = "成功数据")
    private T data;
    @ApiModelProperty(value = "分页总页数")
    private Integer count;
    @ApiModelProperty(value = "错误堆栈信息", notes = "仅在调试模式下生效")
    private StackTraceElement[] errorStack;

    /**
     * 默认构造方法
     */
    public BaseResult() {
    }

    public BaseResult(Integer code, String errorMsg, Boolean success) {
        this.code = code;
        this.errorMsg = errorMsg;
        this.success = success;
    }

    public BaseResult(Integer code, Boolean success) {
        this.code = code;
        this.success = success;
    }

    public BaseResult(Integer code, Boolean success, T data) {
        this.code = code;
        this.success = success;
        this.data = data;
    }

    public BaseResult(Integer code, Boolean success, T data, Integer count) {
        this.code = code;
        this.success = success;
        this.data = data;
        this.count = count;
    }

    public BaseResult(Integer code, String errorMsg, String errorDetail, Boolean success) {
        this.code = code;
        this.errorMsg = errorMsg;
        this.errorDetail = errorDetail;
        this.success = success;
    }

    public static BaseResult ok() {
        return new BaseResult(SUCCESS_CODE, true);
    }

    public static BaseResult ok(Object data) {
        return new BaseResult(SUCCESS_CODE, true, data);
    }

    public static BaseResult ok(Object data, int count) {
        return new BaseResult(SUCCESS_CODE, true, data, count);
    }

    public static BaseResult error() {
        return error(ErrorMessage.UNKNOWN);
    }

    public static BaseResult error(ErrorMessage errorMessage) {
        return new BaseResult(errorMessage.getCode(), errorMessage.getMsg(), false);
    }

    public static BaseResult error(ErrorMessage errorMessage, String msg) {
        return new BaseResult(errorMessage.getCode(), errorMessage.getMsg() + (StringUtils.isBlank(msg) ? "" : ": " + msg), false);
    }

    public static BaseResult error(String detail, ErrorMessage errorMessage) {
        return new BaseResult(errorMessage.getCode(), errorMessage.getMsg(), detail, false);
    }

    public static BaseResult error(String detail, ErrorMessage errorMessage, String msg) {
        return new BaseResult(errorMessage.getCode(), errorMessage.getMsg() + (StringUtils.isBlank(msg) ? "" : ": " + msg), detail, false);
    }

    public static BaseResult error(CaasRuntimeException e) {
        return new BaseResult(e.getCode(), e.getErrorMsg(), e.getErrorDetail(), false);
    }

    public static BaseResult error(BusinessException e) {
        return new BaseResult(e.getCode(), e.getMessage(), e.getDetail(), false);
    }

    @Override
    public String toString() {
        return "BaseResult{" +
                "code=" + code +
                ", errorMsg='" + errorMsg + '\'' +
                ", errorDetail='" + errorDetail + '\'' +
                ", success=" + success +
                ", data=" + data +
                ", count=" + count +
                '}';
    }
}
