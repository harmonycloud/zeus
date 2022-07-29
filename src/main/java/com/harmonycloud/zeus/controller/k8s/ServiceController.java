package com.harmonycloud.zeus.controller.k8s;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.ServicePortDTO;
import com.harmonycloud.zeus.service.k8s.ServiceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tangtx
 * @date 4/02/21 6:00 PM
 */
@Api(tags = "service", value = "中间件服务")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares")
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @ApiOperation(value = "查询中间件对外访问列表", notes = "查询中间件对外访问列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareType", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{middlewareName}/services")
    public BaseResult<List<ServicePortDTO>> list(@PathVariable("clusterId") String clusterId,
                                                 @PathVariable(value = "namespace") String namespace,
                                                 @PathVariable(value = "middlewareName") String middlewareName,
                                                 @RequestParam(value = "middlewareType") String middlewareType) {
        return BaseResult.ok(serviceService.list(clusterId, namespace, middlewareName, middlewareType));
    }

    @ApiOperation(value = "查询中间件集群内访问列表", notes = "查询中间件集群内访问列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareType", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{middlewareName}/internalServices")
    public BaseResult<List<ServicePortDTO>> listInternalService(@PathVariable("clusterId") String clusterId,
                                                 @PathVariable(value = "namespace") String namespace,
                                                 @PathVariable(value = "middlewareName") String middlewareName,
                                                 @RequestParam(value = "middlewareType") String middlewareType) {
        return BaseResult.ok(serviceService.listInternalService(clusterId, namespace, middlewareName, middlewareType));
    }

}
