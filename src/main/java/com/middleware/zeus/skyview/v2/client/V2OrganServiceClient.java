package com.middleware.zeus.skyview.v2.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.middleware.zeus.common.base.CaasResult;
import com.middleware.zeus.config.ForestUnauthorizedSuccessCondition;
import com.middleware.zeus.config.SkyviewAddressSource;
import com.middleware.zeus.interceptor.SkyviewInterceptor;
import org.springframework.stereotype.Component;

/**
 * @author xutianhong
 * @Date 2023/3/23 8:21 下午
 */
@Component
@Address(source = SkyviewAddressSource.class)
@BaseRequest(interceptor = SkyviewInterceptor.class)
@Success(condition = ForestUnauthorizedSuccessCondition.class)
public interface V2OrganServiceClient {

    @Get(url = "#{system.skyview.prefix}/caas/tenants", headers = {"Authorization: ${token}"})
    CaasResult<JSONArray> list(@Var("token") String token,
                               @Query("username") String username);

    @Get(url = "#{system.skyview.prefix}/caas/tenants/{organId}")
    CaasResult<JSONObject> get(@Var("organId") String organId,
                               @Query("includeMemberCount") Boolean includeMemberCount,
                               @Query("includeBackupCount") Boolean includeBackupCount);

    @Get(url = "#{system.skyview.prefix}/caas/tenants/{organId}/resourcequotas")
    CaasResult<JSONObject> quotas(@Var("organId") String organId,
                                  @Query("includeStorageClusterQuota") Boolean include);

    @Get(url = "#{system.skyview.prefix}/caas/tenants/{organId}/members")
    CaasResult<JSONArray> userList(@Var("organId") String organId);

    @Get(url = "#{system.skyview.prefix}/caas/tenants/{organId}/namespaces")
    CaasResult<JSONArray> nsList(@Var("organId") String organId,
                                 @Query("clusterId") String clusterId,
                                 @Query("withFed") Boolean withFed,
                                 @Query("withAllDataCenter") Boolean withAllDataCenter);
}
