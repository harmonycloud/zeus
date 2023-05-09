package com.middleware.zeus.controller.system;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.github.pagehelper.PageInfo;
import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.*;
import com.middleware.zeus.service.system.AlertService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @Date 2023/5/5 11:27 上午
 */
@Api(tags = {"监控告警","告警中心"}, value = "告警中心")
@RestController
@RequestMapping("/alert")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @ApiOperation(value = "查询告警记录索引", notes = "查询告警记录索引")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "alertType", value = "告警对象类型", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{alertType}/record/index")
    public BaseResult<List<AlertRecordIndex>> alertRecordIndex(@PathVariable("alertType") String alertType) {
        return BaseResult.ok(alertService.alertRecordIndex(alertType));
    }

    @ApiOperation(value = "查询告警记录过滤条件", notes = "查询告警记录过滤条件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "alertType", value = "告警对象类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "告警对象类型", required = false, paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{alertType}/record/filter")
    public BaseResult<List<AlertRecordIndex>> alertRecordFilter(@PathVariable("alertType") String alertType,
                                                                @RequestParam(value = "clusterId", required = false) String clusterId) {
        return BaseResult.ok(alertService.alertRecordFilter(alertType, clusterId));
    }

    @ApiOperation(value = "查询告警记录", notes = "查询告警记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "alertType", value = "告警对象类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertRecordQueryDto", value = "告警记录查询", paramType = "query", dataTypeClass = AlertRecordQueryDto.class),
    })
    @PostMapping("/{alertType}/record")
    public BaseResult<PageInfo<AlertDTO>> searchAlertRecord(@PathVariable("alertType") String alertType,
                                                            @RequestBody AlertRecordQueryDto alertRecordQueryDto) {
        alertRecordQueryDto.setAlertType(alertType);
        if (alertRecordQueryDto.getCurrent() == null){
            alertRecordQueryDto.setCurrent(1);
        }
        if (alertRecordQueryDto.getSize() == null){
            alertRecordQueryDto.setSize(10);
        }
        return BaseResult.ok(alertService.searchAlertRecord(alertRecordQueryDto));
    }

    @ApiOperation(value = "新建/接入告警对象", notes = "新建/接入告警对象")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("/target")
    public BaseResult alertTarget(@RequestBody AlertTargetDto alertTargetDto) {
        alertService.alertTarget(alertTargetDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询告警对象列表", notes = "查询告警对象列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/target")
    public BaseResult<List<AlertTargetDto>> alertTarget(@RequestParam("clusterId") String clusterId) {
        return BaseResult.ok(alertService.alertTargetList(clusterId));
    }

    @ApiOperation(value = "查询告警对象规则", notes = "查询告警对象规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "targetName", value = "告警对象名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "prometheusRuleName", value = "prometheusRule名称", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/target/{targetName}/rule")
    public BaseResult<List<AlertTargetDto>> alertTargetRule(@PathVariable("targetName") String targetName,
                                                            @RequestParam("clusterId") String clusterId,
                                                            @RequestParam("namespace") String namespace,
                                                            @RequestParam("prometheusRuleName") String prometheusRuleName) {
        return BaseResult.ok(alertService.alertRule(targetName, clusterId, namespace, prometheusRuleName));
    }

    @ApiOperation(value = "查询告警用户列表", notes = "查询告警用户列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "allocatable", value = "可分配的", paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping("/user")
    public BaseResult<List<AlertUserDto>> alertUser(@RequestParam("clusterId") String clusterId,
                                                    @RequestParam(value = "allocatable", required = false, defaultValue = "false") Boolean allocatable) {
        return BaseResult.ok(alertService.alertUser(clusterId, allocatable));
    }

    @ApiOperation(value = "新增告警用户", notes = "查询告警用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "alertUserDto", value = "集群id", paramType = "query", dataTypeClass = AlertUserListDto.class),
    })
    @PostMapping("/user")
    public BaseResult addAlertUser(@RequestParam("clusterId") String clusterId,
                                   @RequestBody AlertUserListDto alertUserListDto) {
        alertService.addAlertUser(clusterId, alertUserListDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "移除告警用户", notes = "移除告警用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "path", dataTypeClass = Boolean.class),
    })
    @DeleteMapping("/user/{username}")
    public BaseResult removeAlertUser(@PathVariable("username") String username,
                                      @RequestParam("clusterId") String clusterId) {
        alertService.removeAlertUser(username, clusterId);
        return BaseResult.ok();
    }
}
