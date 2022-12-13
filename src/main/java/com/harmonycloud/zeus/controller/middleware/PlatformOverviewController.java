package com.harmonycloud.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.AlertDTO;
import com.middleware.caas.common.model.middleware.ClusterQuotaDTO;
import com.middleware.caas.common.model.middleware.MiddlewareBriefInfoDTO;
import com.middleware.caas.common.model.middleware.MiddlewareOperatorDTO;
import com.harmonycloud.zeus.bean.AlertMessageDTO;
import com.harmonycloud.zeus.bean.BeanOperationAudit;
import com.harmonycloud.zeus.bean.PlatformOverviewDTO;
import com.harmonycloud.zeus.service.middleware.OverviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @ApiOperation(value = "平台总览", notes = "平台总览")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult<PlatformOverviewDTO> getPlatformOverview(@RequestParam(value = "clusterId", required = false) String clusterId) {
        return BaseResult.ok(overviewService.getClusterPlatformOverview(clusterId));
    }

    @ApiOperation(value = "资源信息", notes = "资源信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/resources")
    public BaseResult<ClusterQuotaDTO> resources(@RequestParam(value = "clusterId", required = false) String clusterId) {
        return BaseResult.ok(overviewService.resources(clusterId));
    }

    @ApiOperation(value = "控制器状态信息", notes = "控制器状态信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/operatorStatus")
    public BaseResult<MiddlewareOperatorDTO> operatorStatus(@RequestParam(value = "clusterId", required = false) String clusterId) {
        return BaseResult.ok(overviewService.operatorStatus(clusterId));
    }

    @ApiOperation(value = "告警信息", notes = "告警信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "当前页", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "size", value = "每页数量", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "level", value = "告警级别", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/alertInfo")
    public BaseResult<AlertMessageDTO> getAlertInfo(@RequestParam(value = "clusterId", required = false) String clusterId,
                                                    @RequestParam(value = "current", defaultValue = "1", required = false) Integer current,
                                                    @RequestParam(value = "size", defaultValue = "10", required = false) Integer size,
                                                    @RequestParam(value = "level", required = false) String level) {
        return BaseResult.ok(overviewService.getAlertInfo(clusterId, current, size, level));
    }

    @ApiOperation(value = "服务信息", notes = "服务信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/middlewareInfo")
    public BaseResult<List<MiddlewareBriefInfoDTO>> getMiddlewareInfo(@RequestParam(value = "clusterId", required = false) String clusterId) {
        return BaseResult.ok(overviewService.getClusterMiddlewareInfo(clusterId));
    }

    @ApiOperation(value = "操作审计信息", notes = "操作审计信息")
    @GetMapping("/audit")
    public BaseResult<List<BeanOperationAudit>> audit() {
        return BaseResult.ok(overviewService.recentAudit());
    }

    @ApiOperation(value = "版本信息", notes = "版本信息")
    @GetMapping("/version")
    public BaseResult<Map<String, String>> version() {
        return BaseResult.ok(overviewService.version());
    }

    @ApiOperation(value = "告警记录", notes = "告警记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", required = false, dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "query", required = false, dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "当前页", paramType = "query", required = false, dataTypeClass = String.class),
            @ApiImplicitParam(name = "size", value = "size", paramType = "query", required = false, dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", required = false, dataTypeClass = String.class),
    })
    @GetMapping("/alerts")
    public BaseResult<List<AlertDTO>> getAlertsRecord(@RequestParam(value = "clusterId", required = false) String clusterId,
                                                      @RequestParam(value = "namespace", required = false) String namespace,
                                                      @RequestParam(value = "current", required = false) Integer current,
                                                      @RequestParam(value = "size", required = false) Integer size,
                                                      @RequestParam(value = "middlewareName", required = false) String middlewareName,
                                                      @RequestParam(value = "level", required = false) String level,
                                                      @RequestParam(value = "lay") String lay,
                                                      @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(overviewService.getAlertRecord(clusterId, namespace, middlewareName, current, size,level, keyword, lay));
    }

}
