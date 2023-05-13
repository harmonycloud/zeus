package com.middleware.zeus.common.base;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
public class CaasResult<T> {
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
    public CaasResult() {
    }

    public CaasResult(Integer code, String errorMsg, Boolean success) {
        this.code = code;
        this.errorMsg = errorMsg;
        this.success = success;
    }

    public CaasResult(Integer code, Boolean success) {
        this.code = code;
        this.success = success;
    }

    public CaasResult(Integer code, Boolean success, T data) {
        this.code = code;
        this.success = success;
        this.data = data;
    }

    public CaasResult(Integer code, Boolean success, T data, Integer count) {
        this.code = code;
        this.success = success;
        this.data = data;
        this.count = count;
    }

    public CaasResult(Integer code, String errorMsg, String errorDetail, Boolean success) {
        this.code = code;
        this.errorMsg = errorMsg;
        this.errorDetail = errorDetail;
        this.success = success;
    }

    public static CaasResult ok() {
        return new CaasResult(SUCCESS_CODE, true);
    }

    public static CaasResult ok(JSONObject data) {
        return new CaasResult(SUCCESS_CODE, true, data);
    }

    public static CaasResult ok(JSONObject data, int count) {
        return new CaasResult(SUCCESS_CODE, true, data, count);
    }

    public static CaasResult error() {
        return error(ErrorMessage.UNKNOWN);
    }

    public static CaasResult error(ErrorMessage errorMessage) {
        return new CaasResult(errorMessage.getCode(), errorMessage.getMsg(), false);
    }

    public static CaasResult error(ErrorMessage errorMessage, String msg) {
        return new CaasResult(errorMessage.getCode(), errorMessage.getMsg() + (StringUtils.isBlank(msg) ? "" : ": " + msg), false);
    }

    public static CaasResult error(String detail, ErrorMessage errorMessage) {
        return new CaasResult(errorMessage.getCode(), errorMessage.getMsg(), detail, false);
    }

    public static CaasResult error(String detail, ErrorMessage errorMessage, String msg) {
        return new CaasResult(errorMessage.getCode(), errorMessage.getMsg() + (StringUtils.isBlank(msg) ? "" : ": " + msg), detail, false);
    }

    public static CaasResult error(CaasRuntimeException e) {
        return new CaasResult(e.getCode(), e.getErrorMsg(), e.getErrorDetail(), false);
    }

    public static CaasResult error(BusinessException e) {
        return new CaasResult(e.getCode(), e.getMessage(), e.getDetail(), false);
    }

    public String getStringVal(String key) {
        if (!isJSONObject()) {
            return null;
        }
        JSONObject jsonData = (JSONObject) data;
        return jsonData.getString(key);
    }

    public String getJSONArrayStringVal(int index, String key) {
        if (isJSONObject()) {
            return getStringVal(key);
        }
        JSONArray jsonArray = (JSONArray) this.data;
        JSONObject object = (JSONObject) jsonArray.get(index);
        return object.getString(key);
    }

    public Integer getJSONArrayIntegerVal(int index, String key) {
        if (isJSONObject()) {
            return getIntegerVal(key);
        }
        JSONArray jsonArray = (JSONArray) this.data;
        JSONObject object = (JSONObject) jsonArray.get(index);
        return object.getInteger(key);
    }

    public Integer getIntegerVal(String key) {
        if (!isJSONObject()) {
            return null;
        }
        JSONObject jsonData = (JSONObject) data;
        return jsonData.getInteger(key);
    }

    public Boolean getBooleanVal(String key) {
        if (!isJSONObject()) {
            return null;
        }
        JSONObject jsonData = (JSONObject) data;
        return jsonData.getBoolean(key);
    }

    public JSONObject getJSONObject(String key) {
        if (!isJSONObject()) {
            return null;
        }
        if (!isJSONObject()) {
            return null;
        }
        JSONObject jsonData = (JSONObject) data;
        return jsonData.getJSONObject(key);
    }

    public JSONArray getJSONArray(String key) {
        if (!isJSONObject()) {
            return null;
        }
        JSONObject jsonData = (JSONObject) data;
        return jsonData.getJSONArray(key);
    }

    public boolean isJSONObject() {
        return this.data instanceof JSONObject;
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
