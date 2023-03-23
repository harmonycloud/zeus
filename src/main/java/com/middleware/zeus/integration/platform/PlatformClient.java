package com.middleware.zeus.integration.platform;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.Address;
import com.dtflys.forest.annotation.Get;
import com.dtflys.forest.annotation.Header;
import com.dtflys.forest.annotation.Post;
import org.springframework.stereotype.Component;

/**
 * @auther wangpenglei
 * @date 2023/3/22 16:04
 */
@Component
@Address(source = PlatformAddress.class)
public interface PlatformClient {

    /**
     * 平台灾备切换
     * 
     * @return
     */
    @Post(url = "/platform/disasterRecovery/switch")
    JSONObject switchPlatform(@Header("userToken") String userToken);

    /**
     * 获取同步器状态
     * @param userToken
     * @return
     */
    @Get(url = "/platform/disasterRecovery/replicate")
    JSONObject getMysqlReplicateStatus(@Header("userToken") String userToken);
}
