package com.harmonycloud.zeus.controller.dashboard;

import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.SqlRecordQueryDto;
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
            @ApiImplicitParam(name = "sqlRecordQueryDto", value = "查询条件", paramType = "query", dataTypeClass = SqlRecordQueryDto.class),
    })
    @GetMapping("/databases/{database}/history")
    public BaseResult<PageInfo<BeanSqlExecuteRecord>> listExecuteSql(@PathVariable("clusterId") String clusterId,
                                                                     @PathVariable("namespace") String namespace,
                                                                     @PathVariable("middlewareName") String middlewareName,
                                                                     @PathVariable("database") String database,
                                                                     @RequestBody SqlRecordQueryDto sqlRecordQueryDto) {
        return BaseResult.ok(executeSqlService.listExecuteSql(clusterId, namespace, middlewareName, database, sqlRecordQueryDto));
    }

}
