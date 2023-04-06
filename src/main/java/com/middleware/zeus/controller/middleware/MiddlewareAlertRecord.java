package com.middleware.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.zeus.bean.BeanAlertRecord;
import com.middleware.zeus.service.middleware.MiddlewareAlertRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/4/6 2:46 下午
 */
@Api(tags = {"监控告警","服务告警"}, value = "服务告警", description = "服务告警")
@RestController
@RequestMapping(value = {"/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/records"})
public class MiddlewareAlertRecord {

    @Autowired
    private MiddlewareAlertRecordService middlewareAlertRecordService;

    @ApiOperation(value = "查询服务告警记录", notes = "查询服务告警记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "当前页", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "size", value = "每页记录数", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "level", value = "告警级别(info:一般，warning:次要,critical：重要)", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "normalTimeOrder", value = "按时间正序", paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping()
    public BaseResult<List<BeanAlertRecord>> getAlertsRecord(@PathVariable(value = "clusterId") String clusterId,
                                                             @PathVariable(value = "namespace") String namespace,
                                                             @PathVariable(value = "middlewareName") String middlewareName,
                                                             @RequestParam(value = "current", required = false, defaultValue = "1") Integer current,
                                                             @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
                                                             @RequestParam(value = "keyword", required = false) String keyword,
                                                             @RequestParam(value = "level", required = false, defaultValue = "") String level,
                                                             @RequestParam(value = "normalTimeOrder", required = false, defaultValue = "true") Boolean normalTimeOrder) {
        if ("*".equals(namespace)) {
            namespace = "";
        }
        return BaseResult.ok(middlewareAlertRecordService.list(clusterId, namespace, middlewareName, current, size, keyword, level, normalTimeOrder));
    }

}
