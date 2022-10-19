package com.harmonycloud.zeus.controller.dashboard;

import com.dtflys.forest.annotation.Delete;
import com.dtflys.forest.annotation.Put;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.dashboard.*;
import com.harmonycloud.zeus.service.dashboard.PostgresqlDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/10/10 3:29 下午
 */
@Api(tags = {"服务列表", "服务管理"}, value = "mysql中间件", description = "mysql中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/postgresql/{name}")
public class PostgresqlDashboardController {

    @Autowired
    private PostgresqlDashboardService postgresqlDashboardService;

    @ApiOperation(value = "获取database列表", notes = "获取database列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/databases")
    public BaseResult<List<DatabaseDto>> listDatabase(@PathVariable("clusterId") String clusterId,
                                                      @PathVariable("namespace") String namespace,
                                                      @PathVariable("name") String name) {
        return BaseResult.ok(postgresqlDashboardService.listDatabases(clusterId, namespace, name));
    }

    @ApiOperation(value = "创建database", notes = "创建database")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @PostMapping("/databases")
    public BaseResult addDatabase(@PathVariable("clusterId") String clusterId,
                                                     @PathVariable("namespace") String namespace,
                                                     @PathVariable("name") String name,
                                                     @RequestBody DatabaseDto databaseDto) {
        postgresqlDashboardService.addDatabase(clusterId, namespace, name, databaseDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除database", notes = "删除database")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
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
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/database/{databaseName}/schemas")
    public BaseResult<List<SchemaDto>> listSchema(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("name") String name,
                                 @PathVariable("databaseName") String databaseName) {
        return BaseResult.ok(postgresqlDashboardService.listSchemas(clusterId, namespace, name, databaseName));
    }

    @ApiOperation(value = "获取schema", notes = "获取schema")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/database/{databaseName}/schemas/{schema}")
    public BaseResult<SchemaDto> getSchema(@PathVariable("clusterId") String clusterId,
                                           @PathVariable("namespace") String namespace,
                                           @PathVariable("name") String name,
                                           @PathVariable("databaseName") String databaseName,
                                           @PathVariable("schema") String schema) {
        return BaseResult.ok(postgresqlDashboardService.getSchema(clusterId, namespace, name, databaseName, schema));
    }

    @ApiOperation(value = "创建schema", notes = "创建schema")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @PostMapping("/database/{databaseName}/schemas")
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
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @PutMapping("/database/{databaseName}/schemas/{schema}")
    public BaseResult updateSchema(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("name") String name,
                                   @PathVariable("databaseName") String databaseName,
                                   @PathVariable("schema") String schema,
                                   @RequestBody SchemaDto schemaDto) {
        schemaDto.setDatabaseName(databaseName);
        schemaDto.setSchemaName(schema);
        postgresqlDashboardService.updateSchema(clusterId, namespace, name, schemaDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除schema", notes = "删除schema")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @DeleteMapping("/database/{databaseName}/schemas/{schemaName}")
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
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/database/{database}/schemas/{schema}/tables")
    public BaseResult listTables(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("name") String name,
                                 @PathVariable("database") String database,
                                 @PathVariable("schema") String schema) {
        return BaseResult.ok(postgresqlDashboardService.listTables(clusterId, namespace, name, database, schema));
    }

    @ApiOperation(value = "创建table", notes = "创建table")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @PostMapping("/database/{database}/schemas/{schema}/tables")
    public BaseResult addTable(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("name") String name,
                               @PathVariable("database") String database,
                               @PathVariable("schema") String schema,
                               @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(database);
        tableDto.setSchemaName(schema);
        postgresqlDashboardService.addTable(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除table", notes = "删除table")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping("/database/{database}/schemas/{schema}/tables/{table}")
    public BaseResult dropTable(@PathVariable("clusterId") String clusterId,
                                @PathVariable("namespace") String namespace,
                                @PathVariable("name") String name,
                                @PathVariable("database") String database,
                                @PathVariable("schema") String schema,
                                @PathVariable("table") String table) {
        postgresqlDashboardService.dropTable(clusterId, namespace, name, database, schema, table);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取column列表", notes = "获取column列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/database/{database}/schemas/{schema}/tables/{table}")
    public BaseResult listColumns(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("name") String name,
                                  @PathVariable("database") String database,
                                  @PathVariable("schema") String schema,
                                  @PathVariable("table") String table) {
        return BaseResult.ok(postgresqlDashboardService.listColumns(clusterId, namespace, name, database, schema, table));
    }

    //TODO update table

    // 约束增删
    @ApiOperation(value = "增删外键约束", notes = "增删外键约束")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/database/{database}/schemas/{schema}/tables/{table}/foreign")
    public BaseResult updateForeignKey(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @PathVariable("name") String name,
                                    @PathVariable("database") String database,
                                    @PathVariable("schema") String schema,
                                    @PathVariable("table") String table,
                                    @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(database);
        tableDto.setSchemaName(schema);
        tableDto.setTableName(table);
        postgresqlDashboardService.updateForeignKey(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "增删排它约束", notes = "增删排它约束")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/database/{database}/schemas/{schema}/tables/{table}/exclusion")
    public BaseResult updateExclusion(@PathVariable("clusterId") String clusterId,
                                       @PathVariable("namespace") String namespace,
                                       @PathVariable("name") String name,
                                       @PathVariable("database") String database,
                                       @PathVariable("schema") String schema,
                                       @PathVariable("table") String table,
                                       @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(database);
        tableDto.setSchemaName(schema);
        tableDto.setTableName(table);
        postgresqlDashboardService.updateForeignKey(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "增删唯一约束", notes = "增删唯一约束")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/database/{database}/schemas/{schema}/tables/{table}/unique")
    public BaseResult updateUnique(@PathVariable("clusterId") String clusterId,
                                      @PathVariable("namespace") String namespace,
                                      @PathVariable("name") String name,
                                      @PathVariable("database") String database,
                                      @PathVariable("schema") String schema,
                                      @PathVariable("table") String table,
                                      @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(database);
        tableDto.setSchemaName(schema);
        tableDto.setTableName(table);
        postgresqlDashboardService.updateForeignKey(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "增删检查约束", notes = "增删检查约束")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/database/{database}/schemas/{schema}/tables/{table}/check")
    public BaseResult updateCheck(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("name") String name,
                                   @PathVariable("database") String database,
                                   @PathVariable("schema") String schema,
                                   @PathVariable("table") String table,
                                   @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(database);
        tableDto.setSchemaName(schema);
        tableDto.setTableName(table);
        postgresqlDashboardService.updateForeignKey(clusterId, namespace, name, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "增删继承关系", notes = "增删继承关系")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/database/{database}/schemas/{schema}/tables/{table}/inherit")
    public BaseResult updateInherit(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("name") String name,
                                  @PathVariable("database") String database,
                                  @PathVariable("schema") String schema,
                                  @PathVariable("table") String table,
                                  @RequestBody TableDto tableDto) {
        tableDto.setDatabaseName(database);
        tableDto.setSchemaName(schema);
        tableDto.setTableName(table);
        postgresqlDashboardService.updateForeignKey(clusterId, namespace, name, tableDto);
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
    @GetMapping("/user")
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
    @PostMapping("/user")
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
    @DeleteMapping("/user/{username}")
    public BaseResult dropUser(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("name") String name,
                               @PathVariable("username") String username) {
        postgresqlDashboardService.dropUser(clusterId, namespace, name, username);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询用户权限", notes = "查询用户权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/user/{username}/authority")
    public BaseResult<List<MiddlewareUserAuthority>> userAuthority(@PathVariable("clusterId") String clusterId,
                                                                   @PathVariable("namespace") String namespace,
                                                                   @PathVariable("name") String name,
                                                                   @PathVariable("username") String username) {
        return BaseResult.ok(postgresqlDashboardService.userAuthority(clusterId, namespace, name, username));
    }



}
