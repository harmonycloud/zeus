package com.middleware.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.service.middleware.MiddlewareConfigYamlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/12/23 2:21 下午
 */
@Api(tags = {"服务列表","服务配置"}, value = "中间件配置文件yaml", description = "中间件配置文件yaml")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/configmap")
public class MiddlewareConfigYamlController {

    @Autowired
    private MiddlewareConfigYamlService middlewareConfigYamlService;

    @ApiOperation(value = "获取configmap名称列表", notes = "获取configmap名称列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartVersion", value = "中间件版本", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    @Authority(power = 1)
    public BaseResult<List<String>> nameList(@PathVariable("clusterId") String clusterId,
                                             @PathVariable("namespace") String namespace,
                                             @RequestParam("middlewareName") String middlewareName,
                                             @RequestParam("type") String type,
                                             @RequestParam("chartVersion") String chartVersion) {
        return BaseResult.ok(middlewareConfigYamlService.nameList(clusterId, namespace, middlewareName, type, chartVersion));
    }

    @ApiOperation(value = "获取configmap", notes = "获取configmap")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "configMapName", value = "配置文件名称", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{configMapName}")
    @Authority(power = 1)
    public BaseResult<String> get(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("configMapName") String configMapName) {
        return BaseResult.ok(middlewareConfigYamlService.yaml(clusterId, namespace, configMapName));
    }

    @ApiOperation(value = "更新configmap", notes = "更新configmap")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "configMapName", value = "配置文件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "config", value = "配置文件", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/{configMapName}")
    @Authority(power = 1)
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("configMapName") String configMapName,
                             @RequestParam("config") String config) {
        middlewareConfigYamlService.update(clusterId, namespace, configMapName, config);
        return BaseResult.ok();
    }

}
