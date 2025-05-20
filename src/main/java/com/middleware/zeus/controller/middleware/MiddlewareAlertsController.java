package com.middleware.zeus.controller.middleware;

import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.AlertUserDto;
import com.middleware.zeus.common.model.AlertUserListDto;
import com.middleware.zeus.common.model.MiddlewareAlertsListDto;
import com.middleware.zeus.common.model.middleware.MiddlewareAlertsDTO;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.service.middleware.MiddlewareAlertsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/26 10:12 上午
 */
@Api(tags = {"监控告警","服务告警"}, value = "中间件告警")
@RestController
@RequestMapping(value = {"/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/alert"})
@Slf4j
public class MiddlewareAlertsController {

    @Autowired
    private MiddlewareAlertsService middlewareAlertsService;

    @ApiOperation(value = "查询已设置告警规则", notes = "查询已设置告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "关键字", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/rules/used")
    @Authority(power = 1)
    public BaseResult<List<MiddlewareAlertsDTO>> listUsedRules(@PathVariable(value = "clusterId") String clusterId,
                                                               @PathVariable(value = "namespace") String namespace,
                                                               @PathVariable(value = "middlewareName") String middlewareName,
                                                               @RequestParam(value = "type") String type,
                                                               @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(middlewareAlertsService.listUsedRules(clusterId, namespace, middlewareName, type, keyword));
    }

    @ApiOperation(value = "查询告警规则", notes = "查询告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/rules")
    @Authority(power = 1)
    public BaseResult<List<MiddlewareAlertsDTO>> listRules(@PathVariable("clusterId") String clusterId,
                                                           @PathVariable(value = "namespace") String namespace,
                                                           @PathVariable(value = "middlewareName") String middlewareName,
                                                           @RequestParam("type") String type) throws Exception {
        return BaseResult.ok(middlewareAlertsService.listRules(clusterId, namespace, middlewareName, type));
    }

    @ApiOperation(value = "创建服务告警规则", notes = "创建服务告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertsUserDTO", value = "中间件告警规则", paramType = "query", dataTypeClass = List.class)
    })
    @PostMapping("/rules")
    @Authority(power = 1)
    public BaseResult createRules(@PathVariable("clusterId") String clusterId,
                                  @PathVariable(value = "namespace") String namespace,
                                  @PathVariable(value = "middlewareName") String middlewareName,
                                  @RequestBody MiddlewareAlertsListDto middlewareAlertsListDto) throws Exception {
        middlewareAlertsService.createRules(clusterId, namespace, middlewareName, middlewareAlertsListDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询告警规则详情", notes = "查询告警规则详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertName", value = "规则名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/rules/{alertName}")
    @Authority(power = 1)
    public BaseResult<MiddlewareAlertsDTO> alertRuleDetail(@PathVariable("clusterId") String clusterId,
                                                           @PathVariable(value = "namespace") String namespace,
                                                           @PathVariable(value = "middlewareName") String middlewareName,
                                                           @PathVariable("alertName") String alertName) {
        return BaseResult.ok(middlewareAlertsService.detail(clusterId, namespace, middlewareName, alertName));
    }

    @ApiOperation(value = "删除服务告警规则", notes = "删除服务告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertName", value = "告警名称", paramType = "path", dataTypeClass = String.class)
    })
    @DeleteMapping("/rules/{alertName}")
    @Authority(power = 1)
    public BaseResult deleteRules(@PathVariable("clusterId") String clusterId,
                                  @PathVariable(value = "namespace") String namespace,
                                  @PathVariable(value = "middlewareName") String middlewareName,
                                  @PathVariable("alertName") String alertName) {
        middlewareAlertsService.deleteRules(clusterId, namespace, middlewareName, alertName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改服务告警规则", notes = "修改服务告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertName", value = "规则名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareAlertsDTO", value = "中间件告警规则和用户", paramType = "query", dataTypeClass = MiddlewareAlertsDTO.class)
    })
    @PutMapping("/rules/{alertName}")
    @Authority(power = 1)
    public BaseResult updateRules(@PathVariable("clusterId") String clusterId,
                                  @PathVariable(value = "namespace") String namespace,
                                  @PathVariable(value = "middlewareName") String middlewareName,
                                  @PathVariable("alertName") String alertName,
                                  @RequestBody MiddlewareAlertsDTO middlewareAlertsDTO) {
        middlewareAlertsDTO.setAlert(alertName);
        middlewareAlertsService.updateRules(clusterId, namespace, middlewareName, middlewareAlertsDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询告警用户列表", notes = "查询告警用户列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "allocatable", value = "可分配的", paramType = "query", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "organId", value = "组织id", required = false, paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", required = false, paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/user")
    public BaseResult<List<AlertUserDto>> alertUser(@PathVariable("clusterId") String clusterId,
                                                    @PathVariable("namespace") String namespace,
                                                    @PathVariable("middlewareName") String middlewareName,
                                                    @RequestParam(value = "allocatable", required = false, defaultValue = "false") Boolean allocatable,
                                                    @RequestParam(value = "organId", required = false) String organId,
                                                    @RequestParam(value = "projectId", required = false) String projectId) {
        return BaseResult.ok(middlewareAlertsService.alertUser(clusterId, namespace, middlewareName, allocatable, organId, projectId));
    }

    @ApiOperation(value = "新增告警用户", notes = "查询告警用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertUserDto", value = "集群id", paramType = "query", dataTypeClass = AlertUserListDto.class),
    })
    @PostMapping("/user")
    public BaseResult addAlertUser(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("middlewareName") String middlewareName,
                                   @RequestBody AlertUserListDto alertUserListDto) {
        middlewareAlertsService.addAlertUser(clusterId, namespace, middlewareName, alertUserListDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "移除告警用户", notes = "移除告警用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "path", dataTypeClass = Boolean.class),
    })
    @DeleteMapping("/user")
    public BaseResult removeAlertUser(@PathVariable("clusterId") String clusterId,
                                      @PathVariable("namespace") String namespace,
                                      @PathVariable("middlewareName") String middlewareName,
                                      @RequestBody AlertUserListDto alertUserListDto) {
        for (AlertUserDto alertUserDto : alertUserListDto.getAlertUserDtoList()) {
            try {
                middlewareAlertsService.removeAlertUser(clusterId, namespace, middlewareName,
                    alertUserDto.getUsername());
            } catch (Exception e) {
                log.error("remove alert user failed: {}", alertUserDto.getUsername(), e);
            }
        }
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询备份告警开关状态", notes = "查询备份告警开关状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "lay",value = "告警层面",paramType = "query",dataTypeClass = String .class),
            @ApiImplicitParam(name = "keyword", value = "关键字", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/backup")
    @Authority(power = 1)
    public BaseResult<List<MiddlewareAlertsDTO>> getBackupAlert(@PathVariable("clusterId") String clusterId,
                                                                @PathVariable("namespace") String namespace,
                                                                @PathVariable("middlewareName") String middlewareName) {
        return BaseResult.ok(middlewareAlertsService.getBackupAlert(clusterId, namespace, middlewareName));
    }

    @ApiOperation(value = "修改备份告警开关状态", notes = "修改备份告警开关状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "enable",value = "告警层面",paramType = "query",dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "type",value = "中间件类型",paramType = "query",dataTypeClass = Boolean.class),
    })
    @PutMapping("/backup")
    @Authority(power = 1)
    public BaseResult<List<MiddlewareAlertsDTO>> editBackupAlert(@PathVariable("clusterId") String clusterId,
                                                                 @PathVariable("namespace") String namespace,
                                                                 @PathVariable("middlewareName") String middlewareName,
                                                                 @RequestParam("type") String type,
                                                                 @RequestParam("enable") Boolean enable) {
        middlewareAlertsService.editBackupAlert(clusterId, namespace, middlewareName, type, enable);
        return BaseResult.ok();
    }
}

