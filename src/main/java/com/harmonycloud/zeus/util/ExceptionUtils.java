package com.harmonycloud.zeus.util;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.tool.api.common.ApiException;

/**
 * @author chwetion
 * @since 2020/12/7 11:48 上午
 */
public class ExceptionUtils {

    public static BusinessException convertRegistryApiExp2BuzExp(ApiException e) {
        if (e.getCode() > 500) {
            return new BusinessException(ErrorMessage.REGISTRY_CONNECTION_FAILED,
                    "response code: " + e.getCode() + ", response msg: " + e.getMessage() + " : " + e.getResponseMessage());
        }
        if (e.getCode() == 403) {
            return new BusinessException(ErrorMessage.PROJECT_HAS_NO_PERMISSION_IN_REPO,
                    "response code: " + e.getCode() + ", response msg: " + e.getMessage() + " : " + e.getResponseMessage());
        }
        return new BusinessException(ErrorMessage.REGISTRY_CALL_FAILED,
                "response code: " + e.getCode() + ", response msg: " + e.getMessage() + " : " + e.getResponseMessage());
    }

}
