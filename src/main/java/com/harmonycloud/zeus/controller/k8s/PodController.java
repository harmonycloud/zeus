package com.harmonycloud.zeus.controller.k8s;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.annotation.Authority;
import com.harmonycloud.zeus.service.k8s.PodService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = {"服务列表","服务实例"}, value = "pod", description = "pod")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/pods")
public class PodController {

    @Autowired
    private PodService podService;

    @ApiOperation(value = "查询pod列表", notes = "查询pod列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    @Authority(power = 1)
    public BaseResult<Middleware> list(@PathVariable("clusterId") String clusterId,
                                       @PathVariable("namespace") String namespace,
                                       @PathVariable("middlewareName") String middlewareName,
                                       @RequestParam("type") String type) {
        return BaseResult.ok(podService.list(clusterId, namespace, middlewareName, type));
    }

    @ApiOperation(value = "重启pod", notes = "重启pod")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "podName", value = "pod名称", paramType = "path", dataTypeClass = String.class),
    })
    @PostMapping("/{podName}/restart")
    @Authority(power = 1)
    public BaseResult restart(@PathVariable("clusterId") String clusterId,
                              @PathVariable("namespace") String namespace,
                              @PathVariable("middlewareName") String middlewareName,
                              @RequestParam("type") String type,
                              @PathVariable("podName") String podName) {
        podService.restart(clusterId, namespace, middlewareName, type, podName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查看pod yaml", notes = "查看pod yaml")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "podName", value = "pod名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{podName}/yaml")
    @Authority(power = 1)
    public BaseResult<String> yaml(@PathVariable("clusterId") String clusterId,
                                       @PathVariable("namespace") String namespace,
                                       @PathVariable("middlewareName") String middlewareName,
                                       @RequestParam("type") String type,
                                       @PathVariable("podName") String podName) {
        return BaseResult.ok(podService.yaml(clusterId, namespace, middlewareName, type, podName));
    }
}
