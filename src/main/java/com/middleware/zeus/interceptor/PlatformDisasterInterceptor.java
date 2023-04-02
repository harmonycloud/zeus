package com.middleware.zeus.interceptor;

import cn.hutool.json.JSONObject;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.interceptor.Interceptor;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.zeus.util.ZeusCurrentUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;

import static com.middleware.caas.common.constants.NameConstant.AUTHORIZATION;
import static com.middleware.caas.common.constants.NameConstant.AUTH_TYPE;

/**
 * @author xutianhong
 * @Date 2023/4/2 12:51 下午
 */
public class PlatformDisasterInterceptor implements Interceptor<JSONObject> {

    @Override
    public boolean beforeExecute(ForestRequest request) {
        if (request.getHeader(AUTH_TYPE) == null){
            request.addHeader(AUTH_TYPE, 1);
        }
        return true;
    }
}
