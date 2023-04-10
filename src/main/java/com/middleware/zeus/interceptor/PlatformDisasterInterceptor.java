package com.middleware.zeus.interceptor;

import cn.hutool.json.JSONObject;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.interceptor.Interceptor;

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
