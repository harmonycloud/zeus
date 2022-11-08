package com.harmonycloud.zeus.controller.dashboard;

import com.alibaba.fastjson.JSONArray;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.dashboard.ExecuteSqlDto;
import com.harmonycloud.caas.common.model.dashboard.mysql.*;
import com.harmonycloud.zeus.service.dashboard.MysqlDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author liyinlong
 * @Date 2022/10/10 3:29 下午
 */
@Api(tags = {"服务列表", "Mysql管理面板"}, value = "mysql中间件")
@RestController
@RequestMapping(path = {"/clusters/{clusterId}/namespaces/{namespace}/mysql/{middlewareName}", "/dashboard/mysql"})
public class MysqlDashboardController {

    @Autowired
    private MysqlDashboardService mysqlDashboardService;

    @Value("${system.mysql.defaultPassword:zeus123.com}")
    private String defaultPassword;

    @ApiOperation(value = "获取数据类型列表", notes = "获取数据类型列表")
    @GetMapping("/datatype")
    public BaseResult<List<MysqlDataType>> listDataType() {
        return BaseResult.ok(mysqlDashboardService.listDataType());
    }

    @ApiOperation(value = "获取字符集列表", notes = "获取字符集列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/charsets")
    public BaseResult<List<String>> listChartset(@PathVariable("clusterId") String clusterId,
                                                 @PathVariable("namespace") String namespace,
                                                 @PathVariable("middlewareName") String middlewareName) {
        return BaseResult.ok(mysqlDashboardService.listCharset(clusterId, namespace, middlewareName));
    }

    @ApiOperation(value = "获取字符集排序规则", notes = "获取字符集排序规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "charset", value = "字符集", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/charsets/{charset}/collations")
    public BaseResult<List<String>> listChartsetCollations(@PathVariable("clusterId") String clusterId,
                                                           @PathVariable("namespace") String namespace,
                                                           @PathVariable("middlewareName") String middlewareName,
                                                           @PathVariable("charset") String charset) {
        return BaseResult.ok(mysqlDashboardService.listCharsetCollation(clusterId, namespace, middlewareName, charset));
    }

    // database
    @ApiOperation(value = "获取database列表", notes = "获取database列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases")
    public BaseResult<List<DatabaseDto>> listDatabase(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("middlewareName") String middlewareName) {
        return BaseResult.ok(mysqlDashboardService.listDatabases(clusterId, namespace, middlewareName));
    }

    @ApiOperation(value = "创建database", notes = "创建database")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseDto", value = "数据库信息", paramType = "query", dataTypeClass = DatabaseDto.class),
    })
    @PostMapping("/databases")
    public BaseResult createDatabase(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @PathVariable("middlewareName") String middlewareName,
                                     @RequestBody DatabaseDto databaseDto) {
        mysqlDashboardService.createDatabase(clusterId, namespace, middlewareName, databaseDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改database", notes = "修改database")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "databaseDto", value = "数据库信息", paramType = "query", dataTypeClass = DatabaseDto.class),
    })
    @PutMapping("/databases")
    public BaseResult updateDatabase(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @PathVariable("middlewareName") String middlewareName,
                                     @RequestBody DatabaseDto databaseDto) {
        mysqlDashboardService.updateDatabase(clusterId, namespace, middlewareName, databaseDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除database", notes = "删除database")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/databases/{database}")
    public BaseResult deleteDatabase(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @PathVariable("middlewareName") String middlewareName,
                                     @PathVariable("database") String database) {
        mysqlDashboardService.dropDatabase(clusterId, namespace, middlewareName, database);
        return BaseResult.ok();
    }

    // tables
    @ApiOperation(value = "查询数据表引擎", notes = "查询数据表引擎")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/engines")
    public BaseResult<List<MysqlEngineDto>> listEngines(@PathVariable("clusterId") String clusterId,
                                                @PathVariable("namespace") String namespace,
                                                @PathVariable("middlewareName") String middlewareName) {
        return BaseResult.ok(mysqlDashboardService.listEngines(clusterId, namespace, middlewareName));
    }

    @ApiOperation(value = "获取数据库table列表", notes = "获取table列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/tables")
    public BaseResult<List<TableDto>> listTables(@PathVariable("clusterId") String clusterId,
                                                 @PathVariable("namespace") String namespace,
                                                 @PathVariable("middlewareName") String middlewareName,
                                                 @PathVariable("database") String database) {
        return BaseResult.ok(mysqlDashboardService.listTables(clusterId, namespace, middlewareName, database));
    }

    @ApiOperation(value = "获取table详情", notes = "获取table详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/tables/{table}")
    public BaseResult<TableDto> showTableDetail(@PathVariable("clusterId") String clusterId,
                                      @PathVariable("namespace") String namespace,
                                      @PathVariable("middlewareName") String middlewareName,
                                      @PathVariable("database") String database,
                                      @PathVariable("table") String table) {
        return BaseResult.ok(mysqlDashboardService.showTableDetail(clusterId, namespace, middlewareName, database, table));
    }

    @ApiOperation(value = "获取table数据", notes = "获取table数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "queryInfo", value = "查询信息", paramType = "query", dataTypeClass = QueryInfo.class),
    })
    @PostMapping("/databases/{database}/tables/{table}/data")
    public BaseResult<JSONArray> showTableData(@PathVariable("clusterId") String clusterId,
                                               @PathVariable("namespace") String namespace,
                                               @PathVariable("middlewareName") String middlewareName,
                                               @PathVariable("database") String database,
                                               @PathVariable("table") String table,
                                               @RequestBody QueryInfo queryInfo) {
        return BaseResult.ok(mysqlDashboardService.showTableData(clusterId, namespace, middlewareName, database, table, queryInfo));
    }

    @ApiOperation(value = "创建table", notes = "创建table")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库信息", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableDto", value = "表信息", paramType = "query", dataTypeClass = DatabaseDto.class),
    })
    @PostMapping("/databases/{database}/tables")
    public BaseResult createDatabase(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @PathVariable("middlewareName") String middlewareName,
                                     @PathVariable("database") String database,
                                     @RequestBody TableDto tableDto) {
        mysqlDashboardService.createTable(clusterId, namespace, middlewareName, database, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改表信息", notes = "修改表信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableDto", value = "表信息", paramType = "query", dataTypeClass = TableDto.class),
    })
    @PutMapping("/databases/{database}/tabes/{table}")
    public BaseResult updateTableName(@PathVariable("clusterId") String clusterId,
                                      @PathVariable("namespace") String namespace,
                                      @PathVariable("middlewareName") String middlewareName,
                                      @PathVariable("database") String database,
                                      @PathVariable("table") String table,
                                      @RequestBody TableDto tableDto) {
        mysqlDashboardService.updateTableOptions(clusterId, namespace, middlewareName, database, table, tableDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除table", notes = "删除table")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/databases/{database}/tabes/{table}")
    public BaseResult deleteTable(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("middlewareName") String middlewareName,
                                  @PathVariable("database") String database,
                                  @PathVariable("table") String table) {
        mysqlDashboardService.dropTable(clusterId, namespace, middlewareName, database, table);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取数据表所有列", notes = "获取数据表所有列")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/tables/{table}/columns")
    public BaseResult<List<ColumnDto>> listTableColumns(@PathVariable("clusterId") String clusterId,
                                                        @PathVariable("namespace") String namespace,
                                                        @PathVariable("middlewareName") String middlewareName,
                                                        @PathVariable("database") String database,
                                                        @PathVariable("table") String table) {
        return BaseResult.ok(mysqlDashboardService.listTableColumns(clusterId, namespace, middlewareName, database, table));
    }

    @ApiOperation(value = "保存数据表所有列", notes = "保存数据表所有列")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "indexDtoList", value = "列信息列表", paramType = "query", dataTypeClass = ColumnDto.class),
    })
    @PutMapping("/databases/{database}/tables/{table}/columns")
    public BaseResult saveTableColumns(@PathVariable("clusterId") String clusterId,
                                       @PathVariable("namespace") String namespace,
                                       @PathVariable("middlewareName") String middlewareName,
                                       @PathVariable("database") String database,
                                       @PathVariable("table") String table,
                                       @RequestBody TableDto tableDto) {
        mysqlDashboardService.saveTableColumn(clusterId, namespace, middlewareName, database, table, tableDto.getColumns());
        return BaseResult.ok();
    }

    // index
    @ApiOperation(value = "获取数据表所有索引", notes = "获取数据表所有索引")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/tables/{table}/indices")
    public BaseResult<List<IndexDto>> listTableIndices(@PathVariable("clusterId") String clusterId,
                                                       @PathVariable("namespace") String namespace,
                                                       @PathVariable("middlewareName") String middlewareName,
                                                       @PathVariable("database") String database,
                                                       @PathVariable("table") String table) {
        return BaseResult.ok(mysqlDashboardService.listTableIndices(clusterId, namespace, middlewareName, database, table));
    }

    @ApiOperation(value = "保存数据表所有索引", notes = "保存数据表所有索引")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "indexDtoList", value = "索引信息列表", paramType = "query", dataTypeClass = IndexDto.class),
    })
    @PutMapping("/databases/{database}/tables/{table}/indices")
    public BaseResult saveTableForeignKeys(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @PathVariable("middlewareName") String middlewareName,
                                     @PathVariable("database") String database,
                                     @PathVariable("table") String table,
                                     @RequestBody TableDto tableDto) {
        mysqlDashboardService.saveTableIndex(clusterId, namespace, middlewareName, database, table, tableDto.getIndices());
        return BaseResult.ok();
    }

    // foreignkeys
    @ApiOperation(value = "获取数据表所有外键", notes = "获取数据表所有外键")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/tables/{table}/foreignKeys")
    public BaseResult<List<ForeignKeyDto>> listTableForeignKeys(@PathVariable("clusterId") String clusterId,
                                                                @PathVariable("namespace") String namespace,
                                                                @PathVariable("middlewareName") String middlewareName,
                                                                @PathVariable("database") String database,
                                                                @PathVariable("table") String table) {
        return BaseResult.ok(mysqlDashboardService.listTableForeignKeys(clusterId, namespace, middlewareName, database, table));
    }

    @ApiOperation(value = "保存数据表所有外键", notes = "保存数据表所有外键")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "foreignKeyDtos", value = "外键信息列表", paramType = "query", dataTypeClass = List.class),
    })
    @PutMapping("/databases/{database}/tables/{table}/foreignKeys")
    public BaseResult saveTableIndex(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @PathVariable("middlewareName") String middlewareName,
                                     @PathVariable("database") String database,
                                     @PathVariable("table") String table,
                                     @RequestBody TableDto tableDto) {
        mysqlDashboardService.saveTableForeignKey(clusterId, namespace, middlewareName, database, table, tableDto.getForeignKeys());
        return BaseResult.ok();
    }

    // 用户
    @ApiOperation(value = "获取用户列表", notes = "获取用户列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "搜索关键词", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/users")
    public BaseResult<List<UserDto>> listUser(@PathVariable("clusterId") String clusterId,
                                           @PathVariable("namespace") String namespace,
                                           @PathVariable("middlewareName") String middlewareName,
                                           @RequestParam(value = "keyword", defaultValue = "") String keyword) {
        return BaseResult.ok(mysqlDashboardService.listUser(clusterId, namespace, middlewareName, keyword));
    }

    @ApiOperation(value = "创建用户", notes = "创建用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "userDto", value = "用户对象", paramType = "query", dataTypeClass = UserDto.class),
    })
    @PostMapping("/users")
    public BaseResult addUser(@PathVariable("clusterId") String clusterId,
                              @PathVariable("namespace") String namespace,
                              @PathVariable("middlewareName") String middlewareName,
                              @RequestBody UserDto userDto) {
        mysqlDashboardService.addUser(clusterId, namespace, middlewareName, userDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除用户", notes = "删除用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/users/{username}")
    public BaseResult dropUser(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("middlewareName") String middlewareName,
                               @PathVariable("username") String username) {
        mysqlDashboardService.dropUser(clusterId, namespace, middlewareName, username);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新用户", notes = "更新用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "userDto", value = "用户对象", paramType = "query", dataTypeClass = UserDto.class),
    })
    @PutMapping("/users/{username}")
    public BaseResult updateUser(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("middlewareName") String middlewareName,
                                 @PathVariable("username") String username,
                                 @RequestBody UserDto userDto) {
        mysqlDashboardService.updateUser(clusterId, namespace, middlewareName, username, userDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "重置密码", notes = "重置密码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "userDto", value = "用户对象", paramType = "query", dataTypeClass = UserDto.class),
    })
    @PutMapping("/users/{username}/password")
    public BaseResult resetPassword(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @PathVariable("middlewareName") String middlewareName,
                                    @PathVariable("username") String username) {
        UserDto userDto = new UserDto();
        userDto.setPassword(defaultPassword);
        mysqlDashboardService.updatePassword(clusterId, namespace, middlewareName, username, userDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "锁定用户", notes = "锁定用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
    })
    @PutMapping("/users/{username}/lock")
    public BaseResult lockUser(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("middlewareName") String middlewareName,
                               @PathVariable("username") String username) {
        mysqlDashboardService.lockUser(clusterId, namespace, middlewareName, username);
        return BaseResult.ok();
    }

    @ApiOperation(value = "解锁用户", notes = "解锁用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
    })
    @PutMapping("/users/{username}/unlock")
    public BaseResult unlockUser(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("middlewareName") String middlewareName,
                               @PathVariable("username") String username) {
        mysqlDashboardService.unLockUser(clusterId, namespace, middlewareName, username);
        return BaseResult.ok();
    }

    @ApiOperation(value = "授权数据库", notes = "授权数据库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "grantOptionDto", value = "授权信息", paramType = "query", dataTypeClass = GrantOptionDto.class),
    })
    @PutMapping("/databases/{database}/privilege")
    public BaseResult grantDatabasePrivilege(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @PathVariable("middlewareName") String middlewareName,
                                    @PathVariable("database") String database,
                                    @RequestBody GrantOptionDto grantOptionDto) {
        mysqlDashboardService.grantDatabasePrivilege(clusterId, namespace, middlewareName, database, grantOptionDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "授权数据表", notes = "授权数据表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "grantOptionDto", value = "授权信息", paramType = "query", dataTypeClass = GrantOptionDto.class),
    })
    @PutMapping("/databases/{database}/tables/{table}/privilege")
    public BaseResult grantTablePrivilege(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("middlewareName") String middlewareName,
                                 @PathVariable("database") String database,
                                 @PathVariable("table") String table,
                                 @RequestBody GrantOptionDto grantOptionDto) {
        mysqlDashboardService.grantTablePrivilege(clusterId, namespace, middlewareName, database, table, grantOptionDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "释放权限", notes = "释放权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "grantOptionDtos", value = "授权信息", paramType = "query", dataTypeClass = GrantOptionDto.class),
    })
    @DeleteMapping("/users/{username}/privilege")
    public BaseResult revokePrivilege(@PathVariable("clusterId") String clusterId,
                                      @PathVariable("namespace") String namespace,
                                      @PathVariable("middlewareName") String middlewareName,
                                      @PathVariable("username") String username,
                                      @RequestBody List<GrantOptionDto> grantOptionDtos) {
        mysqlDashboardService.revokePrivilege(clusterId, namespace, middlewareName, username, grantOptionDtos);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询用户权限", notes = "查询用户权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/users/{username}/authority")
    public BaseResult<List<GrantOptionDto>> userAuthority(@PathVariable("clusterId") String clusterId,
                                                  @PathVariable("namespace") String namespace,
                                                  @PathVariable("middlewareName") String middlewareName,
                                                  @PathVariable("username") String username) {
        return BaseResult.ok(mysqlDashboardService.listUserAuthority(clusterId, namespace, middlewareName, username));
    }

    @ApiOperation(value = "导出表sql", notes = "导出表sql")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping(value = "/databases/{database}/tables/{table}/scriptFile", produces = "application/octet-stream")
    public byte[] exportTableSql(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("middlewareName") String middlewareName,
                                 @PathVariable("database") String database,
                                 @PathVariable("table") String table,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        return mysqlDashboardService.exportTableSql(clusterId, namespace, middlewareName, database, table, request, response);
    }

    @ApiOperation(value = "导出表结构Excel", notes = "导出表结构Excel")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/tables/{table}/excelFile")
    public void exportTableExcel(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("middlewareName") String middlewareName,
                                 @PathVariable("database") String database,
                                 @PathVariable("table") String table,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        mysqlDashboardService.exportTableExcel(clusterId, namespace, middlewareName, database, table, request, response);
    }

    @ApiOperation(value = "导出库sql", notes = "导出库sql")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/scriptFile")
    public void exportDatabaseSql(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("middlewareName") String middlewareName,
                                  @PathVariable("database") String database,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        mysqlDashboardService.exportDatabaseSql(clusterId, namespace, middlewareName, database, request, response);
    }

    @ApiOperation(value = "导出库所有表结构Excel", notes = "导出库所有表结构Excel")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "table", value = "表名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/excelFile")
    public void exportDatabaseExcel(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("middlewareName") String middlewareName,
                                 @PathVariable("database") String database,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        mysqlDashboardService.exportDatabaseExcel(clusterId, namespace, middlewareName, database, request, response);
    }

    @ApiOperation(value = "执行sql", notes = "执行sql")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "sql", value = "sql语句", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("/databases/{database}/query")
    public BaseResult<ExecuteSqlDto> execQuery(@PathVariable("clusterId") String clusterId,
                                               @PathVariable("namespace") String namespace,
                                               @PathVariable("middlewareName") String middlewareName,
                                               @PathVariable("database") String database,
                                               @RequestParam("sql") String sql) {

        return BaseResult.ok();
    }

}
