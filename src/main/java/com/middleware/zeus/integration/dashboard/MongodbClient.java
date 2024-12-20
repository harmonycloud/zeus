package com.middleware.zeus.integration.dashboard;

import com.dtflys.forest.annotation.*;
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
     * 删除项目
     */
    @Delete(url = "/mongodb/ops/project?protocol={protocol}&path={path}&port={port}&publicKey={publicKey}&privateKey={privateKey}&projectName={projectName}")
    JSONObject deleteProject(@Var("protocol") String protocol,
                             @Var("path") String path,
                             @Var("port") String port,
                             @Var("publicKey") String publicKey,
                             @Var("privateKey") String privateKey,
                             @Var("projectName") String projectName);

}
