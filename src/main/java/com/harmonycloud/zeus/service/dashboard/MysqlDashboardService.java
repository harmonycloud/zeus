package com.harmonycloud.zeus.service.dashboard;

import com.harmonycloud.caas.common.model.dashboard.mysql.*;

import java.util.List;

/**
 * @description mysql控制台
 * @author  liyinlong
 * @since 2022/10/13 8:29 PM
 */
public interface MysqlDashboardService extends BaseMiddlewareApiService {

    /**
     * 获取database列表
     */
    List<DatabaseDto> listDatabases(String clusterId, String namespace, String middlewareName);

    /**
     * 创建数据库
     */
    void createDatabase(String clusterId,String namespace, String middlewareName,DatabaseDto databaseDto);

    /**
     * 修改数据库
     */
    void updateDatabase(String clusterId, String namespace, String middlewareName, DatabaseDto databaseDto);

    /**
     * 删除数据库
     */
    void dropDatabase(String clusterId,String namespace, String middlewareName,String databaseName);

    /**
     * 查询字符集
     */
    List<String> listCharset(String clusterId, String namespace, String middlewareName);

    /**
     * 查询字符集校验规则
     */
    List<String> listCharsetCollation(String clusterId, String namespace, String middlewareName, String charset);

    /**
     * 查询数据表引擎
     */
    List<String> listEngines(String clusterId, String namespace, String middlewareName);

    /**
     * 查询数据库数据表
     */
    List<TableDto> listTables(String clusterId, String namespace, String middlewareName, String database);

    /**
     * 创建表
     */
    void createTable(String clusterId,String namespace, String middlewareName,String database,TableDto databaseDto);

    /**
     * 删除数据表
     */
    void dropTable(String clusterId,String namespace, String middlewareName,String database,String table);

    /**
     * 获取表详情
     */
    TableDto showTableDetail(String clusterId,String namespace, String middlewareName,String database,String table);

    /**
     * 查询指定数据表所有列
     */
    List<ColumnDto> listTableColumns(String clusterId, String namespace, String middlewareName, String database, String table);

    /**
     * 查询指定数据表所有索引
     */
    List<IndexDto> listTableIndices(String clusterId, String namespace, String middlewareName, String database, String table);

    /**
     * 保存数据表索引信息
     */
    void saveTableIndex(String clusterId, String namespace, String middlewareName, String database, String table, List<IndexDto> indexDtoList);

    /**
     * 查询指定数据表所有外键
     */
    List<ForeignKeyDto> listTableForeignKeys(String clusterId, String namespace, String middlewareName, String database, String table);

    /**
     * 保存所有外键
     */
    void saveTableForeignKey(String clusterId, String namespace, String middlewareName, String database, String table, List<ForeignKeyDto> foreignKeyDtos);

    /**
     * 获取user列表
     */
    List<UserDto> listUser(String clusterId, String namespace, String middlewareName, String keyword);

    /**
     * 创建用户
     *
     */
    void addUser(String clusterId, String namespace, String middlewareName, UserDto userDto);

    /**
     * 删除用户
     *
     */
    void dropUser(String clusterId, String namespace, String middlewareName, String username);

    /**
     * 修改用户
     */
    void updateUser(String clusterId, String namespace, String middlewareName, String username, UserDto userDto);

    /**
     * 更新用户名
     */
    void updateUsername(String clusterId, String namespace, String middlewareName, String username,UserDto userDto);

    /**
     * 更新密码
     */
    void updatePassword(String clusterId, String namespace, String middlewareName, String username, UserDto userDto);

    /**
     * 锁定用户
     */
    void lockUser(String clusterId, String namespace, String middlewareName, String username);

    /**
     * 解锁用户
     */
    void unLockUser(String clusterId, String namespace, String middlewareName, String username);

    /**
     * 授权数据库
     */
    void grantDatabasePrivilege(String clusterId, String namespace, String middlewareName, String database, GrantOptionDto grantOptionDto);

    /**
     * 授权数据表
     */
    void grantTablePrivilege(String clusterId, String namespace, String middlewareName, String database, String table, GrantOptionDto grantOptionDto);

    /**
     * 释放权限
     */
    void revokePrivilege(String clusterId, String namespace, String middlewareName, String username, List<GrantOptionDto> grantOptionDtos);

    /**
     * 释放数据库权限
     */
    void revokeDatabasePrivilege(String clusterId, String namespace, String middlewareName, String database, GrantOptionDto grantOptionDto);

    /**
     * 释放数据表权限
     */
    void revokeTablePrivilege(String clusterId, String namespace, String middlewareName, String database, String table, GrantOptionDto grantOptionDto);

    /**
     * 查询用户权限
     */
    List<GrantOptionDto> listUserAuthority(String clusterId, String namespace, String middlewareName, String username);

    /**
     * 判断用户是否存在，存在则返回true
     */
    boolean checkUserExists(String namespace, String middlewareName, String username);

}
