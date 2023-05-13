package com.middleware.zeus.controller.middleware;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.github.pagehelper.PageInfo;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.AlertDTO;
import com.middleware.zeus.common.model.AlertRecordQueryDto;
import com.middleware.zeus.service.middleware.MiddlewareAlertRecordService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author liyinlong
 * @since 2023/4/6 2:46 下午
 */
@Api(tags = {"监控告警","服务告警"}, value = "服务告警", description = "服务告警")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/records")
public class MiddlewareAlertRecord {

    @Autowired
    private MiddlewareAlertRecordService middlewareAlertRecordService;

    @ApiOperation(value = "查询服务告警记录", notes = "查询服务告警记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "alertRecordQueryDto", value = "告警记录查询", paramType = "query", dataTypeClass = AlertRecordQueryDto.class),
    })
    @PostMapping
    public BaseResult<PageInfo<AlertDTO>> getAlertsRecord(@PathVariable(value = "clusterId") String clusterId,
                                                          @PathVariable(value = "namespace") String namespace,
                                                          @PathVariable(value = "middlewareName") String middlewareName,
                                                          @RequestBody AlertRecordQueryDto alertRecordQueryDto) {
        if ("*".equals(namespace)) {
            namespace = "";
        }
        if (alertRecordQueryDto.getCurrent() == null){
            alertRecordQueryDto.setCurrent(1);
        }
        if (alertRecordQueryDto.getSize() == null){
            alertRecordQueryDto.setSize(10);
        }
        return BaseResult.ok(middlewareAlertRecordService.list(clusterId, namespace, middlewareName, alertRecordQueryDto));
    }

}
