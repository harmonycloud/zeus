package com.harmonycloud.zeus.skyviewservice;

import com.alibaba.fastjson.JSONArray;
import com.dtflys.forest.annotation.Address;
import com.dtflys.forest.annotation.Get;
import com.dtflys.forest.annotation.Var;
import com.harmonycloud.caas.common.base.CaasResult;
import com.harmonycloud.zeus.config.SkyviewAddressSource;

/**
 * @author liyinlong
 * @since 2022/6/21 2:43 下午
 */
@Address(source = SkyviewAddressSource.class)
public interface Skyview2NamespaceServiceClient {

    @Get(url = "#{system.skyview.prefix}/caas/dashboard/clusters/{clusterId}/namespaces", headers = {"Authorization: ${token}"})
    CaasResult<JSONArray> clusterNamespaces(@Var("token") String token, @Var("clusterId") String clusterId);

}
