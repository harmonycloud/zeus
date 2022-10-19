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
@Address
@BaseRequest(interceptor = MiddlewareApiInterceptor.class)
public interface PostgresqlClient {

    /**
     * 登录
     */
    @Get(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/login")
    JSONObject login(@Var("name") String name, @Var("port") String port, @Body("username") String username,
        @Body("password") String password);

    /**
     * 查询database列表
     */
    @Get(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases")
    JSONObject listDatabases(@Var("name") String name, @Var("port") String port);

    /**
     * 查询database列表
     */
    @Post(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases")
    JSONObject createDatabase(@Var("name") String name, @Var("port") String port, @JSONBody DatabaseDto database);

    /**
     * 查询database列表
     */
    @Delete(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}")
    JSONObject dropDatabase(@Var("name") String name, @Var("port") String port, @Var("database") String database);

    /**
     * 查询schema列表
     */
    @Get(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas")
    JSONObject listSchemas(@Var("name") String name, @Var("port") String port, @Var("database") String database);

    /**
     * 创建schema
     */
    @Post(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas")
    JSONObject createSchema(@Var("name") String name, @Var("port") String port, @Var("port") String database,
        @JSONBody SchemaDto schema);

    /**
     * 删除schema
     */
    @Delete(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}")
    JSONObject dropSchemas(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema);

    /**
     * 查询schema列表
     */
    @Get(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/notes")
    JSONObject getSchemaNotes(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema);

    /**
     * 查询table列表
     */
    @Get(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}")
    JSONObject listTables(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema);

    /**
     * 创建table
     */
    @Post(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}")
    JSONObject addTable(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema, @JSONBody JSONObject table);

    /**
     * 删除table
     */
    @Delete(
        url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}")
    JSONObject dropTable(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema, @Var("table") String table);

    /**
     * 查询column列表
     */
    @Get(
        url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}")
    JSONObject listColumns(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema, @Var("table") String table);

    /**
     * 创建外键约束
     */
    @Post(
        url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/foreignKey")
    JSONObject createForeignKey(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema, @Var("table") String table, @JSONBody TableForeignKey foreignKey);

    /**
     * 创建排它约束
     */
    @Post(
        url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/exclude")
    JSONObject createExclusion(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema, @Var("table") String table, @JSONBody TableExclusion exclusion);

    /**
     * 创建唯一约束
     */
    @Post(
        url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/unique")
    JSONObject createUnique(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema, @Var("table") String table, @JSONBody TableUnique unique);

    /**
     * 创建检查约束
     */
    @Post(
        url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/check")
    JSONObject createCheck(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema, @Var("table") String table, @JSONBody TableCheck check);

    /**
     * 添加继承关系
     */
    @Post(
        url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/foreignKey")
    JSONObject addInherit(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema, @Var("table") String table, @JSONBody TableForeignKey foreignKey);

    /**
     * 删除约束
     */
    @Delete(
        url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/databases/{database}/schemas/{schema}/tables/{table}/foreignKey")
    JSONObject deleteConstraint(@Var("name") String name, @Var("port") String port, @Var("database") String database,
        @Var("schema") String schema, @Var("table") String table, @Body String constraintName);

    /**
     * 查询user列表
     */
    @Get(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/user")
    JSONObject listUsers(@Var("name") String name, @Var("port") String port);

    /**
     * 创建用户
     */
    @Post(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/user")
    JSONObject addUser(@Var("name") String name, @Var("port") String port, @Body("username") String username,
        @Body("password") String password);

    /**
     * 创建用户
     */
    @Delete(url = "http://127.0.0.1:8081/postgresql/{name}/port/{port}/user/{username}")
    JSONObject dropUser(@Var("name") String name, @Var("port") String port, @Var("username") String username);

}
