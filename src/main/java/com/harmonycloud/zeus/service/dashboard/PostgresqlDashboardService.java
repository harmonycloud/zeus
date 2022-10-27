package com.harmonycloud.zeus.service.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.dashboard.*;
import com.harmonycloud.caas.common.model.dashboard.mysql.QueryInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/10/11 2:50 下午
 */
public interface PostgresqlDashboardService extends BaseMiddlewareApiService {

    /**
     * 获取database列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 库名称
     * @param executeSqlDto 执行sql对象
     * @return  ExecuteSqlDto
     */
    ExecuteSqlDto executeSql(String clusterId, String namespace, String middlewareName, String databaseName,
                                 ExecuteSqlDto executeSqlDto);

    /**
     * 获取database列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @return  List<DatabaseDto>
     */
    List<DatabaseDto> listDatabases(String clusterId, String namespace, String middlewareName);

    /**
     * 获取database信息
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName  database名称
     * @return  DatabaseDto
     */
    DatabaseDto getDatabase(String clusterId, String namespace, String middlewareName, String databaseName);

    /**
     * 创建database
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     */
    void addDatabase(String clusterId, String namespace, String middlewareName, DatabaseDto databaseDto);

    /**
     * 创建database
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     */
    void updateDatabase(String clusterId, String namespace, String middlewareName, String database, DatabaseDto databaseDto);

    /**
     * 删除database
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     */
    void deleteDatabase(String clusterId, String namespace, String middlewareName, String databaseName);

    /**
     * 获取tablespace
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     */
    List<String> getTablespace(String clusterId, String namespace, String middlewareName, String databaseName);

    /**
     * 获取schema列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     * @return List<SchemaDto>
     */
    List<SchemaDto> listSchemas(String clusterId, String namespace, String middlewareName, String databaseName);

    /**
     * 获取schema列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     * @param schemaName 模式名称
     * @return List<SchemaDto>
     */
    SchemaDto getSchema(String clusterId, String namespace, String middlewareName, String databaseName, String schemaName);

    /**
     * 创建schema
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param schemaDto 模式对象
     */
    void addSchema(String clusterId, String namespace, String middlewareName, SchemaDto schemaDto);

    /**
     * 更新shcema
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param schemaDto 模式对象
     */
    void updateSchema(String clusterId, String namespace, String middlewareName, String databaseName, String schemaName, SchemaDto schemaDto);

    /**
     * 获取schema列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     * @param schemaName 模式名称
     */
    void deleteSchema(String clusterId, String namespace, String middlewareName, String databaseName, String schemaName);

    /**
     * 获取table列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     * @param schemaName 模式名称
     */
    List<TableDto> listTables(String clusterId, String namespace, String middlewareName, String databaseName, String schemaName);

    /**
     * 获取table
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     * @param schemaName 模式名称
     */
    TableDto getTable(String clusterId, String namespace, String middlewareName, String databaseName, String schemaName,
                      String tableName);

    /**
     * 创建table
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param tableDto 表对象
     */
    void addTable(String clusterId, String namespace, String middlewareName, TableDto tableDto);

    /**
     * 更新table
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param tableDto 表对象
     */
    void updateTable(String clusterId, String namespace, String middlewareName, String databaseName, String schemaName,
                     String tableName, TableDto tableDto);

    /**
     * 删除table
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     * @param schemaName 模式名称
     * @param tableName 表名称
     */
    void dropTable(String clusterId, String namespace, String middlewareName, String databaseName, String schemaName, String tableName);

    /**
     * 查询表行数
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     * @param schemaName 模式名称
     * @param tableName 表名称
     */
    Integer getTableDataCount(String clusterId, String namespace, String middlewareName, String databaseName,
                              String schemaName, String tableName);

    /**
     * 查询表数据
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     * @param schemaName 模式名称
     * @param tableName 表名称
     * @param queryInfo 分页业务数据
     */
    List<Map<String, String>> getTableData(String clusterId, String namespace, String middlewareName, String databaseName, String schemaName,
                      String tableName, QueryInfo queryInfo);

    /**
     * 查询建表语句文件
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     * @param schemaName 模式名称
     * @param tableName 表名称
     */
    void getTableCreateSql(String clusterId, String namespace, String middlewareName, String databaseName,
                           String schemaName, String tableName, HttpServletRequest request, HttpServletResponse response);

    /**
     * 获取表结构文件
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param databaseName 数据库名称
     * @param schemaName 模式名称
     * @param tableName 表名称
     */
    void getTableExcel(String clusterId, String namespace, String middlewareName, String databaseName,
                       String schemaName, String tableName, HttpServletRequest request, HttpServletResponse response);

    /**
     * 获取table列表
     */
    List<ColumnDto> listColumns(String clusterId, String namespace, String middlewareName, String databaseName, String schemaName, String table);

    /**
     * 增删外键约束
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param tableDto 表对象
     */
    void updateForeignKey(String clusterId, String namespace, String middlewareName, TableDto tableDto);

    /**
     * 增删排它约束
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param tableDto 表对象
     */
    void updateExclusion(String clusterId, String namespace, String middlewareName, TableDto tableDto);

    /**
     * 增删排它约束
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param tableDto 表对象
     */
    void updateUnique(String clusterId, String namespace, String middlewareName, TableDto tableDto);

    /**
     * 增删检查约束
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param tableDto 表对象
     */
    void updateCheck(String clusterId, String namespace, String middlewareName, TableDto tableDto);

    /**
     * 增删继承关系
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param tableDto 表对象
     */
    void updateInherit(String clusterId, String namespace, String middlewareName, TableDto tableDto);

    /**
     * 获取user列表
     *
     */
    List<MiddlewareUserDto> listUser(String clusterId, String namespace, String middlewareName, String keyword);

    /**
     * 创建用户
     *
     */
    void addUser(String clusterId, String namespace, String middlewareName, MiddlewareUserDto middlewareUserDto);

    /**
     * 删除用户
     *
     */
    void dropUser(String clusterId, String namespace, String middlewareName, String username);

    /**
     * 赋权用户
     *
     */
    void grantUser(String clusterId, String namespace, String middlewareName, String username,
                   MiddlewareUserAuthority middlewareUserAuthority);

    /**
     * 赋权用户
     *
     */
    void revokeUser(String clusterId, String namespace, String middlewareName, MiddlewareUserDto middlewareUserDto);

    /**
     * 获取用户权限
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param username 用户名
     * @param oid      oid
     * @return List<MiddlewareUserAuthority>
     */
    List<MiddlewareUserAuthority> userAuthority(String clusterId, String namespace, String middlewareName, String username, String oid);

    /**
     * 重置密码
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param username 用户名
     */
    void resetPassword(String clusterId, String namespace, String middlewareName, String username);

    /**
     * 修改密码
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param username 用户名
     * @param password 密码
     */
    void updatePassword(String clusterId, String namespace, String middlewareName, String username, String password);

    /**
     * 启用/禁用用户
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param username 用户名
     */
    void enableUser(String clusterId, String namespace, String middlewareName, String username, Boolean enable);

    /**
     * 查询encoding
     *
     */
    List<String> listEncoding();

    /**
     * 查询encoding
     *
     */
    List<String> listDataType();

    /**
     * 查询encoding
     *
     */
    List<String> listCollate();

}
