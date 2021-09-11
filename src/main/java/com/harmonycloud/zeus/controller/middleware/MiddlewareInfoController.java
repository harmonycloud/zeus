package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harmonycloud.caas.common.base.BaseResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = "middlewareInfo", value = "中间件信息", description = "中间件信息")
@RestController
@RequestMapping("/middlewares/info")
public class MiddlewareInfoController {

    @Autowired
    private MiddlewareInfoService middlewareInfoService;

    @ApiOperation(value = "查询可用的中间件列表", notes = "查询可用的中间件列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult list(@RequestParam(value = "clusterId") String clusterId) {
        return BaseResult.ok(middlewareInfoService.list(clusterId));
    }

    @ApiOperation(value = "查询指定中间件版本", notes = "查询指定中间件版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/version")
    public BaseResult version(@RequestParam(value = "clusterId") String clusterId,
                              @RequestParam(value = "type") String type) {
        return BaseResult.ok(middlewareInfoService.version(clusterId, type));
    }

    @ApiOperation(value = "查询中间件服务列表", notes = "查询中间件服务列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "命名空间", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("listAllMiddleware")
    public BaseResult listAllMiddleware(@RequestParam(value = "clusterId") String clusterId,
                                        @RequestParam(value = "namespace", required = false) String namespace,
                                        @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(middlewareInfoService.listAllMiddleware(clusterId, namespace, keyword));
    }

}
