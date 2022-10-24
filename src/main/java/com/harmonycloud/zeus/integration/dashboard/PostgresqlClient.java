package com.harmonycloud.zeus.integration.dashboard;

import com.dtflys.forest.http.ForestResponse;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.harmonycloud.caas.common.model.dashboard.*;
import com.harmonycloud.zeus.interceptor.MiddlewareApiInterceptor;

import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/10/11 3:08 下午
 */
@Component
@Address(source = MiddlewareApiAddress.class)
@BaseRequest(interceptor = MiddlewareApiInterceptor.class)
public interface PostgresqlClient {

    /**
     * 登录
     */
    @Post(url = "/postgresql/{path}/port/{port}/login")
    JSONObject login(@Var("path") String path,
                     @Var("port") String port,
                     @Body("username") String username,
                     @Body("password") String password);

    /**
     * 执行sql语句
     */
    @Post(url = "/postgresql/{path}/port/{port}/databases/{databaseName}/sql")
    ForestResponse<JSONObject> sqlExecute(@Var("path") String path,
                                          @Var("port") String port,
                                          @Var("databaseName") String databaseName,
                                          @Body("sql") String sql,
                                          @Body("query") boolean query);

    /**
     * 查询database列表
     */
    @Get(url = "/postgresql/{path}/port/{port}/databases")
    JSONObject listDatabases(@Var("path") String path,
                             @Var("port") String port);

    /**
     * 创建database
     */
    @Post(url = "/postgresql/{path}/port/{port}/databases")
    JSONObject createDatabase(@Var("path") String path,
                              @Var("port") String port,
                              @JSONBody DatabaseDto database);

    /**
     * 更新database
     */
    @Put(url = "/postgresql/{path}/port/{port}/databases/{database}")
    JSONObject updateDatabase(@Var("path") String path,
                              @Var("port") String port,
                              @Var("database") String database,
                              @JSONBody DatabaseDto databaseDto);

    /**
     * 删除database
     */
    @Delete(url = "/postgresql/{path}/port/{port}/databases/{database}")
    JSONObject dropDatabase(@Var("path") String path,
                            @Var("port") String port,
                            @Var("database") String database);

    /**
     * 获取tablespace
     */
    @Get(url = "/postgresql/{path}/port/{port}/databases/{database}/tablespace")
    JSONObject getTablespace(@Var("path") String path,
                             @Var("port") String port,
                             @Var("database") String database);

    /**
     * 查询database备注
     */
    @Get(url = "/postgresql/{path}/port/{port}/databases/{database}/notes?oid={oid}")
    JSONObject getDatabaseNotes(@Var("path") String path,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("oid") String oid);

    /**
     * 查询schema列表
     */
    @Get(url = "/postgresql/{path}/port/{port}/databases/{database}/schemas")
    JSONObject listSchemas(@Var("path") String path,
                           @Var("port") String port,
                           @Var("database") String database);

    /**
     * 创建schema
     */
    @Post(url = "/postgresql/{path}/port/{port}/databases/{database}/schemas")
    JSONObject createSchema(@Var("path") String path,
                            @Var("port") String port,
                            @Var("database") String database,
                            @JSONBody SchemaDto schema);

    /**
     * 更新schema
     */
    @Put(url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schemaName}")
    JSONObject updateSchema(@Var("path") String path,
                            @Var("port") String port,
                            @Var("database") String database,
                            @Var("schemaName") String schemaName,
                            @JSONBody SchemaDto schema);

    /**
     * 删除schema
     */
    @Delete(url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}")
    JSONObject dropSchemas(@Var("path") String path,
                           @Var("port") String port,
                           @Var("database") String database,
                           @Var("schema") String schema);

    /**
     * 查询schema备注
     */
    @Get(url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/notes")
    JSONObject getSchemaNotes(@Var("path") String path,
                              @Var("port") String port,
                              @Var("database") String database,
                              @Var("schema") String schema);

    /**
     * 查询table列表
     */
    @Get(url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/tables")
    JSONObject listAllTables(@Var("path") String path,
                             @Var("port") String port,
                             @Var("database") String database);

    /**
     * 查询table列表
     */
    @Get(url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables")
    JSONObject listTables(@Var("path") String path,
                          @Var("port") String port,
                          @Var("database") String database,
                          @Var("schema") String schema);

    /**
     * 创建table
     */
    @Post(url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables")
    JSONObject addTable(@Var("path") String path,
                        @Var("port") String port,
                        @Var("database") String database,
                        @Var("schema") String schema,
                        @JSONBody JSONObject table);

    /**
     * 更新table
     */
    @Post(url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}")
    JSONObject updateTable(@Var("path") String path,
                           @Var("port") String port,
                           @Var("database") String database,
                           @Var("schema") String schema,
                           @Var("table") String table,
                           @JSONBody TableDto tableDto);

    /**
     * 更新table column
     */
    @Post(url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/columns")
    JSONObject updateColumn(@Var("path") String path,
                            @Var("port") String port,
                            @Var("database") String database,
                            @Var("schema") String schema,
                            @Var("table") String table,
                            @JSONBody JSONObject object);

    /**
     * 删除table
     */
    @Delete(
        url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}")
    JSONObject dropTable(@Var("path") String path,
                         @Var("port") String port,
                         @Var("database") String database,
                         @Var("schema") String schema,
                         @Var("table") String table);

    /**
     * 查询column列表
     */
    @Get(
        url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/columns")
    JSONObject listColumns(@Var("path") String path,
                           @Var("port") String port,
                           @Var("database") String database,
                           @Var("schema") String schema,
                           @Var("table") String table);

    /**
     * 查询表数据
     */
    @Get(
            url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/data?limit={limit}&offset={offset}&order={order}")
    JSONObject getTableData(@Var("path") String path,
                            @Var("port") String port,
                            @Var("database") String database,
                            @Var("schema") String schema,
                            @Var("table") String table,
                            @Var("limit") Integer limit,
                            @Var("offset") Integer offset,
                            @Var("order") String order);

    /**
     * 查询表数据条数
     */
    @Get(
            url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/count")
    JSONObject getTableDataCount(@Var("path") String path,
                                 @Var("port") String port,
                                 @Var("database") String database,
                                 @Var("schema") String schema,
                                 @Var("table") String table);

    /**
     * 创建外键约束
     */
    @Post(
        url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/foreignKey")
    JSONObject createForeignKey(@Var("path") String path,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("schema") String schema,
                                @Var("table") String table,
                                @JSONBody TableForeignKey foreignKey);

    /**
     * 创建排它约束
     */
    @Post(
        url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/exclude")
    JSONObject createExclusion(@Var("path") String path,
                               @Var("port") String port,
                               @Var("database") String database,
                               @Var("schema") String schema,
                               @Var("table") String table,
                               @JSONBody TableExclusion exclusion);

    /**
     * 创建唯一约束
     */
    @Post(
        url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/unique")
    JSONObject createUnique(@Var("path") String path,
                            @Var("port") String port,
                            @Var("database") String database,
                            @Var("schema") String schema,
                            @Var("table") String table,
                            @Body String uniqueName,
                            @Body String columnName,
                            @Body String deferrablity);

    /**
     * 创建检查约束
     */
    @Post(
        url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/check")
    JSONObject createCheck(@Var("path") String path,
                           @Var("port") String port,
                           @Var("database") String database,
                           @Var("schema") String schema,
                           @Var("table") String table,
                           @Body String checkName,
                           @Body String rule,
                           @Body String inherit,
                           @Body String vaild);

    /**
     * 添加继承关系
     */
    @Post(
        url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/inherit")
    JSONObject addInherit(@Var("path") String path,
                          @Var("port") String port,
                          @Var("database") String database,
                          @Var("schema") String schema,
                          @Var("table") String table,
                          @Body("parentSchema") String parentSchema,
                          @Body("parentTable") String parentTable);

    /**
     * 添加继承关系
     */
    @Delete(
        url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/inherit")
    JSONObject dropInherit(@Var("path") String path,
                           @Var("port") String port,
                           @Var("database") String database,
                           @Var("schema") String schema,
                           @Var("table") String table,
                           @Body("parentSchema") String parentSchema,
                           @Body("parentTable") String parentTable);

    /**
     * 获取约束
     */
    @Get(
            url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/constraint?oid={oid}")
    JSONObject getConstraint(@Var("path") String path,
                             @Var("port") String port,
                             @Var("database") String database,
                             @Var("schema") String schema,
                             @Var("table") String table,
                             @Var("oid") String oid);

    /**
     * 删除约束
     */
    @Delete(
        url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/constraint")
    JSONObject deleteConstraint(@Var("path") String path,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("schema") String schema,
                                @Var("table") String table,
                                @Body String constraintName);

    /**
     * 获取继承关系
     */
    @Get(
            url = "/postgresql/{path}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/inherit?oid={oid}")
    JSONObject getInherit(@Var("path") String path,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("schema") String schema,
                                @Var("table") String table,
                                @Var("oid") String oid);

    /**
     * 查询user列表
     */
    @Get(url = "/postgresql/{path}/port/{port}/user")
    JSONObject listUsers(@Var("path") String path,
                         @Var("port") String port);

    /**
     * 创建用户
     */
    @Post(url = "/postgresql/{path}/port/{port}/user")
    JSONObject addUser(@Var("path") String path,
                       @Var("port") String port,
                       @Body("username") String username,
                       @Body("password") String password);

    /**
     * 更新密码
     */
    @Put(url = "/postgresql/{path}/port/{port}/user/{usernmae}/password")
    JSONObject updatePassword(@Var("path") String path,
                              @Var("port") String port,
                              @Var("username") String username,
                              @Body("password") String password);


    /**
     * 删除用户
     */
    @Delete(url = "/postgresql/{path}/port/{port}/user/{username}")
    JSONObject dropUser(@Var("path") String path,
                        @Var("port") String port,
                        @Var("username") String username);

    /**
     * 赋权用户database
     */
    @Post(url = "/postgresql/{path}/port/{port}/user/{username}/database/{database}")
    JSONObject grantUserDatabase(@Var("path") String path,
                                 @Var("port") String port,
                                 @Var("username") String username,
                                 @Var("database") String database,
                                 @Body("privileges") String privileges,
                                 @Body("grantOption") String grantOption);

    /**
     * 赋权用户schema
     */
    @Post(url = "/postgresql/{path}/port/{port}/user/{username}/database/{database}/schema/{schema}")
    JSONObject grantUserSchema(@Var("path") String path,
                               @Var("port") String port,
                               @Var("username") String username,
                               @Var("database") String database,
                               @Var("schema") String schema,
                               @Body("privileges") String privileges,
                               @Body("grantOption") String grantOption);

    /**
     * 赋权用户table
     */
    @Post(
        url = "/postgresql/{path}/port/{port}/user/{username}/database/{database}/schema/{schema}/table/{table}")
    JSONObject grantUserTable(@Var("path") String path,
                              @Var("port") String port,
                              @Var("username") String username,
                              @Var("database") String database,
                              @Var("schema") String schema,
                              @Var("table") String table,
                              @Body("privileges") String privileges,
                              @Body("grantOption") String grantOption);

    /**
     * 取消赋权用户database
     */
    @Delete(url = "/postgresql/{path}/port/{port}/user/{username}/database/{database}")
    JSONObject revokeUserDatabase(@Var("path") String path,
                                 @Var("port") String port,
                                 @Var("username") String username,
                                 @Var("database") String database,
                                 @Body("privileges") String privileges);

    /**
     * 取消赋权用户schema
     */
    @Delete(url = "/postgresql/{path}/port/{port}/user/{username}/database/{database}/schema/{schema}")
    JSONObject revokeUserSchema(@Var("path") String path,
                               @Var("port") String port,
                               @Var("username") String username,
                               @Var("database") String database,
                               @Var("schema") String schema,
                               @Body("privileges") String privileges);

    /**
     * 取消赋权用户table
     */
    @Delete(
            url = "/postgresql/{path}/port/{port}/user/{username}/database/{database}/schema/{schema}/table/{table}")
    JSONObject revokeUserTable(@Var("path") String path,
                              @Var("port") String port,
                              @Var("username") String username,
                              @Var("database") String database,
                              @Var("schema") String schema,
                              @Var("table") String table,
                              @Body("privileges") String privileges);

    /**
     * 启用/禁用用户
     */
    @Put(url = "/postgresql/{path}/port/{port}/user/{username}/login?enable={enable}")
    JSONObject enableUser(@Var("path") String path,
                          @Var("port") String port,
                          @Var("username") String username,
                          @Var("enable") Boolean enable);

}
