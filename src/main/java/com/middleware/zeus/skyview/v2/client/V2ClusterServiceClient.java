package com.middleware.zeus.skyview.v2.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.zeus.config.ForestUnauthorizedSuccessCondition;
import com.middleware.zeus.config.SkyviewAddressSource;
import com.middleware.zeus.interceptor.SkyviewInterceptor;
import org.springframework.stereotype.Component;

/**
 * @author xutianhong
 * @Date 2023/3/26 4:31 下午
 */
@Component
@Address(source = SkyviewAddressSource.class)
@BaseRequest(interceptor = SkyviewInterceptor.class)
@Success(condition = ForestUnauthorizedSuccessCondition.class)
public interface V2ClusterServiceClient {

    @Get(url = "#{system.skyview.prefix}/caas/clusters")
    CaasResult<JSONArray> list(@Query("includeDisable") Boolean includeDisable,
                               @Query("includePlatformCluster") Boolean includePlatformCluster,
                               @Query("includeAllocatedResource") Boolean includeAllocatedResource);

    @Get(url = "#{system.skyview.prefix}/caas/clusters/{clusterId}")
    CaasResult<JSONObject> get(@Var("clusterId") String clusterId);

}
