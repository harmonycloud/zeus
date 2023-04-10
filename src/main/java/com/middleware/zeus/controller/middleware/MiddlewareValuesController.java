package com.middleware.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.middleware.MiddlewareValues;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.service.middleware.MiddlewareValuesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author xutianhong
 * @Date 2021/12/16 4:13 下午
 */
@Api(tags = {"服务列表","服务管理"}, value = "中间件values.yaml", description = "中间件values.yaml")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/values")
public class MiddlewareValuesController {

    @Autowired
    private MiddlewareValuesService middlewareValuesService;

    @ApiOperation(value = "获取values.yaml", notes = "获取values.yaml")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    @Authority(power = 1)
    public BaseResult<String> get(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("middlewareName") String name) {
        return BaseResult.ok(middlewareValuesService.get(clusterId, namespace, name));
    }

    @ApiOperation(value = "更新values.yaml", notes = "更新values.yaml")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareValues", value = "中间件values", paramType = "query", dataTypeClass = MiddlewareValues.class),
    })
    @PutMapping
    @Authority(power = 1)
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String name,
                             @RequestBody MiddlewareValues middlewareValues) {
        middlewareValues.setClusterId(clusterId).setNamespace(namespace).setName(name);
        middlewareValuesService.update(middlewareValues);
        return BaseResult.ok();
    }

}
