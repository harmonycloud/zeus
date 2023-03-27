package com.middleware.zeus.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.interceptor.Interceptor;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.exception.CaasRuntimeException;
import com.middleware.zeus.util.ZeusCurrentUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;

/**
 * @author xutianhong
 * @Date 2023/3/21 7:18 下午
 */
public class SkyviewInterceptor implements Interceptor<CaasResult<T>> {

    @Override
    public boolean beforeExecute(ForestRequest request) {
        // 获取当前用户的token  并传入request
        String token = ZeusCurrentUser.getCaasToken();
        if (StringUtils.isNotEmpty(token)){
            request.addHeader("Authorization", token);
        }
        return true;
    }

    @Override
    public void onSuccess(CaasResult<T> data, ForestRequest req, ForestResponse res) {
        // token刷新

    }

    @Override
    public void onError(ForestRuntimeException ex, ForestRequest req, ForestResponse res) {
        if (StringUtils.isNotEmpty(res.getContent())) {
            // 修改接口异常时的报错信息
            JSONObject jsonObject = JSONObject.parseObject(res.getContent());
            String err = jsonObject.getString("data");
            throw new BusinessException(ErrorMessage.ACCESS_EXTERNAL_SERVICE_FAILED, err);
        } else {
            throw new BusinessException(ErrorMessage.ACCESS_EXTERNAL_SERVICE_FAILED, ex.getMessage());
        }
    }

}
