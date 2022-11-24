package com.harmonycloud.zeus.controller.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.zeus.service.middleware.MiddlewareDashboardAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * @author xutianhong
 * @Date 2022/10/11 10:43 上午
 */
@Api(tags = {"服务列表", "服务管理"}, value = "中间件运维", description = "中间件运维")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}")
public class MiddlewareDashboardAuthController {

    @Autowired
    private MiddlewareDashboardAuthService middlewareDashboardAuthService;

    @ApiOperation(value = "中间件登录", notes = "中间件登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", value = "密码", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "类型", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("/auth")
    public BaseResult<JSONObject> login(@PathVariable("clusterId") String clusterId,
                                        @PathVariable("namespace") String namespace,
                                        @PathVariable("middlewareName") String middlewareName,
                                        @RequestParam("username") String username,
                                        @RequestParam("password") String password,
                                        @RequestParam("type") String type) {
        return BaseResult.ok(middlewareDashboardAuthService.login(clusterId, namespace, middlewareName, username, password, type));
    }

    @ApiOperation(value = "中间件登录", notes = "中间件登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "类型", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("/logout")
    public BaseResult<String> logout(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @PathVariable("middlewareName") String middlewareName,
                                     @RequestParam("type") String type) {
        middlewareDashboardAuthService.logout(clusterId, namespace, middlewareName, type);
        return BaseResult.ok();
    }

}
