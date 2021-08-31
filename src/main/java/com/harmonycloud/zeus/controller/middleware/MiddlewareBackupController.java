package com.harmonycloud.zeus.controller.middleware;

import java.util.List;

import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackup;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = {"工作台","实例列表"}, value = "中间件备份", description = "中间件备份")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/backups")
public class MiddlewareBackupController {

    @Autowired
    private MiddlewareBackupService middlewareBackupService;

    @ApiOperation(value = "查询中间件列表", notes = "查询中间件列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<List<MiddlewareBackup>> list(@PathVariable("clusterId") String clusterId,
                                                   @PathVariable("namespace") String namespace,
                                                   @PathVariable("middlewareName") String middlewareName,
                                                   @RequestParam("type") String type) {
        return BaseResult.ok(middlewareBackupService.list(clusterId, namespace, type, middlewareName));
    }

    @ApiOperation(value = "创建中间件备份", notes = "创建中间件备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
    })
    @PostMapping
    public BaseResult create(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String middlewareName,
                             @RequestParam("type") String type) {
        middlewareBackupService.create(clusterId, namespace, type, middlewareName);
        return BaseResult.ok();
    }

}
