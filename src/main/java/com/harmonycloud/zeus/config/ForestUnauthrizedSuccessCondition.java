package com.harmonycloud.zeus.config;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.callback.SuccessWhen;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.harmonycloud.caas.common.base.CaasResult;
import com.harmonycloud.caas.common.enums.ErrorCodeMessage;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.tool.encrypt.RSAUtils;
import com.harmonycloud.zeus.skyviewservice.Skyview2UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @description 未登录成功时的拦截器
 * @author  liyinlong
 * @since 2022/6/23 1:52 下午
 */
@Slf4j
@Configuration
public class ForestUnauthrizedSuccessCondition implements SuccessWhen {

    /**
     * 请求成功条件
     * @param req Forest请求对象
     * @param res Forest响应对象
     * @return 是否成功，true: 请求成功，false: 请求失败
     */
    @Override
    public boolean successWhen(ForestRequest req, ForestResponse res) {
        // req 为Forest请求对象，即 ForestRequest 类实例
        // res 为Forest响应对象，即 ForestResponse 类实例
        // 返回值为 ture 则表示请求成功，false 表示请求失败
        log.info("请求失败:{}", res.getStatusCode());
        String content = res.getContent();
        if(content.contains(String.valueOf(ErrorCodeMessage.USER_NOT_AUTH.value()))){
            throw new BusinessException(ErrorMessage.USER_NOT_AUTH);
        }else if(content.contains(String.valueOf(ErrorCodeMessage.AUTH_FAIL.value()))){
            throw new BusinessException(ErrorMessage.AUTH_FAILED);
        }
        return res.noException();
    }

}