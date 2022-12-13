package com.middleware.zeus.config;

import com.dtflys.forest.callback.SuccessWhen;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.middleware.caas.common.enums.ErrorCodeMessage;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

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
        log.info("请求完成:{},{}", req.getUrl(), res.getStatusCode());
        String content = res.getContent();
        if(content.contains(String.valueOf(ErrorCodeMessage.USER_NOT_AUTH.value()))){
            throw new BusinessException(ErrorMessage.USER_NOT_AUTH);
        }else if(content.contains(String.valueOf(ErrorCodeMessage.AUTH_FAIL.value()))){
            throw new BusinessException(ErrorMessage.AUTH_FAILED);
        }else if(content.contains(String.valueOf(ErrorCodeMessage.USER_DISABLED.value()))){
            throw new BusinessException(ErrorMessage.USER_NOT_EXIT);
        }
        return res.noException();
    }

}