package com.middleware.zeus.controller.middleware;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.middleware.MiddlewareLogAlertDto;
import com.middleware.zeus.service.middleware.MiddlewareLogAlertsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @Date 2025/5/14 下午5:20
 */
@Api(tags = {"监控告警","服务告警"}, value = "中间件日志告警")
@RestController
@RequestMapping(value = {"/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/logalert"})
public class MiddlewareLogAlertsController {

    @Autowired
    private MiddlewareLogAlertsService middlewareLogAlertsService;

    @ApiOperation(value = "查询日志告警规则", notes = "查询日志告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/rules")
    @Authority(power = 1)
    public BaseResult<List<MiddlewareLogAlertDto>> listRules(@PathVariable("clusterId") String clusterId,
                                                            @PathVariable(value = "namespace") String namespace,
                                                            @PathVariable(value = "middlewareName") String middlewareName,
                                                            @RequestParam("type") String type) {
        return BaseResult.ok(middlewareLogAlertsService.listRules(clusterId, namespace, middlewareName, type));
    }

    @ApiOperation(value = "创建日志告警规则", notes = "创建日志告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareLogAlertDto", value = "告警数据对象", paramType = "query", dataTypeClass = MiddlewareLogAlertDto.class)
    })
    @PostMapping("/rules")
    @Authority(power = 1)
    public BaseResult addRules(@PathVariable("clusterId") String clusterId,
                               @PathVariable(value = "namespace") String namespace,
                               @PathVariable(value = "middlewareName") String middlewareName,
                               @RequestBody MiddlewareLogAlertDto middlewareLogAlertDto) {
        middlewareLogAlertDto.setClusterId(clusterId).setNamespace(namespace).setMiddlewareName(middlewareName);
        middlewareLogAlertsService.createRules(middlewareLogAlertDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新日志告警规则", notes = "更新日志告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareLogAlertDto", value = "告警数据对象", paramType = "query", dataTypeClass = MiddlewareLogAlertDto.class)
    })
    @PutMapping("/rules")
    @Authority(power = 1)
    public BaseResult updateRules(@PathVariable("clusterId") String clusterId,
                                  @PathVariable(value = "namespace") String namespace,
                                  @PathVariable(value = "middlewareName") String middlewareName,
                                  @RequestBody MiddlewareLogAlertDto middlewareLogAlertDto) {
        middlewareLogAlertDto.setClusterId(clusterId).setNamespace(namespace).setMiddlewareName(middlewareName);
        middlewareLogAlertsService.updateRules(middlewareLogAlertDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除日志告警规则", notes = "删除日志告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertName", value = "告警名称", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping("/rules")
    @Authority(power = 1)
    public BaseResult deleteRules(@PathVariable("clusterId") String clusterId,
                                  @PathVariable(value = "namespace") String namespace,
                                  @PathVariable(value = "middlewareName") String middlewareName,
                                  @RequestParam("type") String type,
                                  @RequestParam("alertName") String alertName) {
        middlewareLogAlertsService.deleteRules(clusterId, namespace, middlewareName, type, alertName);
        return BaseResult.ok();
    }

}
