package com.middleware.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.EventDetail;
import com.middleware.caas.common.model.middleware.MiddlewarePvcDto;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.service.middleware.MiddlewarePvcService;
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
                                                   @PathVariable("namespace") String namespace,
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
    public BaseResult<List<EventDetail>> getEvent(@PathVariable("clusterId") String clusterId,
                                                  @PathVariable("namespace") String namespace,
                                                  @PathVariable("middlewareName") String middlewareName,
                                                  @PathVariable("pvcName") String pvcName) {
        return BaseResult.ok(middlewarePvcService.getEvent(clusterId, namespace, middlewareName, pvcName));
    }

    @ApiOperation(value = "中间件存储扩容", notes = "中间件存储扩容")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pvcName", value = "pvc名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageClass", value = "存储类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storage", value = "当前存储大小", paramType = "query", dataTypeClass = Double.class),
            @ApiImplicitParam(name = "targetStorage", value = "目标存储大小", paramType = "query", dataTypeClass = Double.class),
    })
    @Authority(power = 1)
    @PutMapping("/{pvcName}/scale")
    public BaseResult scalePvc(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("middlewareName") String middlewareName,
                               @PathVariable("pvcName") String pvcName,
                               @RequestParam("type") String type,
                               @RequestParam("storageClass") String storageClass,
                               @RequestParam("storage") Double storage,
                               @RequestParam("targetStorage") Double targetStorage) {
        middlewarePvcService.scalePvc(clusterId, namespace, middlewareName, pvcName, type, storageClass, storage, targetStorage);
        // 避免因k8s资源状态加载过慢导致的后续接口信息获取错误，此处沉睡1s
        try {
            Thread.sleep(1000);
        } catch (Exception ignored){
        }
        return BaseResult.ok();
    }

    @ApiOperation(value = "中间件存储回滚", notes = "中间件存储回滚")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pvcName", value = "pvc名称", paramType = "query", dataTypeClass = String.class),
    })
    @Authority(power = 1)
    @PutMapping("/{pvcName}/rollBack")
    public BaseResult rollBackPvc(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("middlewareName") String middlewareName,
                                  @PathVariable("pvcName") String pvcName) {
        middlewarePvcService.rollback(clusterId, namespace, middlewareName, pvcName);
        // 避免因k8s资源状态加载过慢导致的后续接口信息获取错误，此处沉睡1s
        try {
            Thread.sleep(1000);
        } catch (Exception ignored){
        }
        return BaseResult.ok();
    }

}
