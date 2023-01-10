package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MiddlewarePvcDto;
import com.harmonycloud.zeus.annotation.Authority;
import com.harmonycloud.zeus.service.middleware.MiddlewarePvcService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/1/6 4:34 下午
 */
@Api(tags = {"服务列表","服务管理"}, value = "中间件存储管理", description = "中间件存储管理")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/storage")
public class MiddlewarePvcController {

    @Autowired
    private MiddlewarePvcService middlewarePvcService;

    @ApiOperation(value = "中间件pvc信息获取", notes = "中间件pvc信息获取")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @Authority
    @GetMapping
    public BaseResult<List<MiddlewarePvcDto>> list(@PathVariable("clusterId") String clusterId,
                                                   @RequestParam("namespace") String namespace,
                                                   @PathVariable("middlewareName") String middlewareName,
                                                   @RequestParam("type") String type) {
        return BaseResult.ok(middlewarePvcService.list(clusterId, namespace, middlewareName, type));
    }

    @ApiOperation(value = "中间件pvc事件获取", notes = "中间件pvc事件获取")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pvcName", value = "pvc名称", paramType = "path", dataTypeClass = String.class),
    })
    @Authority
    @GetMapping("/{pvcName}/event")
    public BaseResult<List<MiddlewarePvcDto>> getEvent(@PathVariable("clusterId") String clusterId,
                                                       @RequestParam("namespace") String namespace,
                                                       @PathVariable("middlewareName") String middlewareName,
                                                       @PathVariable("pvcName") String pvcName) {
        return BaseResult.ok(middlewarePvcService.list(clusterId, namespace, middlewareName, pvcName));
    }

    @ApiOperation(value = "中间件存储扩容", notes = "中间件存储扩容")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class)
    })
    @Authority(power = 1)
    @PutMapping("/{pvcName}/scale")
    public BaseResult switchDisasterRecovery(@PathVariable("clusterId") String clusterId,
                                             @RequestParam("namespace") String namespace,
                                             @PathVariable("middlewareName") String middlewareName,
                                             @PathVariable("pvcName") String pvcName,
                                             @RequestParam("targetStorage") Double targetStorage) {
        middlewarePvcService.scalePvc(clusterId, namespace, middlewareName, pvcName, targetStorage);
        return BaseResult.ok();
    }

}
