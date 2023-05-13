package com.middleware.zeus.integration.platform;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.middleware.zeus.common.model.DisasterRecoveryInfo;
import com.middleware.zeus.interceptor.PlatformDisasterInterceptor;
import org.springframework.stereotype.Component;

/**
 * @auther wangpenglei
 * @date 2023/3/22 16:04
 */
@Component
@Address(source = PlatformAddress.class)
@BaseRequest(interceptor = PlatformDisasterInterceptor.class)
public interface PlatformClient {

    /**
     * 平台灾备切换
     * 
     * @return
     */
    @Post(url = "/api/platform/disasterRecovery/switch")
    JSONObject switchPlatform(@Header("userToken") String userToken,
                              @Body("isMaster") Boolean isMaster);

    /**
     * 获取同步器状态
     * @param userToken
     * @return
     */
    @Get(url = "/api/platform/disasterRecovery/replicate")
    JSONObject getMysqlReplicateStatus(@Header("userToken") String userToken);

    /**
     * 设置远程平台地址
     * @param disasterRecoveryInfo
     * @return
     */
    @Post(url = "/api/platform/disasterRecovery")
    JSONObject setAddress(@Header("userToken") String userToken,
                          @JSONBody DisasterRecoveryInfo disasterRecoveryInfo);

    /**
     * 查询远程数据库状态
     * @return
     */
    @Get(url = "/api/platform/disasterRecovery/mysql")
    JSONObject getRelationMysqlPhase(@Header("userToken") String userToken);
}
