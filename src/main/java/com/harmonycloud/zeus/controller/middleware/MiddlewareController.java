package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MonitorDto;
import com.harmonycloud.caas.common.model.middleware.MysqlSlowSqlDTO;
import com.harmonycloud.caas.common.model.middleware.SlowLogQuery;
import com.harmonycloud.tool.page.PageObject;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    })
    @GetMapping
    public BaseResult list(@PathVariable("clusterId") String clusterId,
                           @PathVariable("namespace") String namespace,
                           @RequestParam(value = "type", required = false) String type,
                           @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(middlewareService.listAllMiddleware(clusterId, namespace, type, keyword));
    }

    @ApiOperation(value = "查询中间件详情", notes = "查询中间件详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{middlewareName}")
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
    public BaseResult create(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestBody Middleware middleware) {
        middleware.setClusterId(clusterId).setNamespace(namespace);
        middlewareService.create(middleware);
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
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String name,
                             @RequestParam("type") String type) {
        middlewareService.delete(clusterId, namespace, name, type);
        return BaseResult.ok();
    }

    @ApiOperation(value = "中间件切换", notes = "中间件切换")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "isAuto", value = "true开启/false关闭自动切换，不传/传null都为手动切换", paramType = "query", dataTypeClass = Boolean.class),
    })
    @PutMapping("/{middlewareName}/switch")
    public BaseResult switchMiddleware(@PathVariable("clusterId") String clusterId,
                                       @PathVariable("namespace") String namespace,
                                       @PathVariable("middlewareName") String name,
                                       @RequestParam("type") String type,
                                       @RequestParam(value = "isAuto", required = false) Boolean isAuto) {
        middlewareService.switchMiddleware(clusterId, namespace, name, type, isAuto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "性能监控", notes = "性能监控")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("{middlewareName}/monitor")
    public BaseResult<MonitorDto> monitor(@PathVariable("clusterId") String clusterId,
                                          @PathVariable("namespace") String namespace,
                                          @PathVariable("middlewareName") String name,
                                          @RequestParam("type") String type,
                                          @RequestParam("chartVersion") String chartVersion) {
        return BaseResult.ok(middlewareService.monitor(clusterId, namespace, name, type, chartVersion));
    }

    @ApiOperation(value = "慢日志查询", notes = "慢日志查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startTime", value = "起始时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endTime", value = "结束时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "fromQueryTime", value = "查询时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "toQueryTime", value = "查询时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "当前页", paramType = "query", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "searchType", value = "搜索类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "searchWord", value = "关键词", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "size", value = "每页记录数", paramType = "query", dataTypeClass = Long.class),
    })

    @GetMapping("{middlewareName}/slowsql")
    public BaseResult slowsql(@PathVariable("clusterId") String clusterId,
                              @PathVariable("namespace") String namespace,
                              @PathVariable("middlewareName") String middlewareName,
                              @RequestParam("startTime") String startTime,
                              @RequestParam("endTime") String endTime,
                              @RequestParam(value = "fromQueryTime", required = false) String fromQueryTime,
                              @RequestParam(value = "toQueryTime", required = false) String toQueryTime,
                              @RequestParam(value = "current", required = false, defaultValue = "1") Integer current,
                              @RequestParam(value = "searchType", required = false) String searchType,
                              @RequestParam(value = "searchWord", required = false) String searchWord,
                              @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) throws Exception {
        SlowLogQuery slowLogQuery = new SlowLogQuery();
        slowLogQuery.setStartTime(startTime);
        slowLogQuery.setEndTime(endTime);
        slowLogQuery.setCurrent(current);
        slowLogQuery.setSize(size);
        slowLogQuery.setClusterId(clusterId);
        slowLogQuery.setNamespace(namespace);
        slowLogQuery.setMiddlewareName(middlewareName);
        slowLogQuery.setFromQueryTime(fromQueryTime);
        slowLogQuery.setToQueryTime(toQueryTime);
        slowLogQuery.setSearchWord(searchWord);
        slowLogQuery.setSearchType(searchType);
        PageObject<MysqlSlowSqlDTO> slowsql = middlewareService.slowsql(slowLogQuery);
        return BaseResult.ok(slowsql.getData(), slowsql.getCount());
    }

    @ApiOperation(value = "慢日志导出", notes = "慢日志导出")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startTime", value = "起始时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endTime", value = "结束时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "searchType", value = "搜索类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "searchWord", value = "关键词", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("{middlewareName}/slowsql/file")
    public void slowsqlExcel(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String middlewareName,
                             @RequestParam("startTime") String startTime,
                             @RequestParam("endTime") String endTime,
                             @RequestParam(value = "searchType", required = false) String searchType,
                             @RequestParam(value = "searchWord", required = false) String searchWord,
                             HttpServletRequest request, HttpServletResponse response) throws Exception {
        SlowLogQuery slowLogQuery = new SlowLogQuery();
        slowLogQuery.setStartTime(startTime);
        slowLogQuery.setEndTime(endTime);
        slowLogQuery.setClusterId(clusterId);
        slowLogQuery.setNamespace(namespace);
        slowLogQuery.setMiddlewareName(middlewareName);
        slowLogQuery.setSearchWord(searchWord);
        slowLogQuery.setSearchType(searchType);
        middlewareService.slowsqlExcel(slowLogQuery, response, request);
    }

    @ApiOperation(value = "查询服务版本", notes = "查询服务版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("{middlewareName}/version")
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
    public BaseResult upgradeChart(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("middlewareName") String middlewareName,
                                   @RequestParam("type") String type,
                                   @RequestParam("chartName") String chartName,
                                   @RequestParam("upgradeChartVersion") String upgradeChartVersion) {
        middlewareService.upgradeChart(clusterId, namespace, middlewareName, type, chartName,upgradeChartVersion);
        return BaseResult.ok();
    }


}
