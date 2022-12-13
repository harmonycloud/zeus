package com.harmonycloud.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.AlertSettingDTO;
import com.middleware.caas.common.model.AlertUserDTO;
import com.middleware.caas.common.model.AlertsUserDTO;
import com.middleware.caas.common.model.middleware.MiddlewareAlertsDTO;
import com.harmonycloud.zeus.annotation.Authority;
import com.harmonycloud.zeus.service.middleware.MiddlewareAlertsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/26 10:12 上午
 */
@Api(tags = {"监控告警","告警中心"}, value = "中间件告警", description = "中间件告警")
@RestController
@RequestMapping(value = {"/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/rules","/clusters/{clusterId}/rules","/rules"})
public class MiddlewareAlertsController {

    @Autowired
    private MiddlewareAlertsService middlewareAlertsService;

    @ApiOperation(value = "查询已设置告警规则", notes = "查询已设置告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "lay",value = "告警层面",paramType = "query",dataTypeClass = String .class),
            @ApiImplicitParam(name = "keyword", value = "关键字", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/used")
    @Authority(power = 1)
    public BaseResult<List<MiddlewareAlertsDTO>> listUsedRules(@PathVariable("clusterId") String clusterId,
                                                               @PathVariable(value = "namespace", required = false) String namespace,
                                                               @PathVariable(value = "middlewareName", required = false) String middlewareName,
                                                               @RequestParam(value = "lay") String lay,
                                                               @RequestParam(value = "keyword", required = false) String keyword) throws Exception {
        return BaseResult.ok(middlewareAlertsService.listUsedRules(clusterId, namespace, middlewareName, lay, keyword));
    }

    @ApiOperation(value = "查询告警规则", notes = "查询告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    @Authority(power = 1)
    public BaseResult<List<MiddlewareAlertsDTO>> listRules(@PathVariable("clusterId") String clusterId,
                                                           @PathVariable(value = "namespace", required = false) String namespace,
                                                           @PathVariable(value = "middlewareName", required = false) String middlewareName,
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
    @PostMapping
    @Authority(power = 1)
    public BaseResult createRules(@PathVariable("clusterId") String clusterId,
                                  @PathVariable(value = "namespace", required = false) String namespace,
                                  @PathVariable(value = "middlewareName", required = false) String middlewareName,
                                  @RequestBody AlertsUserDTO alertsUserDTO) throws Exception {
        middlewareAlertsService.createRules(clusterId, namespace, middlewareName,
                alertsUserDTO.getMiddlewareAlertsDTOList());
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除服务告警规则", notes = "删除服务告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alert", value = "告警名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertRuleId", value = "规则ID", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping
    @Authority(power = 1)
    public BaseResult deleteRules(@PathVariable("clusterId") String clusterId,
                                  @PathVariable(value = "namespace", required = false) String namespace,
                                  @PathVariable(value = "middlewareName", required = false) String middlewareName,
                                  @RequestParam("alert") String alert,
                                  @RequestParam("alertRuleId") String alertRuleId) {
        middlewareAlertsService.deleteRules(clusterId, namespace, middlewareName, alert,alertRuleId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改服务告警规则", notes = "修改服务告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ding", value = "是否选择钉钉通知", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertRuleId", value = "规则ID", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertUserDTO", value = "中间件告警规则和用户", paramType = "query", dataTypeClass = AlertUserDTO.class)
    })
    @PostMapping("/update")
    @Authority(power = 1)
    public BaseResult updateRules(@PathVariable("clusterId") String clusterId,
                                  @PathVariable(value = "namespace", required = false) String namespace,
                                  @PathVariable(value = "middlewareName", required = false) String middlewareName,
                                  @RequestParam("ding") String ding,
                                  @RequestParam("alertRuleId") String alertRuleId,
                                  @RequestBody AlertUserDTO alertUserDTO) throws Exception {
        middlewareAlertsService.updateRules(clusterId, namespace, middlewareName, ding, alertRuleId,alertUserDTO);
        return BaseResult.ok();
    }


    @ApiOperation(value = "创建系统告警规则", notes = "创建系统告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ding", value = "是否选择钉钉通知", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertsUserDTO", value = "中间件告警规则和用户", paramType = "query", dataTypeClass = AlertsUserDTO.class)
    })
    @PostMapping("/system")
    public BaseResult createSystemRules(@PathVariable("clusterId") String clusterId,
                                        @RequestParam("ding") String ding,
                                        @RequestBody AlertsUserDTO alertsUserDTO) {
        middlewareAlertsService.createSystemRule(clusterId, ding, alertsUserDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改系统告警规则", notes = "修改系统告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ding", value = "是否选择钉钉通知", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertRuleId", value = "规则ID", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertUserDTO", value = "中间件告警规则和用户", paramType = "query", dataTypeClass = AlertUserDTO.class)
    })
    @PutMapping("/system")
    public BaseResult updateSystemRules(@PathVariable("clusterId") String clusterId,
                                        @RequestParam("ding") String ding,
                                        @RequestParam("alertRuleId") String alertRuleId,
                                        @RequestBody AlertUserDTO alertUserDTO) {
        middlewareAlertsService.updateSystemRules(clusterId, ding, alertRuleId, alertUserDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除系统告警规则", notes = "删除系统告警规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alert", value = "告警名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertRuleId", value = "规则ID", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping("/system")
    public BaseResult deleteSystemRules(@PathVariable("clusterId") String clusterId,
                                        @RequestParam("alert") String alert,
                                        @RequestParam("alertRuleId") String alertRuleId) {
        middlewareAlertsService.deleteSystemRules(clusterId, alert, alertRuleId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询告警规则详情", notes = "查询告警规则详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "alertRuleId", value = "规则ID", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/detail")
    @Authority(power = 1)
    public BaseResult<MiddlewareAlertsDTO> alertRuleDetail(@RequestParam("alertRuleId") String alertRuleId) {
        return BaseResult.ok(middlewareAlertsService.alertRuleDetail(alertRuleId));
    }

    @ApiOperation(value = "保存服务告警设置", notes = "保存服务告警设置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertSettingDTO", value = "告警设置信息", paramType = "query", dataTypeClass = AlertSettingDTO.class),
    })
    @PostMapping("/alertSetting")
    public BaseResult saveAlertSetting(
            @PathVariable("clusterId") String clusterId,
            @PathVariable("namespace") String namespace,
            @PathVariable("middlewareName") String middlewareName,
            @RequestBody AlertSettingDTO alertSettingDTO) {
        alertSettingDTO.setClusterId(clusterId);
        alertSettingDTO.setNamespace(namespace);
        alertSettingDTO.setMiddlewareName(middlewareName);
        middlewareAlertsService.saveServiceAlertSetting(alertSettingDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "保存系统告警设置", notes = "保存系统告警设置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "alertSettingDTO", value = "告警设置信息", paramType = "query", dataTypeClass = AlertSettingDTO.class)
    })
    @PostMapping("/systemAlertSetting")
    public BaseResult saveSystemAlertSetting(@RequestBody AlertSettingDTO alertSettingDTO) {
        middlewareAlertsService.saveSystemAlertSetting(alertSettingDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询服务告警设置", notes = "查询系统告警设置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/alertSetting")
    public BaseResult queryServiceAlertSetting(@PathVariable("clusterId") String clusterId,
                                        @PathVariable("namespace") String namespace,
                                        @PathVariable("middlewareName") String middlewareName) {
        return BaseResult.ok(middlewareAlertsService.queryAlertSetting(clusterId, namespace, middlewareName));
    }

    @ApiOperation(value = "查询系统告警设置", notes = "查询系统告警设置")
    @GetMapping("/systemAlertSetting")
    public BaseResult querySystemAlertSetting() {
        return BaseResult.ok(middlewareAlertsService.queryAlertSetting());
    }

}

