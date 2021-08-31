package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MiddlewareStatusDto;
import com.harmonycloud.zeus.service.middleware.OverviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/3/29 12:28 下午
 */
@Api(tags = "middlewareOverview", value = "空间概览", description = "空间概览")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/overview")
public class MiddlewareOverviewController {

    @Autowired
    private OverviewService overviewService;


    @ApiOperation(value = "查询实例情况", notes = "查询实例情况")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/status")
    public BaseResult<List<MiddlewareStatusDto>> listStatus(@PathVariable("clusterId") String clusterId,
                                                            @PathVariable("namespace") String namespace) {
        return BaseResult.ok(overviewService.getMiddlewareStatus(clusterId, namespace));
    }

    @ApiOperation(value = "查询实例监控数据", notes = "查询实例监控数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startTime", value = "开始时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endTime", value = "结束时间", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/monitors")
    public BaseResult<List<MiddlewareStatusDto>> getMonitorInfo(@PathVariable("clusterId") String clusterId,
                                                                @PathVariable("namespace") String namespace,
                                                                @RequestParam("name") String name,
                                                                @RequestParam("type") String type,
                                                                @RequestParam(value = "startTime") String startTime,
                                                                @RequestParam(value = "endTime") String endTime) throws Exception {
        return BaseResult.ok(overviewService.getMonitorInfo(clusterId, namespace, name, type, startTime, endTime));
    }

}
