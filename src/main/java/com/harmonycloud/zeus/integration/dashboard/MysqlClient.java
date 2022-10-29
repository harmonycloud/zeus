package com.harmonycloud.zeus.integration.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.harmonycloud.caas.common.model.dashboard.mysql.*;
import com.harmonycloud.zeus.interceptor.DashboardInterceptor;
import com.harmonycloud.zeus.interceptor.MiddlewareApiInterceptor;
import org.springframework.stereotype.Component;

/**
 * @author liyinlong
 * @Date 2022/10/09 3:08 下午
 */
@Component
@Address(source = MiddlewareApiAddress.class)
@BaseRequest(interceptor = {MiddlewareApiInterceptor.class, DashboardInterceptor.class})
public interface MysqlClient {

    /**
     * 查询database列表
     */
    @Get(url = "/mysql/{host}/port/{port}/databases")
    JSONObject listDatabases(@Var("host") String host, @Var("port") String port);

    /**
     * 创建数据库
     * databaseDto.db: 数据库名称
     * databaseDto.character: 字符集
     * databaseDto.collate 排序规则
     */
    @Post(url = "/mysql/{host}/port/{port}/databases")
    JSONObject createDatabase(@Var("host") String host, @Var("port") String port, @JSONBody DatabaseDto databaseDto);

    /**
     * 修改数据库
     */
    @Put(url = "/mysql/{host}/port/{port}/databases")
    JSONObject alterDatabase(@Var("host") String host, @Var("port") String port, @JSONBody DatabaseDto databaseDto);

    /**
     * 删除数据库
     */
    @Delete(url = "/mysql/{host}/port/{port}/databases/{database}")
    JSONObject dropDatabase(@Var("host") String host, @Var("port") String port, @Var("database") String database);

    /**
     * 查询字符集
     */
    @Get(url = "/mysql/{host}/port/{port}/charsets")
    JSONObject listCharsets(@Var("host") String host, @Var("port") String port);

    /**
     * 查询字符集排序规则
     */
    @Get(url = "/mysql/{host}/port/{port}/charsets/{charset}/collations")
    JSONObject listCharsetCollations(@Var("host") String host, @Var("port") String port, @Var("charset") String charset);

    // tables

    @Get(url = "/mysql/{host}/port/{port}/engines")
    JSONObject listEngines(@Var("host") String host, @Var("port") String port);

    /**
     * 查询指定数据库所有数据表
     */
    @Get(url = "/mysql/{host}/port/{port}/databases/{database}/tables")
    JSONObject listTables(@Var("host") String host, @Var("port") String port, @Var("database") String database);

    /**
     * 创建表
     */
    @Post(url = "/mysql/{host}/port/{port}/databases/{database}/tables")
    JSONObject createTable(@Var("host") String host, @Var("port") String port, @Var("database") String database, @JSONBody TableDto tableDto);

    /**
     * 更新表基本信息
     */
    @Put(url = "/mysql/{host}/port/{port}/databases/{database}/tables/{table}/options")
    JSONObject updateTableOptions(@Var("host") String host,
                                  @Var("port") String port,
                                  @Var("database") String database,
                                  @Var("table") String table,
                                  @JSONBody TableDto tableDto);

    /**
     * 删除表
     */
    @Delete(url = "/mysql/{host}/port/{port}/databases/{database}/tables/{table}")
    JSONObject dropTable(@Var("host") String host,
                         @Var("port") String port,
                         @Var("database") String database,
                         @Var("table") String table);

    /**
     * 获取表基本信息
     */
    @Get(url = "/mysql/{host}/port/{port}/databases/{database}/tables/{table}/options")
    JSONObject showTableOptions(@Var("host") String host,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("table") String table);

    /**
     * 查询表数据
     */
    @Post(url = "/mysql/{host}/port/{port}/databases/{database}/tables/{table}/data")
    JSONObject showTableData(@Var("host") String host,
                             @Var("port") String port,
                             @Var("database") String database,
                             @Var("table") String table,
                             @JSONBody QueryInfo queryInfo);

    /**
     * 查询建表语句
     */
    @Get(url = "/mysql/{host}/port/{port}/databases/{database}/tables/{table}/script")
    JSONObject showTableScript(@Var("host") String host,
                               @Var("port") String port,
                               @Var("database") String database,
                               @Var("table") String table);

    // column
    /**
     * 查询指定数据表所有列
     */
    @Get(url = "/mysql/{host}/port/{port}/databases/{database}/tables/{table}/columns")
    JSONObject listTableColumns(@Var("host") String host,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("table") String table);

    // index
    /**
     * 查询数据表所有索引
     */
    @Get(url = "/mysql/{host}/port/{port}/databases/{database}/tables/{table}/indices")
    JSONObject listTableIndices(@Var("host") String host,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("table") String table);

    // foreign key
    /**
     * 查询数据表所有外键
     */
    @Get(url = "/mysql/{host}/port/{port}/databases/{database}/tables/{table}/foreignKeys")
    JSONObject listTableForeignKeys(@Var("host") String host,
                                    @Var("port") String port,
                                    @Var("database") String database,
                                    @Var("table") String table);

    // user
    /**
     * 登录
     */
    @Post(url = "/mysql/{host}/port/{port}/login")
    JSONObject login(@Var("host") String host,
                     @Var("port") String port,
                     @Body("username") String username,
                     @Body("password") String password);

    /**
     * 查询用户列表
     */
    @Get(url = "/mysql/{host}/port/{port}/users")
    JSONObject listUser(@Var("host") String host,
                        @Var("port") String port);

    /**
     * 查询用户详情
     */
    @Get(url = "/mysql/{host}/port/{port}/users/{user}/detail")
    JSONObject showUserDetail(@Var("host") String host,
                              @Var("port") String port,
                              @Var("user") String user);

    /**
     * 创建用户
     */
    @Post(url = "/mysql/{host}/port/{port}/users")
    JSONObject createUser(@Var("host") String host,
                          @Var("port") String port,
                          @JSONBody UserDto userDto);

    /**
     * 删除用户
     */
    @Delete(url = "/mysql/{host}/port/{port}/users/{user}")
    JSONObject dropUser(@Var("host") String host,
                        @Var("port") String port,
                        @Var("user") String user);

    /**
     * 修改用户
     */
    @Put(url = "/mysql/{host}/port/{port}/users/{user}")
    JSONObject updateUsername(@Var("host") String host,
                              @Var("port") String port,
                              @Var("user") String user,
                              @JSONBody UserDto userDto);

    /**
     * 锁定用户
     */
    @Put(url = "/mysql/{host}/port/{port}/users/{user}/lock")
    JSONObject lockUser(@Var("host") String host,
                        @Var("port") String port,
                        @Var("user") String user);

    /**
     * 解锁用户
     */
    @Put(url = "/mysql/{host}/port/{port}/users/{user}/unlock")
    JSONObject unlockUser(@Var("host") String host,
                          @Var("port") String port,
                          @Var("user") String user);

    /**
     * 更新密码
     */
    @Put(url = "/mysql/{host}/port/{port}/users/{user}/password")
    JSONObject updatePassword(@Var("host") String host,
                              @Var("port") String port,
                              @Var("user") String user,
                              @JSONBody UserDto userDto);

    /**
     * 授权数据库权限
     */
    @Put(url = "/mysql/{host}/port/{port}/databases/{database}/privilege")
    JSONObject grantDatabase(@Var("host") String host,
                             @Var("port") String port,
                             @Var("database") String database,
                             @JSONBody GrantOptionDto grantOption);

    /**
     * 授权表权限
     */
    @Put(url = "/mysql/{host}/port/{port}/databases/{database}/tables/{table}/privilege")
    JSONObject grantTable(@Var("host") String host,
                          @Var("port") String port,
                          @Var("database") String database,
                          @Var("table") String table,
                          @JSONBody GrantOptionDto grantOption);

    @Get(url = "/mysql/{host}/port/{port}/users/{user}/databasePrivilege")
    JSONObject showDatabasePrivilege(@Var("host") String host,
                                     @Var("port") String port,
                                     @Var("user") String user);

    @Get(url = "/mysql/{host}/port/{port}/users/{user}/tablePrivilege")
    JSONObject showTablePrivilege(@Var("host") String host,
                                  @Var("port") String port,
                                  @Var("user") String user);

    /**
     * 释放数据库权限
     */
    @Delete(url = "/mysql/{host}/port/{port}/databases/{database}/privilege")
    JSONObject revokeDatabasePrivilege(@Var("host") String host,
                                       @Var("port") String port,
                                       @Var("database") String database,
                                       @JSONBody GrantOptionDto grantOption);

    /**
     * 释放表权限
     */
    @Delete(url = "/mysql/{host}/port/{port}/databases/{database}/tables/{table}/privilege")
    JSONObject revokeTablePrivilege(@Var("host") String host,
                                    @Var("port") String port,
                                    @Var("database") String database,
                                    @Var("table") String table,
                                    @JSONBody GrantOptionDto grantOption);


}
