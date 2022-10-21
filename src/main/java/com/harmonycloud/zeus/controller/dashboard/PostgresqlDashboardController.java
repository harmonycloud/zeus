package com.harmonycloud.zeus.controller.dashboard;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.dashboard.*;
import com.harmonycloud.zeus.service.dashboard.PostgresqlDashboardService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @Date 2022/10/10 3:29 下午
 */
@Api(tags = {"中间件面板", "postgresql面板"}, value = "postgresql面板", description = "postgresql面板")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/postgresql/{name}")
public class PostgresqlDashboardController {

    @Autowired
    private PostgresqlDashboardService postgresqlDashboardService;

    @ApiOperation(value = "获取database列表", notes = "获取database列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases")
    public BaseResult<List<DatabaseDto>> listDatabase(@PathVariable("clusterId") String clusterId,
                                                      @PathVariable("namespace") String namespace,
                                                      @PathVariable("name") String name) {
        return BaseResult.ok(postgresqlDashboardService.listDatabases(clusterId, namespace, name));
    }

    @ApiOperation(value = "获取database列表", notes = "获取database列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{databaseName}")
    public BaseResult<List<DatabaseDto>> getDatabase(@PathVariable("clusterId") String clusterId,
                                                     @PathVariable("namespace") String namespace,
                                                     @PathVariable("name") String name,
                                                     @RequestParam("databaseName") String databaseName) {
        return BaseResult.ok(postgresqlDashboardService.getDatabase(clusterId, namespace, name, databaseName));
    }

    @ApiOperation(value = "创建database", notes = "创建database")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseDto", value = "库对象", paramType = "query", dataTypeClass = DatabaseDto.class),
    })
    @PostMapping("/databases")
    public BaseResult addDatabase(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("name") String name,
                                  @RequestBody DatabaseDto databaseDto) {
        postgresqlDashboardService.addDatabase(clusterId, namespace, name, databaseDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新database", notes = "更新database")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseDto", value = "库对象", paramType = "query", dataTypeClass = DatabaseDto.class),
    })
    @PutMapping("/databases/{databaseName}")
    public BaseResult updateDatabase(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @PathVariable("name") String name,
                                     @PathVariable("databaseName") String databaseName,
                                     @RequestBody DatabaseDto databaseDto) {
        postgresqlDashboardService.updateDatabase(clusterId, namespace, name, databaseName, databaseDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除database", notes = "删除database")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/databases/{databaseName}")
    public BaseResult<List<DatabaseDto>> deleteDatabase(@PathVariable("clusterId") String clusterId,
                                                        @PathVariable("namespace") String namespace,
                                                        @PathVariable("name") String name,
                                                        @PathVariable("databaseName") String databaseName) {
        postgresqlDashboardService.deleteDatabase(clusterId, namespace, name, databaseName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取schema列表", notes = "获取schema列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{databaseName}/schemas")
    public BaseResult<List<SchemaDto>> listSchema(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("name") String name,
                                 @PathVariable("databaseName") String databaseName) {
        return BaseResult.ok(postgresqlDashboardService.listSchemas(clusterId, namespace, name, databaseName));
    }

    @ApiOperation(value = "获取schema", notes = "获取schema")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{databaseName}/schemas/{schemaName}")
    public BaseResult<SchemaDto> getSchema(@PathVariable("clusterId") String clusterId,
                                           @PathVariable("namespace") String namespace,
                                           @PathVariable("name") String name,
                                           @PathVariable("databaseName") String databaseName,
                                           @PathVariable("schemaName") String schemaName) {
        return BaseResult.ok(postgresqlDashboardService.getSchema(clusterId, namespace, name, databaseName, schemaName));
    }

    @ApiOperation(value = "创建schema", notes = "创建schema")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaDto", value = "模式对象", paramType = "query", dataTypeClass = SchemaDto.class),
    })
    @PostMapping("/databases/{databaseName}/schemas")
    public BaseResult addSchema(@PathVariable("clusterId") String clusterId,
                                @PathVariable("namespace") String namespace,
                                @PathVariable("name") String name,
                                @PathVariable("databaseName") String databaseName,
                                @RequestBody SchemaDto schemaDto) {
        schemaDto.setDatabaseName(databaseName);
        postgresqlDashboardService.addSchema(clusterId, namespace, name, schemaDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新schema", notes = "更新schema")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaDto", value = "模式对象", paramType = "query", dataTypeClass = SchemaDto.class),

    })
    @PutMapping("/databases/{databaseName}/schemas/{schemaName}")
    public BaseResult updateSchema(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("name") String name,
                                   @PathVariable("databaseName") String databaseName,
                                   @PathVariable("schemaName") String schemaName,
                                   @RequestBody SchemaDto schemaDto) {
        postgresqlDashboardService.updateSchema(clusterId, namespace, name, databaseName, schemaName, schemaDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除schema", notes = "删除schema")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/databases/{databaseName}/schemas/{schemaName}")
    public BaseResult deleteSchema(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("name") String name,
                                   @PathVariable("databaseName") String databaseName,
                                   @PathVariable("schemaName") String schemaName) {
        postgresqlDashboardService.deleteSchema(clusterId, namespace, name, databaseName, schemaName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取table列表", notes = "获取table列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{databaseName}/schemas/{schemaName}/tables")
    public BaseResult<List<TableDto>> listTables(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("name") String name,
                                 @PathVariable("databaseName") String databaseName,
                                 @PathVariable("schemaName") String schemaName) {
        return BaseResult.ok(postgresqlDashboardService.listTables(clusterId, namespace, name, databaseName, schemaName));
    }

    @ApiOperation(value = "创建table", notes = "创建table")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableDto", value = "表对象", paramType = "path", dataTypeClass = TableDto.class),
    })
    @PostMapping("/databases/{databaseName}/schemas/{schemaName}/tables")
    public BaseResult addTable(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("name") String name,
                               @PathVariable("databaseName") String databaseName,
                               @PathVariable("schemaName") String schemaName,
                               @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(databaseName);
        tableDto.setSchemaName(schemaName);
        postgresqlDashboardService.addTable(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新table", notes = "更新table")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableName", value = "表名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableDto", value = "表对象", paramType = "path", dataTypeClass = TableDto.class),
    })
    @PutMapping("/databases/{databaseName}/schemas/{schemaName}/tables/{tableName}")
    public BaseResult updateTable(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("name") String name,
                               @PathVariable("databaseName") String databaseName,
                               @PathVariable("schemaName") String schemaName,
                               @RequestBody TableDto tableDto) {
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除table", notes = "删除table")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableName", value = "表名称", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/databases/{databaseName}/schemas/{schemaName}/tables/{tableName}")
    public BaseResult dropTable(@PathVariable("clusterId") String clusterId,
                                @PathVariable("namespace") String namespace,
                                @PathVariable("name") String name,
                                @PathVariable("databaseName") String databaseName,
                                @PathVariable("schemaName") String schemaName,
                                @PathVariable("tableName") String tableName) {
        postgresqlDashboardService.dropTable(clusterId, namespace, name, databaseName, schemaName, tableName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取column列表", notes = "获取column列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableName", value = "表名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{databaseName}/schemas/{schemaName}/tables/{tableName}/columns")
    public BaseResult<List<ColumnDto>> listColumns(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("name") String name,
                                  @PathVariable("databaseName") String databaseName,
                                  @PathVariable("schemaName") String schemaName,
                                  @PathVariable("tableName") String tableName) {
        return BaseResult.ok(postgresqlDashboardService.listColumns(clusterId, namespace, name, databaseName, schemaName, tableName));
    }

    @ApiOperation(value = "获取table数据", notes = "获取table数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableName", value = "表名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "当前页", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "size", value = "页大小", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "orderMap", value = "排序", paramType = "query", dataTypeClass = Map.class),
    })
    @GetMapping("/databases/{databaseName}/schemas/{schemaName}/tables/{tableName}/data")
    public BaseResult<List<ColumnDto>> getTableData(@PathVariable("clusterId") String clusterId,
                                                    @PathVariable("namespace") String namespace,
                                                    @PathVariable("name") String name,
                                                    @PathVariable("databaseName") String databaseName,
                                                    @PathVariable("schemaName") String schemaName,
                                                    @PathVariable("tableName") String tableName,
                                                    @RequestParam("current") Integer current,
                                                    @RequestParam("size") Integer size,
                                                    @RequestParam("orderMap") Map<String, String> orderMap) {
        return BaseResult.ok(postgresqlDashboardService.getTableData(clusterId, namespace, name, databaseName, schemaName, tableName, current, size, orderMap));
    }

    // 约束增删
    @ApiOperation(value = "增删外键约束", notes = "增删外键约束")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableName", value = "表名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableDto", value = "表对象", paramType = "path", dataTypeClass = TableDto.class),
    })
    @PutMapping("/databases/{databaseName}/schemas/{schemaName}/tables/{tableName}/foreign")
    public BaseResult updateForeignKey(@PathVariable("clusterId") String clusterId,
                                       @PathVariable("namespace") String namespace,
                                       @PathVariable("name") String name,
                                       @PathVariable("databaseName") String databaseName,
                                       @PathVariable("schemaName") String schemaName,
                                       @PathVariable("tableName") String tableName,
                                       @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(databaseName);
        tableDto.setSchemaName(schemaName);
        tableDto.setTableName(tableName);
        postgresqlDashboardService.updateForeignKey(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "增删排它约束", notes = "增删排它约束")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableName", value = "表名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableDto", value = "表对象", paramType = "path", dataTypeClass = TableDto.class),
    })
    @PutMapping("/databases/{databaseName}/schemas/{schemaName}/tables/{tableName}/exclusion")
    public BaseResult updateExclusion(@PathVariable("clusterId") String clusterId,
                                      @PathVariable("namespace") String namespace,
                                      @PathVariable("name") String name,
                                      @PathVariable("databaseName") String databaseName,
                                      @PathVariable("schemaName") String schemaName,
                                      @PathVariable("tableName") String tableName,
                                      @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(databaseName);
        tableDto.setSchemaName(schemaName);
        tableDto.setTableName(tableName);
        postgresqlDashboardService.updateExclusion(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "增删唯一约束", notes = "增删唯一约束")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableName", value = "表名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableDto", value = "表对象", paramType = "path", dataTypeClass = TableDto.class),
    })
    @PutMapping("/databases/{databaseName}/schemas/{schemaName}/tables/{tableName}/unique")
    public BaseResult updateUnique(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("name") String name,
                                   @PathVariable("databaseName") String databaseName,
                                   @PathVariable("schemaName") String schemaName,
                                   @PathVariable("tableName") String tableName,
                                   @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(databaseName);
        tableDto.setSchemaName(schemaName);
        tableDto.setTableName(tableName);
        postgresqlDashboardService.updateUnique(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "增删检查约束", notes = "增删检查约束")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableName", value = "表名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableDto", value = "表对象", paramType = "path", dataTypeClass = TableDto.class),
    })
    @PutMapping("/databases/{databaseName}/schemas/{schemaName}/tables/{tableName}/check")
    public BaseResult updateCheck(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("name") String name,
                                  @PathVariable("databaseName") String databaseName,
                                  @PathVariable("schemaName") String schemaName,
                                  @PathVariable("tableName") String tableName,
                                  @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(databaseName);
        tableDto.setSchemaName(schemaName);
        tableDto.setTableName(tableName);
        postgresqlDashboardService.updateCheck(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "增删继承关系", notes = "增删继承关系")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseName", value = "库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schemaName", value = "模式名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableName", value = "表名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableDto", value = "表对象", paramType = "path", dataTypeClass = TableDto.class),
    })
    @PutMapping("/databases/{databaseName}/schemas/{schemaName}/tables/{tableName}/inherit")
    public BaseResult updateInherit(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @PathVariable("name") String name,
                                    @PathVariable("databaseName") String databaseName,
                                    @PathVariable("schemaName") String schemaName,
                                    @PathVariable("tableName") String tableName,
                                    @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(databaseName);
        tableDto.setSchemaName(schemaName);
        tableDto.setTableName(tableName);
        postgresqlDashboardService.updateInherit(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    // sql console

    @ApiOperation(value = "获取用户列表", notes = "获取用户列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/users")
    public BaseResult<List<MiddlewareUserDto>> listUser(@PathVariable("clusterId") String clusterId,
                                                        @PathVariable("namespace") String namespace,
                                                        @PathVariable("name") String name,
                                                        @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(postgresqlDashboardService.listUser(clusterId, namespace, name, keyword));
    }

    @ApiOperation(value = "创建用户", notes = "创建用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareUserDto", value = "用户对象", paramType = "query", dataTypeClass = MiddlewareUserDto.class),
    })
    @PostMapping("/users")
    public BaseResult addUser(@PathVariable("clusterId") String clusterId,
                              @PathVariable("namespace") String namespace,
                              @PathVariable("name") String name,
                              @RequestBody MiddlewareUserDto middlewareUserDto) {
        postgresqlDashboardService.addUser(clusterId, namespace, name, middlewareUserDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除用户", notes = "删除用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/users/{username}")
    public BaseResult dropUser(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("name") String name,
                               @PathVariable("username") String username) {
        postgresqlDashboardService.dropUser(clusterId, namespace, name, username);
        return BaseResult.ok();
    }

    @ApiOperation(value = "添加用户权限", notes = "添加用户权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareUserAuthority", value = "用户权限对象", paramType = "query", dataTypeClass = MiddlewareUserAuthority.class),
    })
    @PostMapping("/users/{username}/authority")
    public BaseResult grantUser(@PathVariable("clusterId") String clusterId,
                                @PathVariable("namespace") String namespace,
                                @PathVariable("name") String name,
                                @PathVariable("username") String username,
                                @RequestBody MiddlewareUserAuthority middlewareUserAuthority) {
        postgresqlDashboardService.grantUser(clusterId, namespace, name, username, middlewareUserAuthority);
        return BaseResult.ok();
    }

    @ApiOperation(value = "取消用户权限", notes = "取消用户权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareUserDto", value = "用户权限对象", paramType = "query", dataTypeClass = MiddlewareUserDto.class),
    })
    @DeleteMapping("/users/{username}/authority")
    public BaseResult revokeUser(@PathVariable("clusterId") String clusterId,
                                @PathVariable("namespace") String namespace,
                                @PathVariable("name") String name,
                                @PathVariable("username") String username,
                                @RequestBody MiddlewareUserDto middlewareUserDto) {
        middlewareUserDto.setUsername(username);
        postgresqlDashboardService.revokeUser(clusterId, namespace, name, middlewareUserDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询用户权限", notes = "查询用户权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/users/{username}/authority")
    public BaseResult<List<MiddlewareUserAuthority>> userAuthority(@PathVariable("clusterId") String clusterId,
                                                                   @PathVariable("namespace") String namespace,
                                                                   @PathVariable("name") String name,
                                                                   @PathVariable("username") String username) {
        return BaseResult.ok(postgresqlDashboardService.userAuthority(clusterId, namespace, name, username));
    }

    @ApiOperation(value = "重置密码", notes = "重置密码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
    })
    @PostMapping("/users/{username}/reset")
    public BaseResult resetPassword(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @PathVariable("name") String name,
                                    @PathVariable("username") String username) {
        postgresqlDashboardService.resetPassword(clusterId, namespace, name, username);
        return BaseResult.ok();
    }

    @ApiOperation(value = "启用/禁用用户", notes = "启用/禁用用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "enable", value = "启用/禁用", paramType = "query", dataTypeClass = Boolean.class),
    })
    @PostMapping("/users/{username}/enable")
    public BaseResult enableUser(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @PathVariable("name") String name,
                                    @PathVariable("username") String username,
                                    @RequestParam("enable") Boolean enable) {
        postgresqlDashboardService.enableUser(clusterId, namespace, name, username, enable);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询字符集列表", notes = "查询字符集列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/encoding")
    public BaseResult<List<String>> listEncoding() {
        return BaseResult.ok(postgresqlDashboardService.listEncoding());
    }


}
