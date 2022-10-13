package com.harmonycloud.zeus.controller.dashboard;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.zeus.service.middleware.MiddlewareDashboardAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletRequest;

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

    @ApiOperation(value = "获取database列表", notes = "获取database列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @PostMapping("/auth")
    public BaseResult login(@RequestParam("username") String username,
                            @RequestParam("password") String password,
                            @RequestParam("type") String type) {
        return BaseResult.ok(middlewareDashboardAuthService.login(username, password, type));
    }

}
