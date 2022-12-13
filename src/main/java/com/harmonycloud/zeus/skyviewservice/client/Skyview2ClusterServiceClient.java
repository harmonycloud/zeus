package com.harmonycloud.zeus.skyviewservice.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.Address;
import com.dtflys.forest.annotation.Get;
import com.dtflys.forest.annotation.Var;
import com.middleware.caas.common.base.CaasResult;
import com.harmonycloud.zeus.config.SkyviewAddressSource;

/**
 * @author liyinlong
 * @since 2022/6/15 10:26 上午
 */
@Address(source = SkyviewAddressSource.class)
public interface Skyview2ClusterServiceClient {

    /**
     * 获取所有集群信息
     * @param token
     * @return
     */
    @Get(url = "#{system.skyview.prefix}/caas/clusters?includeDisable=true&includePlatformCluster=true&includeAllocatedResource=true", headers = {"Authorization: ${token}"})
    CaasResult<JSONArray> clusters(@Var("token") String token);

    /**
     * 获取集群详细信息
     * @param token
     * @param clusterId
     * @return
     */
    @Get(url = "#{system.skyview.prefix}/caas/clusters/{clusterId}", headers = {"Authorization: ${token}"})
    CaasResult<JSONObject> clusterDetail(@Var("token")String token, @Var("clusterId") String clusterId);

}
