package com.harmonycloud.zeus.controller.middleware;

import java.util.List;

import com.harmonycloud.zeus.annotation.Authority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.middleware.CustomConfig;
import com.middleware.caas.common.model.middleware.CustomConfigHistoryDTO;
import com.middleware.caas.common.model.middleware.Middleware;
import com.middleware.caas.common.model.middleware.MiddlewareCustomConfig;
import com.harmonycloud.zeus.service.middleware.MiddlewareCustomConfigService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @Date 2021/4/23 4:28 下午
 */
@Api(tags = {"服务列表","服务配置"}, value = "中间件自定义参数", description = "中间件自定义参数")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/config")
public class MiddlewareCustomConfigController {

    @Autowired
    private MiddlewareCustomConfigService middlewareCustomConfigService;

    @ApiOperation(value = "获取自定义配置", notes = "获取自定义配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "order", value = "排序", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    @Authority(power = 1)
    public BaseResult<List<CustomConfig>> list(@PathVariable("clusterId") String clusterId,
                                               @PathVariable("namespace") String namespace,
                                               @PathVariable("middlewareName") String middlewareName,
                                               @RequestParam("type") String type,
                                               @RequestParam(value = "order", required = false) String order) throws Exception {
        return BaseResult.ok(middlewareCustomConfigService.listCustomConfig(clusterId, namespace, middlewareName, type, order));
    }

    @ApiOperation(value = "更新自定义配置", notes = "更新自定义配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareCustomConfig", value = "自定义配置", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping
    @Authority(power = 1)
    public BaseResult<Middleware> put(@PathVariable("clusterId") String clusterId,
                                      @PathVariable("namespace") String namespace,
                                      @PathVariable("middlewareName") String middlewareName,
                                      @RequestBody MiddlewareCustomConfig middlewareCustomConfig) {
        middlewareCustomConfig.setClusterId(clusterId);
        middlewareCustomConfig.setNamespace(namespace);
        middlewareCustomConfig.setName(middlewareName);
        middlewareCustomConfigService.updateCustomConfig(middlewareCustomConfig);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取自定义配置修改记录", notes = "获取自定义配置修改记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "item", value = "配置名称",required = false, paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startTime", value = "开始时间", required = false, paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endTime", value = "结束时间", required = false, paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/history")
    @Authority(power = 1)
    public BaseResult<List<CustomConfigHistoryDTO>> getHistory(@PathVariable("clusterId") String clusterId,
                                                               @PathVariable("namespace") String namespace,
                                                               @PathVariable("middlewareName") String middlewareName,
                                                               @RequestParam("type") String type,
                                                               @RequestParam(value = "item", required = false) String item,
                                                               @RequestParam(value = "startTime", required = false) String startTime,
                                                               @RequestParam(value = "endTime", required = false) String endTime) {
        return BaseResult.ok(middlewareCustomConfigService.getCustomConfigHistory(clusterId, namespace,
                middlewareName, type, item, startTime, endTime));
    }

    @ApiOperation(value = "置顶指定参数", notes = "置顶指定参数")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "configName", value = "自定义参数名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/{configName}/top")
    @Authority(power = 1)
    public BaseResult topping(@PathVariable("clusterId") String clusterId,
                              @PathVariable("namespace") String namespace,
                              @PathVariable("middlewareName") String middlewareName,
                              @PathVariable("configName") String configName,
                              @RequestParam("type") String type) {
        middlewareCustomConfigService.topping(clusterId, namespace, middlewareName, configName, type);
        return BaseResult.ok();
    }

}