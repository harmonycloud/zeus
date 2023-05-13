package com.middleware.zeus.integration.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.middleware.zeus.common.model.dashboard.SqlQuery;
import com.middleware.zeus.interceptor.MiddlewareApiInterceptor;
import com.middleware.zeus.interceptor.MysqlDashboardInterceptor;
import com.middleware.zeus.common.model.dashboard.mysql.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author liyinlong
 * @Date 2022/10/09 3:08 下午
 */
@Component
@Address(source = MiddlewareApiAddress.class)
@BaseRequest(interceptor = {MiddlewareApiInterceptor.class, MysqlDashboardInterceptor.class})
public interface MysqlClient {

    /**
     * 查询database列表
     */
    @Get(url = "/mysql/{path}/port/{port}/databases")
    JSONObject listDatabases(@Var("path") String path, @Var("port") String port);

    /**
     * 创建数据库
     * databaseDto.db: 数据库名称
     * databaseDto.character: 字符集
     * databaseDto.collate 排序规则
     */
    @Post(url = "/mysql/{path}/port/{port}/databases")
    JSONObject createDatabase(@Var("path") String path, @Var("port") String port, @JSONBody DatabaseDto databaseDto);

    /**
     * 修改数据库
     */
    @Put(url = "/mysql/{path}/port/{port}/databases")
    JSONObject alterDatabase(@Var("path") String path, @Var("port") String port, @JSONBody DatabaseDto databaseDto);

    /**
     * 删除数据库
     */
    @Delete(url = "/mysql/{path}/port/{port}/databases/{database}")
    JSONObject dropDatabase(@Var("path") String path, @Var("port") String port, @Var("database") String database);

    /**
     * 查询数据库详情
     */
    @Get(url = "/mysql/{path}/port/{port}/databases/{database}/detail")
    JSONObject showDatabaseDetail(@Var("path") String path, @Var("port") String port, @Var("database") String database);

    /**
     * 查询字符集
     */
    @Get(url = "/mysql/{path}/port/{port}/charsets")
    JSONObject listCharsets(@Var("path") String path, @Var("port") String port);

    /**
     * 查询字符集排序规则
     */
    @Get(url = "/mysql/{path}/port/{port}/charsets/{charset}/collations")
    JSONObject listCharsetCollations(@Var("path") String path, @Var("port") String port, @Var("charset") String charset);

    // tables

    @Get(url = "/mysql/{path}/port/{port}/engines")
    JSONObject listEngines(@Var("path") String path, @Var("port") String port);

    /**
     * 查询指定数据库所有数据表
     */
    @Get(url = "/mysql/{path}/port/{port}/databases/{database}/tables")
    JSONObject listTables(@Var("path") String path, @Var("port") String port, @Var("database") String database);

    /**
     * 创建表
     */
    @Post(url = "/mysql/{path}/port/{port}/databases/{database}/tables")
    JSONObject createTable(@Var("path") String path, @Var("port") String port, @Var("database") String database, @JSONBody TableDto tableDto);

    /**
     * 更新表基本信息
     */
    @Put(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/options")
    JSONObject updateTableOptions(@Var("path") String path,
                                  @Var("port") String port,
                                  @Var("database") String database,
                                  @Var("table") String table,
                                  @JSONBody TableDto tableDto);

    /**
     * 删除表
     */
    @Delete(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}")
    JSONObject dropTable(@Var("path") String path,
                         @Var("port") String port,
                         @Var("database") String database,
                         @Var("table") String table);

    /**
     * 修改表名
     */
    @Put(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}")
    JSONObject renameTable(@Var("path") String path,
                           @Var("port") String port,
                           @Var("database") String database,
                           @Var("table") String table,
                           @JSONBody TableDto tableDto);

    /**
     * 获取表基本信息
     */
    @Get(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/options")
    JSONObject showTableOptions(@Var("path") String path,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("table") String table);

    /**
     * 查询表数据
     */
    @Post(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/data")
    JSONObject showTableData(@Var("path") String path,
                             @Var("port") String port,
                             @Var("database") String database,
                             @Var("table") String table,
                             @JSONBody QueryInfo queryInfo);

    /**
     * 查询表记录数
     */
    @Get(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/record")
    JSONObject getTableRecord(@Var("path") String path,
                             @Var("port") String port,
                             @Var("database") String database,
                             @Var("table") String table);

    /**
     * 查询建表语句
     */
    @Get(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/script")
    JSONObject showTableScript(@Var("path") String path,
                               @Var("port") String port,
                               @Var("database") String database,
                               @Var("table") String table);

    // column
    /**
     * 查询指定数据表所有列
     */
    @Get(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/columns")
    JSONObject listTableColumns(@Var("path") String path,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("table") String table);

    /**
     * 保存列信息
     */
    @Put(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/columns")
    JSONObject saveTableColumns(@Var("path") String path,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("table") String table,
                                @JSONBody TableDto tableDto);

    /**
     * 调整列顺序
     */
    @Put(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/columns/reorder")
    JSONObject reorderTableColumns(@Var("path") String path,
                                   @Var("port") String port,
                                   @Var("database") String database,
                                   @Var("table") String table,
                                   @JSONBody TableDto tableDto);

    // index
    /**
     * 查询数据表所有索引
     */
    @Get(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/indices")
    JSONObject listTableIndices(@Var("path") String path,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("table") String table);

    /**
     * 保存索引信息
     */
    @Put(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/indices")
    JSONObject saveTableIndices(@Var("path") String path,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("table") String table,
                                @JSONBody List<IndexDto> indexDtos);

    // foreign key
    /**
     * 查询数据表所有外键
     */
    @Get(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/foreignKeys")
    JSONObject listTableForeignKeys(@Var("path") String path,
                                    @Var("port") String port,
                                    @Var("database") String database,
                                    @Var("table") String table);

    /**
     * 保存外键信息
     */
    @Put(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/foreignKeys")
    JSONObject saveTableForeignKeys(@Var("path") String path,
                                    @Var("port") String port,
                                    @Var("database") String database,
                                    @Var("table") String table,
                                    @JSONBody List<ForeignKeyDto> foreignKeyDtos);

    // user
    /**
     * 登录
     */
    @Post(url = "/mysql/{path}/port/{port}/login")
    JSONObject login(@Var("path") String path,
                     @Var("port") String port,
                     @Body("username") String username,
                     @Body("password") String password);

    /**
     * 查询用户列表
     */
    @Get(url = "/mysql/{path}/port/{port}/users")
    JSONObject listUser(@Var("path") String path,
                        @Var("port") String port);

    /**
     * 查询用户详情
     */
    @Get(url = "/mysql/{path}/port/{port}/users/{user}/host/{host}/detail")
    JSONObject showUserDetail(@Var("path") String path,
                              @Var("port") String port,
                              @Var("user") String user,
                              @Var("host") String host);

    /**
     * 创建用户
     */
    @Post(url = "/mysql/{path}/port/{port}/users")
    JSONObject createUser(@Var("path") String path,
                          @Var("port") String port,
                          @JSONBody UserDto userDto);

    /**
     * 删除用户
     */
    @Delete(url = "/mysql/{path}/port/{port}/users/{user}/host/{host}")
    JSONObject dropUser(@Var("path") String path,
                        @Var("port") String port,
                        @Var("user") String user,
                        @Var("host") String host);

    /**
     * 修改用户
     */
    @Put(url = "/mysql/{path}/port/{port}/users/{user}")
    JSONObject updateUsername(@Var("path") String path,
                              @Var("port") String port,
                              @Var("user") String user,
                              @JSONBody UserDto userDto);

    /**
     * 锁定用户
     */
    @Put(url = "/mysql/{path}/port/{port}/users/{user}/host/{host}/lock")
    JSONObject lockUser(@Var("path") String path,
                        @Var("port") String port,
                        @Var("user") String user,
                        @Var("host") String host);

    /**
     * 解锁用户
     */
    @Put(url = "/mysql/{path}/port/{port}/users/{user}/host/{host}/unlock")
    JSONObject unlockUser(@Var("path") String path,
                          @Var("port") String port,
                          @Var("user") String user,
                          @Var("host") String host);

    /**
     * 更新密码
     */
    @Put(url = "/mysql/{path}/port/{port}/users/{user}/password")
    JSONObject updatePassword(@Var("path") String path,
                              @Var("port") String port,
                              @Var("user") String user,
                              @JSONBody UserDto userDto);

    /**
     * 授权数据库权限
     */
    @Put(url = "/mysql/{path}/port/{port}/databases/{database}/privilege")
    JSONObject grantDatabase(@Var("path") String path,
                             @Var("port") String port,
                             @Var("database") String database,
                             @JSONBody GrantOptionDto grantOption);

    /**
     * 授权表权限
     */
    @Put(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/privilege")
    JSONObject grantTable(@Var("path") String path,
                          @Var("port") String port,
                          @Var("database") String database,
                          @Var("table") String table,
                          @JSONBody GrantOptionDto grantOption);

    @Get(url = "/mysql/{path}/port/{port}/users/{user}/host/{host}/databasePrivilege")
    JSONObject showDatabasePrivilege(@Var("path") String path,
                                     @Var("port") String port,
                                     @Var("user") String user,
                                     @Var("host") String host);

    @Get(url = "/mysql/{path}/port/{port}/users/{user}/host/{host}/tablePrivilege")
    JSONObject showTablePrivilege(@Var("path") String path,
                                  @Var("port") String port,
                                  @Var("user") String user,
                                  @Var("host") String host);

    /**
     * 释放数据库权限
     */
    @Delete(url = "/mysql/{path}/port/{port}/databases/{database}/privilege")
    JSONObject revokeDatabasePrivilege(@Var("path") String path,
                                       @Var("port") String port,
                                       @Var("database") String database,
                                       @JSONBody GrantOptionDto grantOption);

    /**
     * 释放表权限
     */
    @Delete(url = "/mysql/{path}/port/{port}/databases/{database}/tables/{table}/privilege")
    JSONObject revokeTablePrivilege(@Var("path") String path,
                                    @Var("port") String port,
                                    @Var("database") String database,
                                    @Var("table") String table,
                                    @JSONBody GrantOptionDto grantOption);

    /**
     * 执行sql
     */
    @Post(url = "/mysql/{path}/port/{port}/databases/{database}")
    JSONObject execSql(@Var("path") String path,
                       @Var("port") String port,
                       @Var("database") String database,
                       @JSONBody SqlQuery sqlQuery);

}
