package com.harmonycloud.zeus.skyviewservice;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.harmonycloud.caas.common.base.CaasResult;
import com.harmonycloud.zeus.config.ForestSuccessCondition;
import com.harmonycloud.zeus.config.SkyviewAddressSource;

@Address(source = SkyviewAddressSource.class)
public interface Skyview2ProjectServiceClient {

    /**
     * 获取租户所有项目
     * @param token
     * @param tenantId 租户id
     * @return
     */
    @Get(url = "#{system.skyview.prefix}/caas/tenants/{tenantId}/switchTenant", headers = {"Authorization: ${token}"})
    CaasResult<JSONObject> getProjectList(@Var("token") String token, @Var("tenantId") String tenantId);

    /**
     * 获取用户所在项目角色
     * @param token
     * @param projectId 项目id
     * @return
     */
    @Get(url = "#{system.skyview.prefix}/user/switchProject", headers = {"Authorization: ${token}"})
    CaasResult<JSONArray> getUserProjectRole(@Var("token") String token, @Query("projectId") String projectId);

    /**
     * 查询租户下所有项目及项目绑定的所有分区
     * @param token
     * @param tenantId
     * @return
     */
    @Get(url = "#{system.skyview.prefix}/caas/tenants/{tenantId}/projects", headers = {"Authorization: ${token}"})
    CaasResult<JSONArray> getTenantProject(@Var("token")String token,@Var("tenantId")String tenantId);

    /**
     * 获取项目成员
     * @param token
     * @param tenantId 租户id
     * @param projectId 项目id
     * @return
     */
    @Get(url = "#{system.skyview.prefix}/caas/tenants/{tenantId}/projects/{projectId}", headers = {"Authorization: ${token}"})
    CaasResult<JSONObject> getProjectMember(@Var("token")String token,@Var("tenantId")String tenantId,@Var("projectId")String projectId);

    /**
     * 查询项目所有分区(联邦)
     * @param token
     * @param tenantId
     * @param projectId
     * @return
     */
    @Get(url = "#{system.skyview.prefix}/caas/tenants/{tenantId}/federations/namespaces?withQuota=false", headers = {"Authorization: ${token}"})
    CaasResult<JSONArray> getFederationProjectNamespace(@Var("token")String token,@Var("tenantId")String tenantId,@Query("projectId")String projectId);

    /**
     * 查询项目所有分区
     * @param token
     * @param tenantId
     * @param projectId
     * @return
     */
    @Get(url = "#{system.skyview.prefix}/caas/tenants/{tenantId}/projects/{projectId}/namespaces", headers = {"Authorization: ${token}"})
    CaasResult<JSONArray> getProjectNamespace(@Var("token") String token, @Var("tenantId") String tenantId, @Query("projectId") String projectId);

}

