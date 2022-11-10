package com.harmonycloud.zeus.controller.dashboard;

import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.dashboard.DatabaseDto;
import com.harmonycloud.caas.common.model.dashboard.ExecuteSqlDto;
import com.harmonycloud.zeus.bean.BeanSqlExecuteRecord;
import com.harmonycloud.zeus.service.dashboard.ExecuteSqlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/11/8 10:10 上午
 */
@Api(tags = {"中间件面板", "sql console"}, value = "sql console", description = "sql console")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/sql")
public class SqlExecuteRecordController {

    @Autowired
    private ExecuteSqlService executeSqlService;

    @ApiOperation(value = "查询执行记录", notes = "查询执行记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", value = "过滤字", required = false, paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "当前页", required = false, paramType = "query", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "size", value = "每页记录数", required = false, paramType = "query", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "size", value = "每页记录数", required = false, paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<PageInfo<ExecuteSqlDto>> listSqlRecord(@RequestParam(value = "keyword", required = false) String keyword,
                                                             @RequestParam(value = "current", required = false, defaultValue = "1") Integer current,
                                                             @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
                                                             @RequestParam(value = "order", required = false) String order) {
        return BaseResult.ok(executeSqlService.list(keyword, current, size, order));
    }

    @ApiOperation(value = "查询sql历史记录", notes = "查询sql历史记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "start", value = "开始时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "end", value = "结束时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageNum", value = "页码", paramType = "query", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "size", value = "每页数量", paramType = "query", dataTypeClass = Integer.class),
    })
    @GetMapping("/databases/{database}/cmd/history")
    public BaseResult<List<BeanSqlExecuteRecord>> listExecuteSql(@PathVariable("clusterId") String clusterId,
                                                                 @PathVariable("namespace") String namespace,
                                                                 @PathVariable("middlewareName") String middlewareName,
                                                                 @PathVariable("database") String database,
                                                                 @RequestParam(value = "keyword", required = false) String keyword,
                                                                 @RequestParam(value = "start", required = false) String start,
                                                                 @RequestParam(value = "end", required = false) String end,
                                                                 @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                                 @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        return BaseResult.ok(executeSqlService.listExecuteSql(clusterId, namespace, middlewareName, database, keyword, start, end, pageNum, size));
    }

}
