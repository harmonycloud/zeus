package com.harmonycloud.zeus.util;

import com.harmonycloud.caas.common.base.CaasResult;
import com.harmonycloud.caas.common.enums.CaasErrorMessage;

/**
 * @author liyinlong
 * @since 2022/6/9 2:37 下午
 */
public class CaasResponseUtil {

    public static boolean fitError(CaasResult baseResult, CaasErrorMessage caasErrorMessage) {
        if (Boolean.FALSE.equals(baseResult.getSuccess()) && caasErrorMessage.getCode() == baseResult.getCode()) {
            return true;
        }
        return false;
    }

}
