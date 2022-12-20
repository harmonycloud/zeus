package com.middleware.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.middleware.*;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.service.middleware.MiddlewareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static com.middleware.caas.common.constants.CommonConstant.ASTERISK;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = {"服务列表", "服务管理"}, value = "分区下中间件", description = "分区下中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares")
public class MiddlewareController {

    @Autowired
    private MiddlewareService middlewareService;

    @ApiOperation(value = "查询中间件列表", notes = "查询中间件列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    @Authority
    public BaseResult<MiddlewareBriefInfoDTO> list(@PathVariable("clusterId") String clusterId,
                                                   @PathVariable("namespace") String namespace,
                                                   @RequestParam(value = "type", required = false) String type,
                                                   @RequestParam(value = "keyword", required = false) String keyword,
                                                   @RequestParam(value = "projectId", required = false) String projectId) throws Exception {
        if (namespace.equals(ASTERISK)){
            namespace = null;
        }
        return BaseResult.ok(middlewareService.list(clusterId, namespace, type, keyword, projectId));
    }

    @ApiOperation(value = "查询中间件详情", notes = "查询中间件详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{middlewareName}")
    @Authority
    public BaseResult<Middleware> detail(@PathVariable("clusterId") String clusterId,
                                         @PathVariable("namespace") String namespace,
                                         @PathVariable("middlewareName") String name,
                                         @RequestParam("type") String type) {
        return BaseResult.ok(middlewareService.detail(clusterId, namespace, name, type));
    }



    @ApiOperation(value = "创建中间件", notes = "创建中间件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middleware", value = "middleware信息", paramType = "query", dataTypeClass = Middleware.class)
    })
    @PostMapping
    @Authority(power = 2)
    public BaseResult<Middleware> create(@PathVariable("clusterId") String clusterId,
                                         @PathVariable("namespace") String namespace,
                                         @RequestBody Middleware middleware) {
        middleware.setClusterId(clusterId).setNamespace(namespace);
        return BaseResult.ok(middlewareService.create(middleware));
    }

    @ApiOperation(value = "恢复中间件", notes = "恢复中间件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middleware", value = "middleware信息", paramType = "query", dataTypeClass = Middleware.class)
    })
    @PostMapping("/{middlewareName}/recovery")
    @Authority(power = 1)
    public BaseResult recovery(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("middlewareName") String name,
                               @RequestBody Middleware middleware) {
        middleware.setClusterId(clusterId).setNamespace(namespace).setName(name);
        middlewareService.recovery(middleware);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改中间件", notes = "修改中间件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middleware", value = "middleware信息", paramType = "query", dataTypeClass = Middleware.class)
    })
    @PutMapping("/{middlewareName}")
    @Authority(power = 1)
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String name,
                             @RequestBody Middleware middleware) {
        middleware.setClusterId(clusterId).setNamespace(namespace).setName(name);
        middlewareService.update(middleware);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除中间件", notes = "删除中间件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{middlewareName}")
    @Authority(power = 3)
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String name,
                             @RequestParam("type") String type) {
        middlewareService.delete(clusterId, namespace, name, type);
        return BaseResult.ok();
    }
    @ApiOperation(value = "删除中间件相关存储", notes = "删除中间件相关存储")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{middlewareName}/storage")
    @Authority(power = 3)
    public BaseResult deleteStorage(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @PathVariable("middlewareName") String name,
                                    @RequestParam("type") String type) {
        middlewareService.deleteStorage(clusterId, namespace, name, type);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询中间件切换信息", notes = "查询中间件切换信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{middlewareName}/switch")
    @Authority(power = 1)
    public BaseResult<SwitchInfo> autoSwitch(@PathVariable("clusterId") String clusterId,
                                             @PathVariable("namespace") String namespace,
                                             @PathVariable("middlewareName") String name,
                                             @RequestParam("type") String type) {
        return BaseResult.ok(middlewareService.autoSwitch(clusterId, namespace, name, type));
    }

    @ApiOperation(value = "中间件切换", notes = "中间件切换")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "slaveName", value = "从节点名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "isAuto", value = "true开启/false关闭自动切换，不传/传null都为手动切换", paramType = "query", dataTypeClass = Boolean.class),
    })
    @PutMapping("/{middlewareName}/switch")
    @Authority(power = 1)
    public BaseResult switchMiddleware(@PathVariable("clusterId") String clusterId,
                                       @PathVariable("namespace") String namespace,
                                       @PathVariable("middlewareName") String name,
                                       @RequestParam("type") String type,
                                       @RequestParam(value = "slaveName", required = false) String slaveName,
                                       @RequestParam(value = "isAuto", required = false) Boolean isAuto) {
        middlewareService.switchMiddleware(clusterId, namespace, name, type, slaveName, isAuto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "性能监控", notes = "性能监控")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("{middlewareName}/monitor")
    @Authority(power = 1)
    public BaseResult<MonitorDto> monitor(@PathVariable("clusterId") String clusterId,
                                          @PathVariable("namespace") String namespace,
                                          @PathVariable("middlewareName") String name,
                                          @RequestParam("type") String type,
                                          @RequestParam("chartVersion") String chartVersion) {
        return BaseResult.ok(middlewareService.monitor(clusterId, namespace, name, type, chartVersion));
    }

    @ApiOperation(value = "查询服务版本", notes = "查询服务版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("{middlewareName}/version")
    @Authority(power = 1)
    public BaseResult version(@PathVariable("clusterId") String clusterId,
                              @PathVariable("namespace") String namespace,
                              @PathVariable("middlewareName") String middlewareName,
                              @RequestParam("type") String type) {
        return BaseResult.ok(middlewareService.version(clusterId, namespace, middlewareName, type));
    }

    @ApiOperation(value = "服务版本升级", notes = "服务版本升级")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartName", value = "中间件chartName", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "upgradeChartVersion", value = "升级chart版本", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("{middlewareName}/upgradeChart")
    @Authority(power = 1)
    public BaseResult upgradeChart(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("middlewareName") String middlewareName,
                                   @RequestParam("type") String type,
                                   @RequestParam("chartName") String chartName,
                                   @RequestParam("upgradeChartVersion") String upgradeChartVersion) {
        return middlewareService.upgradeChart(clusterId, namespace, middlewareName, type, chartName, upgradeChartVersion);
    }

    @ApiOperation(value = "服务版本升级校验", notes = "服务版本升级校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartName", value = "中间件chartName", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "upgradeChartVersion", value = "升级chart版本", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("{middlewareName}/upgradeCheck")
    @Authority(power = 1)
    public BaseResult upgradeCheck(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("middlewareName") String middlewareName,
                                   @RequestParam("type") String type,
                                   @RequestParam("chartName") String chartName,
                                   @RequestParam("upgradeChartVersion") String upgradeChartVersion) {
        return middlewareService.upgradeCheck(clusterId, namespace, middlewareName, type, chartName, upgradeChartVersion);
    }

    @ApiOperation(value = "重启服务", notes = "重启服务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class)
    })
    @PostMapping("/{middlewareName}/reboot")
    @Authority(power = 1)
    public BaseResult reboot(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String name,
                             @RequestParam("type") String type) {
        middlewareService.reboot(clusterId, namespace, name, type);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新存储", notes = "更新存储")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middleware", value = "中间件内容", paramType = "query", dataTypeClass = Middleware.class)
    })
    @PutMapping("/{middlewareName}/storage")
    @Authority(power = 1)
    public BaseResult updateStorage(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @PathVariable("middlewareName") String name,
                                    @RequestBody Middleware middleware) {
        middleware.setClusterId(clusterId);
        middleware.setNamespace(namespace);
        middleware.setName(name);
        middlewareService.updateStorage(middleware);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询拓扑图相关信息", notes = "查询拓扑图相关信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{middlewareName}/topology")
    @Authority(power = 1)
    public BaseResult<MiddlewareTopologyDTO> topology(@PathVariable("clusterId") String clusterId,
                                                      @PathVariable("namespace") String namespace,
                                                      @PathVariable("middlewareName") String name,
                                                      @RequestParam("type") String type) throws Exception {
        return BaseResult.ok(middlewareService.topology(clusterId, namespace, name, type));
    }

    @ApiOperation(value = "查询管理控制台地址", notes = "查询管理控制台地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{middlewareName}/platform")
    @Authority(power = 1)
    public BaseResult<String> platform(@PathVariable("clusterId") String clusterId,
                                                      @PathVariable("namespace") String namespace,
                                                      @PathVariable("middlewareName") String name,
                                                      @RequestParam("type") String type) {
        return BaseResult.ok(middlewareService.platform(clusterId, namespace, name, type));
    }

    @ApiOperation(value = "查询中间件地址", notes = "查询中间件地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "version", value = "中间件版本", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/middlewareImage")
    @Authority(power = 1)
    public BaseResult<String> middlewareImage(@RequestParam("type") String type,
                                              @RequestParam("version") String version) {
        return BaseResult.ok(middlewareService.middlewareImage(type, version));
    }

}
