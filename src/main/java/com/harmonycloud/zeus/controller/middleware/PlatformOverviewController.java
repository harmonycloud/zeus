package com.harmonycloud.zeus.controller.middleware;

import java.util.List;

import com.harmonycloud.zeus.service.middleware.OverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.AlertDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareOverviewDTO;
import com.harmonycloud.zeus.service.k8s.ConfigMapService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author zackchen
 * @date 2021/05/06
 */
@Api(tags = "platformOverview", value = "平台总览", description = "平台总览")
@RestController
@RequestMapping("/platform/overview")
public class PlatformOverviewController {

    @Autowired
    private OverviewService overviewService;
    @Autowired
    private ConfigMapService configMapService;

    @ApiOperation(value = "平台总览", notes = "平台总览")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", required = false, dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult getPlatformOverview(@RequestParam(value = "clusterId", required = false) String clusterId) {
        return overviewService.getClusterPlatformOverview(clusterId);
    }

    @ApiOperation(value = "平台总览-获取服务信息", notes = "服务信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", required = false, dataTypeClass = String.class)
    })
    @GetMapping("/middlewareInfo")
    public BaseResult getMiddlewareInfo(@RequestParam(value = "clusterId", required = false) String clusterId) {
        return overviewService.getClusterMiddlewareInfo(clusterId);
    }

    @ApiOperation(value = "查询告警记录", notes = "查询告警记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", required = false, dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "query", required = false, dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "当前页", paramType = "query", required = false, dataTypeClass = String.class),
            @ApiImplicitParam(name = "size", value = "size", paramType = "query", required = false, dataTypeClass = String.class),
            @ApiImplicitParam(name = "level", value = "等级", paramType = "query", required = false, dataTypeClass = String.class),
    })
    @GetMapping("/alerts")
    public BaseResult<List<AlertDTO>> getAlertsRecord(@RequestParam(value = "clusterId", required = false) String clusterId,
                                                      @RequestParam(value = "namespace", required = false) String namespace,
                                                      @RequestParam(value = "current", required = false) Integer current,
                                                      @RequestParam(value = "size", required = false) Integer size,
                                                      @RequestParam(value = "level", required = false) String level) {
        return BaseResult.ok(overviewService.getAlertRecord(clusterId, namespace, current, size, level));
    }
}
