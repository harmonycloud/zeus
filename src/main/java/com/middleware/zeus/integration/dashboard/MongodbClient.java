package com.middleware.zeus.integration.dashboard;

import com.dtflys.forest.annotation.*;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbOrgDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbProjectDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbUserDo;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.interceptor.MiddlewareApiInterceptor;

/**
 * @author xutianhong
 * @Date 2024/12/18 1:41 PM
 */
@Component
@Address(source = MiddlewareApiAddress.class)
@BaseRequest(interceptor = MiddlewareApiInterceptor.class)
public interface MongodbClient {

    /**
     * 获取组织id
     */
    @Get(url = "/mongodb/ops/orgs?protocol={protocol}&path={path}&port={port}&publicKey={publicKey}&privateKey={privateKey}")
    JSONObject getOrgId(@Var("protocol") String protocol,
                        @Var("path") String path,
                        @Var("port") String port,
                        @Var("publicKey") String publicKey,
                        @Var("privateKey") String privateKey);

    /**
     * 创建用户
     */
    @Post(url = "/mongodb/ops/user")
    JSONObject createUser(@Var("clusterId") String clusterId,
                          @JSONBody MongodbUserDo mongodbUserDo);

    /**
     * 删除用户
     */
    @Delete(url = "/mongodb/ops/user?protocol={protocol}&path={path}&port={port}&publicKey={publicKey}&privateKey={privateKey}&id={id}")
    JSONObject deleteUser(@Var("clusterId") String clusterId,
                          @Var("protocol") String protocol,
                          @Var("path") String path,
                          @Var("port") String port,
                          @Var("publicKey") String publicKey,
                          @Var("privateKey") String privateKey,
                          @Var("id") String id);

    /**
     * 创建组织
     */
    @Post(url = "/mongodb/ops/org")
    JSONObject createOrg(@Var("clusterId") String clusterId,
                         @JSONBody MongodbOrgDo mongodbOrgDo);

    /**
     * 更新组织
     */
    @Patch(url = "/mongodb/ops/org")
    JSONObject updateOrg(@Var("clusterId") String clusterId,
                         @JSONBody MongodbOrgDo mongodbOrgDo);

    /**
     * 删除组织
     */
    @Delete(url = "/mongodb/ops/org?protocol={protocol}&path={path}&port={port}&publicKey={publicKey}&privateKey={privateKey}&id={id}")
    JSONObject deleteOrg(@Var("clusterId") String clusterId,
                         @Var("protocol") String protocol,
                         @Var("path") String path,
                         @Var("port") String port,
                         @Var("publicKey") String publicKey,
                         @Var("privateKey") String privateKey,
                         @Var("id") String id);

    /**
     * 查询组织列表
     */
    @Get(url = "/mongodb/ops/orgs?protocol={protocol}&path={path}&port={port}&publicKey={publicKey}&privateKey={privateKey}")
    JSONObject listOrgans(@Var("clusterId") String clusterId,
                          @Var("protocol") String protocol,
                          @Var("path") String path,
                          @Var("port") String port,
                          @Var("publicKey") String publicKey,
                          @Var("privateKey") String privateKey);

    /**
     * 将用户分配给项目
     */
    @Post(url = "/mongodb/ops/org/user}")
    JSONObject allocateUserToOrgan(@Var("clusterId") String clusterId,
                                   @JSONBody MongodbUserDo mongodbUserDo);

    /**
     * 将用户分配给项目
     */
    @Post(url = "/mongodb/ops/org/user}")
    JSONObject removeOrganUser(@Var("clusterId") String clusterId,
                               @JSONBody String orgId,
                               @JSONBody MongodbUserDo mongodbUserDo);

    /**
     * 创建项目
     */
    @Post(url = "/mongodb/ops/project")
    JSONObject createProject(@Var("clusterId") String clusterId,
                             @JSONBody MongodbProjectDo mongodbProjectDo);

    /**
     * 更新项目
     */
    @Patch(url = "/mongodb/ops/project")
    JSONObject updateProject(@JSONBody MongodbProjectDo mongodbProjectDo);

    /**
     * 删除项目
     */
    @Delete(url = "/mongodb/ops/project?protocol={protocol}&path={path}&port={port}&publicKey={publicKey}&privateKey={privateKey}&id={id}")
    JSONObject deleteProject(@Var("clusterId") String clusterId,
                             @Var("protocol") String protocol,
                             @Var("path") String path,
                             @Var("port") String port,
                             @Var("publicKey") String publicKey,
                             @Var("privateKey") String privateKey,
                             @Var("id") String id);

    /**
     * 查询所有项目
     */
    @Get(url = "/mongodb/ops/projects?protocol={protocol}&path={path}&port={port}&publicKey={publicKey}&privateKey={privateKey}")
    JSONObject listAllProjects(@Var("clusterId") String clusterId,
                               @Var("protocol") String protocol,
                               @Var("path") String path,
                               @Var("port") String port,
                               @Var("publicKey") String publicKey,
                               @Var("privateKey") String privateKey);

    /**
     * 将用户分配给项目
     */
    @Post(url = "/mongodb/ops/project/user}")
    JSONObject allocateUserToProject(@Var("clusterId") String clusterId,
                                     @JSONBody MongodbUserDo mongodbUserDo);

    /**
     * 获取组织下所有用户
     */
    @Get(url = "/mongodb/ops/orgs/{orgId}/groups/{projectId}/users}")
    JSONObject listOrganUser(@Var("clusterId") String clusterId,
                             @Var("orgId") String orgId,
                             @Var("projectId") String projectId,
                             @Var("publicKey") String publicKey,
                             @Var("privateKey") String privateKey);

    /**
     * 获取项目下所有用户
     */
    @Get(url = "/mongodb/ops/orgs/{orgId}/groups/{projectId}/users}")
    JSONObject listProjectUser(@Var("clusterId") String clusterId,
                               @Var("orgId") String orgId,
                               @Var("projectId") String projectId,
                               @Var("publicKey") String publicKey,
                               @Var("privateKey") String privateKey);



}
