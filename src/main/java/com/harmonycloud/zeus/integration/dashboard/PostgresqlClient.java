package com.harmonycloud.zeus.integration.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.dtflys.forest.http.ForestResponse;
import com.harmonycloud.caas.common.model.dashboard.*;
import com.harmonycloud.zeus.interceptor.MiddlewareApiInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;

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
    @Get(url = "/postgresql/{name}/port/{port}/login")
    JSONObject login(@Var("name") String name,
                     @Var("port") String port,
                     @Body("username") String username,
                     @Body("password") String password);

    /**
     * 查询database列表
     */
    @Get(url = "/postgresql/{name}/port/{port}/databases")
    JSONObject listDatabases(@Var("name") String name,
                             @Var("port") String port);

    /**
     * 创建database
     */
    @Post(url = "/postgresql/{name}/port/{port}/databases")
    JSONObject createDatabase(@Var("name") String name,
                              @Var("port") String port,
                              @JSONBody DatabaseDto database);

    /**
     * 更新database
     */
    @Put(url = "/postgresql/{name}/port/{port}/databases/{database}")
    JSONObject updateDatabase(@Var("name") String name,
                              @Var("port") String port,
                              @Var("database") String database,
                              @JSONBody DatabaseDto databaseDto);

    /**
     * 删除database
     */
    @Delete(url = "/postgresql/{name}/port/{port}/databases/{database}")
    JSONObject dropDatabase(@Var("name") String name,
                            @Var("port") String port,
                            @Var("database") String database);

    /**
     * 查询database备注
     */
    @Get(url = "/postgresql/{name}/port/{port}/databases/{database}/notes?oid={oid}")
    JSONObject getDatabaseNotes(@Var("name") String name,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("oid") String oid);

    /**
     * 查询schema列表
     */
    @Get(url = "/postgresql/{name}/port/{port}/databases/{database}/schemas")
    JSONObject listSchemas(@Var("name") String name,
                           @Var("port") String port,
                           @Var("database") String database);

    /**
     * 创建schema
     */
    @Post(url = "/postgresql/{name}/port/{port}/databases/{database}/schemas")
    JSONObject createSchema(@Var("name") String name,
                            @Var("port") String port,
                            @Var("port") String database,
                            @JSONBody SchemaDto schema);

    /**
     * 更新schema
     */
    @Put(url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schemaName}")
    JSONObject updateSchema(@Var("name") String name,
                            @Var("port") String port,
                            @Var("database") String database,
                            @Var("schemaName") String schemaName,
                            @JSONBody SchemaDto schema);

    /**
     * 删除schema
     */
    @Delete(url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}")
    JSONObject dropSchemas(@Var("name") String name,
                           @Var("port") String port,
                           @Var("database") String database,
                           @Var("schema") String schema);

    /**
     * 查询schema备注
     */
    @Get(url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/notes")
    JSONObject getSchemaNotes(@Var("name") String name,
                              @Var("port") String port,
                              @Var("database") String database,
                              @Var("schema") String schema);

    /**
     * 查询table列表
     */
    @Get(url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables")
    JSONObject listTables(@Var("name") String name,
                          @Var("port") String port,
                          @Var("database") String database,
                          @Var("schema") String schema);

    /**
     * 创建table
     */
    @Post(url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables")
    JSONObject addTable(@Var("name") String name,
                        @Var("port") String port,
                        @Var("database") String database,
                        @Var("schema") String schema,
                        @JSONBody JSONObject table);

    /**
     * 删除table
     */
    @Delete(
        url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}")
    JSONObject dropTable(@Var("name") String name,
                         @Var("port") String port,
                         @Var("database") String database,
                         @Var("schema") String schema,
                         @Var("table") String table);

    /**
     * 查询column列表
     */
    @Get(
        url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}")
    JSONObject listColumns(@Var("name") String name,
                           @Var("port") String port,
                           @Var("database") String database,
                           @Var("schema") String schema,
                           @Var("table") String table);

    /**
     * 创建外键约束
     */
    @Post(
        url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/foreignKey")
    JSONObject createForeignKey(@Var("name") String name,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("schema") String schema,
                                @Var("table") String table,
                                @JSONBody TableForeignKey foreignKey);

    /**
     * 创建排它约束
     */
    @Post(
        url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/exclude")
    JSONObject createExclusion(@Var("name") String name,
                               @Var("port") String port,
                               @Var("database") String database,
                               @Var("schema") String schema,
                               @Var("table") String table,
                               @JSONBody TableExclusion exclusion);

    /**
     * 创建唯一约束
     */
    @Post(
        url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/unique")
    JSONObject createUnique(@Var("name") String name,
                            @Var("port") String port,
                            @Var("database") String database,
                            @Var("schema") String schema,
                            @Var("table") String table,
                            @JSONBody TableUnique unique);

    /**
     * 创建检查约束
     */
    @Post(
        url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/check")
    JSONObject createCheck(@Var("name") String name,
                           @Var("port") String port,
                           @Var("database") String database,
                           @Var("schema") String schema,
                           @Var("table") String table,
                           @JSONBody TableCheck check);

    /**
     * 添加继承关系
     */
    @Post(
        url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/inherit")
    JSONObject addInherit(@Var("name") String name,
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
        url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/inherit")
    JSONObject dropInherit(@Var("name") String name,
                           @Var("port") String port,
                           @Var("database") String database,
                           @Var("schema") String schema,
                           @Var("table") String table,
                           @Body("parentSchema") String parentSchema,
                           @Body("parentTable") String parentTable);

    /**
     * 删除约束
     */
    @Delete(
        url = "/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/foreignKey")
    JSONObject deleteConstraint(@Var("name") String name,
                                @Var("port") String port,
                                @Var("database") String database,
                                @Var("schema") String schema,
                                @Var("table") String table,
                                @Body String constraintName);

    /**
     * 查询user列表
     */
    @Get(url = "/postgresql/{name}/port/{port}/user")
    JSONObject listUsers(@Var("name") String name,
                         @Var("port") String port);

    /**
     * 创建用户
     */
    @Post(url = "/postgresql/{name}/port/{port}/user")
    JSONObject addUser(@Var("name") String name,
                       @Var("port") String port,
                       @Body("username") String username,
                       @Body("password") String password);

    /**
     * 删除用户
     */
    @Delete(url = "/postgresql/{name}/port/{port}/user/{username}")
    JSONObject dropUser(@Var("name") String name,
                        @Var("port") String port,
                        @Var("username") String username);

    /**
     * 赋权用户database
     */
    @Post(url = "/postgresql/{name}/port/{port}/user/{username}/database/{database}")
    JSONObject grantUserDatabase(@Var("name") String name,
                                 @Var("port") String port,
                                 @Var("username") String username,
                                 @Var("database") String database,
                                 @Body("privileges") String privileges,
                                 @Body("grantOption") String grantOption);

    /**
     * 赋权用户schema
     */
    @Post(url = "/postgresql/{name}/port/{port}/user/{username}/database/{database}/schema/{schema}")
    JSONObject grantUserSchema(@Var("name") String name,
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
        url = "/postgresql/{name}/port/{port}/user/{username}/database/{database}/schema/{schema}/table/{table}")
    JSONObject grantUserTable(@Var("name") String name,
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
    @Delete(url = "/postgresql/{name}/port/{port}/user/{username}/database/{database}")
    JSONObject revokeUserDatabase(@Var("name") String name,
                                 @Var("port") String port,
                                 @Var("username") String username,
                                 @Var("database") String database,
                                 @Body("privileges") String privileges);

    /**
     * 取消赋权用户schema
     */
    @Delete(url = "/postgresql/{name}/port/{port}/user/{username}/database/{database}/schema/{schema}")
    JSONObject revokeUserSchema(@Var("name") String name,
                               @Var("port") String port,
                               @Var("username") String username,
                               @Var("database") String database,
                               @Var("schema") String schema,
                               @Body("privileges") String privileges);

    /**
     * 取消赋权用户table
     */
    @Delete(
            url = "/postgresql/{name}/port/{port}/user/{username}/database/{database}/schema/{schema}/table/{table}")
    JSONObject revokeUserTable(@Var("name") String name,
                              @Var("port") String port,
                              @Var("username") String username,
                              @Var("database") String database,
                              @Var("schema") String schema,
                              @Var("table") String table,
                              @Body("privileges") String privileges);

}
