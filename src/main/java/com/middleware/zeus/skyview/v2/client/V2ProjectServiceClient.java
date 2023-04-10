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
 * @Date 2023/3/24 4:13 下午
 */
@Component
@Address(source = SkyviewAddressSource.class)
@BaseRequest(interceptor = SkyviewInterceptor.class)
@Success(condition = ForestUnauthorizedSuccessCondition.class)
public interface V2ProjectServiceClient {

    @Get(url = "#{system.skyview.prefix}/caas/tenants/{organId}/projects")
    CaasResult<JSONArray> list(@Var("organId") String organId,
                               @Query("withFed") Boolean withFed,
                               @Query("includeQuota") Boolean includeQuota,
                               @Query("includeIpStatistic") Boolean includeIpStatistic);

    @Get(url = "#{system.skyview.prefix}/caas/tenants/{organId}/switchTenant", headers = {"Authorization: ${token}"})
    CaasResult<JSONObject> switchTenants(@Var("token") String token,
                                         @Var("organId") String organId);

    @Get(url = "#{system.skyview.prefix}/user/switchProject", headers = {"Authorization: ${token}"})
    CaasResult<JSONArray> switchProject(@Var("token") String token,
                                         @Var("projectId") String projectId);

    @Get(url = "#{system.skyview.prefix}/caas/tenants/{organId}/projects/{projectId}/namespaces")
    CaasResult<JSONArray> nsList(@Var("organId") String organId,
                                 @Var("projectId") String projectId,
                                 @Query("withQuota") Boolean withQuota,
                                 @Query("withAllDataCenter") Boolean withAllDataCenter);

    @Get(url = "#{system.skyview.prefix}/caas/tenants/{organId}/projects/{projectId}")
    CaasResult<JSONObject> get(@Var("organId") String organId,
                               @Var("projectId") String projectId,
                               @Query("includeNsCount") Boolean includeNsCount,
                               @Query("includeQuota") Boolean includeQuota,
                               @Query("includeIpStatistic") Boolean includeIpStatistic);

}
