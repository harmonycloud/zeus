package com.harmonycloud.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.MysqlEngineEnum;
import com.harmonycloud.caas.common.enums.MysqlPrivilegeEnum;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.enums.middleware.MysqlDataTypeEnum;
import com.harmonycloud.caas.common.enums.middleware.MysqlOperationEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.dashboard.ExecResult;
import com.harmonycloud.caas.common.model.dashboard.SqlQuery;
import com.harmonycloud.caas.common.model.dashboard.mysql.*;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.bean.BeanSqlExecuteRecord;
import com.harmonycloud.zeus.dao.BeanSqlExecuteRecordMapper;
import com.harmonycloud.zeus.integration.dashboard.MysqlClient;
import com.harmonycloud.zeus.service.dashboard.MysqlDashboardService;
import com.harmonycloud.zeus.util.ExcelUtil;
import com.harmonycloud.zeus.util.FileDownloadUtil;
import com.harmonycloud.zeus.util.MysqlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/10/13 8:34 PM
 */
@Slf4j
@Service
@Operator(paramTypes4One = String.class)
public class MysqlDashboardServiceImpl implements MysqlDashboardService {

    @Autowired
    private MysqlClient mysqlClient;
    
    @Value("${system.middleware-api.mysql.port:3306}")
    private String port;

    @Value("${system.middleware-api.mysql.excelPath:/usr/local/zeus-pv/excel/}")
    private String path;

    @Value("${system.middleware-api.mysql.temppath:10.10.102.52}")
    private String temppath;

    @Autowired
    private BeanSqlExecuteRecordMapper sqlExecuteRecordMapper;

    @Override
    public String login(String clusterId, String namespace, String middlewareName, String username, String password) {
        JSONObject res = mysqlClient.login(getPath(middlewareName,namespace), port, username, password);
        if (res.getJSONObject("err") != null) {
            throw new BusinessException(ErrorMessage.MYSQL_LOGIN_FAILED, res.getString("message"));
        }
        return res.getString("data");
    }

    @Override
    public boolean support(String type) {
        return MiddlewareTypeEnum.MYSQL.getType().equals(type);
    }

    @Override
    public List<MysqlDataType> listDataType() {
        return MysqlDataTypeEnum.dataTypeList;
    }

    @Override
    public List<DatabaseDto> listDatabases(String clusterId, String namespace, String middlewareName) {
        JSONArray dataAry = mysqlClient.listDatabases(getPath(middlewareName,namespace), port).getJSONArray("dataAry");
        return dataAry.stream().map(data -> {
            JSONObject obj = (JSONObject) data;
            DatabaseDto databaseDto = new DatabaseDto();
            databaseDto.setDb(obj.getString("SCHEMA_NAME"));
            databaseDto.setCharacter(obj.getString("DEFAULT_CHARACTER_SET_NAME"));
            databaseDto.setCollate(obj.getString("DEFAULT_COLLATION_NAME"));
            return databaseDto;
        }).collect(Collectors.toList());
    }

    @Override
    public void createDatabase(String clusterId, String namespace, String middlewareName, DatabaseDto databaseDto) {
        if(checkDatabaseExists(namespace, middlewareName,  databaseDto.getDb())){
            throw new BusinessException(ErrorMessage.DATABASE_EXISTS);
        }
        JSONObject res = mysqlClient.createDatabase(getPath(middlewareName,namespace), port, databaseDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.CREATE_DATABASE_FAILED, res.getString("message"));
        }
    }

    @Override
    public void updateDatabase(String clusterId, String namespace, String middlewareName, DatabaseDto databaseDto) {
        JSONObject res = mysqlClient.alterDatabase(getPath(middlewareName,namespace), port, databaseDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.ALTER_DATABASE_FAILED, res.getString("message"));
        }
    }

    @Override
    public void dropDatabase(String clusterId, String namespace, String middlewareName, String databaseName) {
        JSONObject res = mysqlClient.dropDatabase(getPath(middlewareName,namespace), port, databaseName);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.DELETE_TABLE_FAILED, res.getString("message"));
        }
    }

    @Override
    public List<String> listCharset(String clusterId, String namespace, String middlewareName) {
        JSONArray dataAry = mysqlClient.listCharsets(getPath(middlewareName,namespace), port).getJSONArray("dataAry");
        return dataAry.stream().map(data -> {
            JSONObject obj = (JSONObject) data;
            return obj.getString("Charset");
        }).sorted().collect(Collectors.toList());
    }

    @Override
    public List<String> listCharsetCollation(String clusterId, String namespace, String middlewareName, String charset) {
        JSONArray dataAry = mysqlClient.listCharsetCollations(getPath(middlewareName,namespace), port, charset).getJSONArray("dataAry");
        return dataAry.stream().map(data -> {
            JSONObject obj = (JSONObject) data;
            return obj.getString("Collation");
        }).sorted().collect(Collectors.toList());
    }

    @Override
    public List<MysqlEngineDto> listEngines(String clusterId, String namespace, String middlewareName) {
        JSONArray dataAry = mysqlClient.listEngines(getPath(middlewareName, namespace), port).getJSONArray("dataAry");
        return dataAry.stream().map(data -> {
            JSONObject obj = (JSONObject) data;
            String engine = obj.getString("ENGINE");
            MysqlEngineEnum mysqlEngineEnum = MysqlEngineEnum.findByEngineName(engine);
            MysqlEngineDto mysqlEngineDto = new MysqlEngineDto();
            if (mysqlEngineEnum != null) {
                mysqlEngineDto.setEngine(engine);
                mysqlEngineDto.setIndexTypes(Arrays.asList(mysqlEngineEnum.getIndexTypes()));
                mysqlEngineDto.setStorageTypes(Arrays.asList(mysqlEngineEnum.getStorageTypes()));
                mysqlEngineDto.setSupportForeignKey(mysqlEngineEnum.getSupportForeignKey());
            }
            return mysqlEngineDto;
        }).filter(mysqlEngineDto -> !StringUtils.isEmpty(mysqlEngineDto.getEngine())).sorted().collect(Collectors.toList());
    }

    @Override
    public List<TableDto> listTables(String clusterId, String namespace, String middlewareName, String database) {
        JSONArray dataAry = mysqlClient.listTables(getPath(middlewareName,namespace), port, database).getJSONArray("dataAry");
        return dataAry.stream().map(data -> {
            JSONObject obj = (JSONObject) data;
            TableDto tableDto = new TableDto();
            tableDto.setTableName(obj.getString("TABLE_NAME"));
            tableDto.setCollate(obj.getString("TABLE_COLLATION"));
            tableDto.setCharset(MysqlUtil.extractCharset(obj.getString("TABLE_COLLATION")));
            return tableDto;
        }).collect(Collectors.toList());
    }

    @Override
    public void createTable(String clusterId, String namespace, String middlewareName, String database, TableDto tableDto) {
        JSONObject res = mysqlClient.createTable(getPath(middlewareName,namespace), port, database, tableDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.CREATE_TABLE_FAILED, res.getString("message"));
        }
    }

    @Override
    public void updateTableOptions(String clusterId,String namespace, String middlewareName,String database,String table,TableDto tableDto) {
        JSONObject res = mysqlClient.updateTableOptions(getPath(middlewareName,namespace), port, database, table,tableDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.ALTER_TABLE_FAILED, res.getString("message"));
        }
    }

    @Override
    public void dropTable(String clusterId, String namespace, String middlewareName, String database, String table) {
        JSONObject res = mysqlClient.dropTable(getPath(middlewareName,namespace), port, database, table);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.CREATE_DATABASE_FAILED, res.getString("message"));
        }
    }

    @Override
    public void updateTableName(String clusterId, String namespace, String middlewareName, String database, String table, TableDto tableDto) {
        JSONObject res = mysqlClient.renameTable(getPath(middlewareName, namespace), port, database, table, tableDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.ALTER_TABLE_FAILED, res.getString("message"));
        }
    }

    @Override
    public TableDto showTableDetail(String clusterId, String namespace, String middlewareName, String database, String table) {
        JSONArray dataAry = mysqlClient.showTableOptions(getPath(middlewareName,namespace), port, database, table).getJSONArray("dataAry");
        TableDto tableDto = new TableDto();
        if (CollectionUtils.isEmpty(dataAry)) {
            throw new BusinessException(ErrorMessage.OBTAIN_TABLE_DETAIL_FAILED);
        }
        JSONObject tableObj = dataAry.getJSONObject(0);
        log.info(tableObj.toJSONString());
        tableDto.setTableName(table);
        tableDto.setComment(tableObj.getString("TABLE_COMMENT"));
        tableDto.setCollate(tableObj.getString("TABLE_COLLATION"));
        tableDto.setCharset(MysqlUtil.extractCharset(tableObj.getString("TABLE_COLLATION")));
        tableDto.setAutoIncrement(tableObj.getInteger("AUTO_INCREMENT"));
        // TODO 获取minrows、maxrows
        tableDto.setMinRows(tableObj.getInteger(""));
        tableDto.setMaxRows(tableObj.getInteger(""));
        tableDto.setEngine(tableObj.getString("ENGINE"));
        // 设置列、索引、外键信息
        tableDto.setColumns(listTableColumns(clusterId, namespace, middlewareName, database, table));
        tableDto.setIndices(listTableIndices(clusterId, namespace, middlewareName, database, table));
        tableDto.setForeignKeys(listTableForeignKeys(clusterId, namespace, middlewareName, database, table));
        return tableDto;
    }

    @Override
    public JSONArray showTableData(String clusterId, String namespace, String middlewareName, String database, String table,QueryInfo queryInfo) {
        JSONArray dataAry = mysqlClient.showTableData(getPath(middlewareName, namespace), port, database, table, queryInfo).getJSONArray("dataAry");
        if (CollectionUtils.isEmpty(dataAry)) {
            throw new BusinessException(ErrorMessage.FAILED_TO_OBTAIN_TABLE_DATA);
        }
        return dataAry;
    }

    @Override
    public int getTableRecord(String clusterId, String namespace, String middlewareName, String database, String table) {
        JSONArray dataAry = mysqlClient.getTableRecord(getPath(middlewareName,namespace), port, database, table).getJSONArray("dataAry");
        if (CollectionUtils.isEmpty(dataAry)) {
            throw new BusinessException(ErrorMessage.FAILED_TO_OBTAIN_TABLE_RECORD);
        }
        JSONObject tableObj = dataAry.getJSONObject(0);
        return tableObj.getInteger("num");
    }

    @Override
    public List<ColumnDto> listTableColumns(String clusterId, String namespace, String middlewareName, String database, String table) {
        JSONArray dataAry = mysqlClient.listTableColumns(getPath(middlewareName,namespace), port, database, table).getJSONArray("dataAry");
        return dataAry.stream().map(data -> {
            JSONObject obj = (JSONObject) data;
            ColumnDto columnDto = new ColumnDto();
            columnDto.setColumn(obj.getString("COLUMN_NAME"));
            columnDto.setDataType(obj.getString("DATA_TYPE"));
            columnDto.setColumnType(obj.getString("COLUMN_TYPE"));
            columnDto.setComment(obj.getString("COLUMN_COMMENT"));
            columnDto.setNullable(MysqlUtil.convertColumnNullable(obj.getString("IS_NULLABLE")));
            columnDto.setColumnDefault(obj.getString("COLUMN_DEFAULT"));
            columnDto.setPrimary(MysqlUtil.convertColumnPrimary(obj.getString("COLUMN_KEY")));
            columnDto.setAutoIncrement(MysqlUtil.convertAutoIncrement(obj.getString("EXTRA")));
            columnDto.setSize(MysqlUtil.convertColumnDataSize(columnDto.getColumnType()));
            columnDto.setCollate(obj.getString("COLLATION_NAME"));
            columnDto.setCharset(MysqlUtil.extractCharset(columnDto.getCollate()));
            return columnDto;
        }).collect(Collectors.toList());
    }

    @Override
    public void saveTableColumn(String clusterId, String namespace, String middlewareName, String database, String table, TableDto tableDto) {
        List<ColumnDto> columnDtoList = tableDto.getColumns();
        if (CollectionUtils.isEmpty(columnDtoList)) {
            return;
        }
        this.correctColumn(columnDtoList);

        List<ColumnDto> oldColumns = listTableColumns(clusterId, namespace, middlewareName, database, table);
        Map<String, ColumnDto> oldColumnMap = new HashMap<>();
        oldColumns.forEach(tableColumn -> oldColumnMap.put(tableColumn.getColumn(), tableColumn));

        Map<String, ColumnDto> columnMap = new HashMap<>();
        columnDtoList.forEach(tableColumn -> columnMap.put(tableColumn.getColumn(), tableColumn));

        List<ColumnDto> newColumnList = new ArrayList<>();
        // 找出要新增或修改的列
        for (ColumnDto newColumnDto : columnDtoList) {
            ColumnDto oldColumnDto = oldColumnMap.get(newColumnDto.getColumn());
            if (oldColumnDto == null) {
                // 新增列
                newColumnDto.setAction(MysqlOperationEnum.ADD.getCode());
                newColumnList.add(newColumnDto);
                continue;
            }
            if (!newColumnDto.equals(oldColumnDto)) {
                if (!StringUtils.isEmpty(newColumnDto.getNewColumn())) {
                    // 修改(改列名)
                    newColumnDto.setAction(MysqlOperationEnum.CHANGE.getCode());
                } else {
                    // 修改(不改列名)
                    newColumnDto.setAction(MysqlOperationEnum.MODIFY.getCode());
                }
                newColumnList.add(newColumnDto);
            }
        }
        // 找出要删除的列
        for (ColumnDto oldColumn : oldColumns) {
            ColumnDto columnDto = columnMap.get(oldColumn.getColumn());
            if (columnDto == null) {
                oldColumn.setAction(MysqlOperationEnum.DROP.getCode());
                newColumnList.add(oldColumn);
            }
        }
        // 设置主键操作类型
        List<String> oldPrimaryKeys = extractPrimaryKey(oldColumns);
        List<String> newPrimaryKeys = extractPrimaryKey(columnDtoList);
        tableDto.setPrimaryKeyAction(getPrimaryKeyActionCode(oldPrimaryKeys, newPrimaryKeys));

        if (!CollectionUtils.isEmpty(newColumnList)) {
            tableDto.setColumns(newColumnList);
            JSONObject res = mysqlClient.saveTableColumns(getPath(middlewareName, namespace), port, database, table, tableDto);
            if (!res.getBoolean("success")) {
                throw new BusinessException(ErrorMessage.ALTER_TABLE_COLUMN_FAILED, res.getString("message"));
            }
        }
    }

    @Override
    public List<IndexDto> listTableIndices(String clusterId, String namespace, String middlewareName, String database, String table) {
        JSONArray dataAry = mysqlClient.listTableIndices(getPath(middlewareName, namespace), port, database, table).getJSONArray("dataAry");
        Map<String, IndexDto> indexDtoMap = extractTableIndex(clusterId, namespace, middlewareName, database, table);

        Map<String, IndexDto> indexMap = new HashMap<>();
        dataAry.forEach(data -> {
            JSONObject obj = (JSONObject) data;
            IndexColumnDto indexColumnDto = new IndexColumnDto();
            indexColumnDto.setKeyName(obj.getString("Key_name"));
            indexColumnDto.setColumnName(obj.getString("Column_name"));
            indexColumnDto.setSubPart(obj.getInteger("Sub_part"));
            indexColumnDto.setIndexComment(obj.getString("Index_comment"));

            IndexDto indexDto = indexDtoMap.get(indexColumnDto.getKeyName());
            if (indexMap.containsKey(indexDto.getIndex())) {
                IndexDto tempIndexDto = indexMap.get(indexDto.getIndex());
                tempIndexDto.getIndexColumns().add(indexColumnDto);
            } else {
                indexDto.setComment(indexColumnDto.getIndexComment());
                List<IndexColumnDto> columnDtoList = new ArrayList<>();
                columnDtoList.add(indexColumnDto);
                indexDto.setIndexColumns(columnDtoList);
                indexMap.put(indexDto.getIndex(), indexDto);
            }
        });
        return new ArrayList<>(indexMap.values());
    }

    @Override
    public void saveTableIndex(String clusterId, String namespace, String middlewareName, String database, String table, List<IndexDto> indexDtoList) {
        List<IndexDto> oldIndices = listTableIndices(clusterId, namespace, middlewareName, database, table);
        Map<String, IndexDto> oldIndexMap = new HashMap<>();
        oldIndices.forEach(indexDto -> oldIndexMap.put(indexDto.getIndex(), indexDto));

        Map<String, IndexDto> indexMap = new HashMap<>();
        indexDtoList.forEach(indexDto -> indexMap.put(indexDto.getIndex(), indexDto));

        List<IndexDto> newIndexList = new ArrayList<>();
        // 找出要删除的
        oldIndices.forEach(oldIndex -> {
            IndexDto indexDto = indexMap.get(oldIndex.getIndex());
            if (indexDto == null) {
                oldIndex.setAction(MysqlOperationEnum.DROP.getCode());
                newIndexList.add(oldIndex);
            }
        });
        // 找出要添加的
        for (IndexDto indexDto : indexDtoList) {
            IndexDto oldIndex = oldIndexMap.get(indexDto.getIndex());
            if (oldIndex == null) {
                // 添加新索引
                indexDto.setAction(MysqlOperationEnum.ADD.getCode());
                newIndexList.add(indexDto);
            } else if (!indexDto.equals(oldIndex)) {
                // 修改索引
                indexDto.setAction(MysqlOperationEnum.MODIFY.getCode());
                newIndexList.add(indexDto);
            }
        }

        JSONObject res = mysqlClient.saveTableIndices(getPath(middlewareName, namespace), port, database, table, newIndexList);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.ALTER_TABLE_INDICES_FAILED, res.getString("message"));
        }
    }

    @Override
    public List<ForeignKeyDto> listTableForeignKeys(String clusterId, String namespace, String middlewareName, String database, String table) {
        JSONArray dataAry = mysqlClient.listTableForeignKeys(getPath(middlewareName, namespace), port, database, table).getJSONArray("dataAry");
        Map<String, ForeignKeyDto> foreignKeyDtoMap = new HashMap<>();
        dataAry.forEach(data -> {
            JSONObject obj = (JSONObject) data;
            ForeignKeyDetailDto foreignKeyDetailDto = new ForeignKeyDetailDto();
            foreignKeyDetailDto.setForeignKey(obj.getString("CONSTRAINT_NAME"));
            foreignKeyDetailDto.setReferenceDatabase(obj.getString("REFERENCED_TABLE_SCHEMA"));
            foreignKeyDetailDto.setReferenceTable(obj.getString("REFERENCED_TABLE_NAME"));
            foreignKeyDetailDto.setColumn(obj.getString("COLUMN_NAME"));
            foreignKeyDetailDto.setReferencedColumn(obj.getString("REFERENCED_COLUMN_NAME"));

            ForeignKeyDto foreignKeyDto = new ForeignKeyDto();
            foreignKeyDto.setForeignKey(foreignKeyDetailDto.getForeignKey());
            foreignKeyDto.setReferenceDatabase(foreignKeyDetailDto.getReferenceDatabase());
            foreignKeyDto.setReferenceTable(foreignKeyDetailDto.getReferenceTable());
            if (foreignKeyDtoMap.containsKey(foreignKeyDto.getForeignKey())) {
                ForeignKeyDto tempForeignKeyDto = foreignKeyDtoMap.get(foreignKeyDto.getForeignKey());
                tempForeignKeyDto.getDetails().add(foreignKeyDetailDto);
            } else {
                foreignKeyDto.setOnDeleteOption(obj.getString("DELETE_RULE"));
                foreignKeyDto.setOnUpdateOption(obj.getString("UPDATE_RULE"));
                List<ForeignKeyDetailDto> columnDtoList = new ArrayList<>();
                columnDtoList.add(foreignKeyDetailDto);
                foreignKeyDto.setDetails(columnDtoList);
                foreignKeyDtoMap.put(foreignKeyDto.getForeignKey(), foreignKeyDto);
            }
        });
        return new ArrayList<>(foreignKeyDtoMap.values());
    }

    @Override
    public void saveTableForeignKey(String clusterId, String namespace, String middlewareName, String database, String table, List<ForeignKeyDto> foreignKeyDtos) {
        List<ForeignKeyDto> oldForeignKeys = listTableForeignKeys(clusterId, namespace, middlewareName, database, table);
        Map<String, ForeignKeyDto> oldForeignKeyMap = new HashMap<>();
        oldForeignKeys.forEach(foreignKeyDto -> oldForeignKeyMap.put(foreignKeyDto.getForeignKey(), foreignKeyDto));

        Map<String, ForeignKeyDto> foreignKeyMap = new HashMap<>();
        foreignKeyDtos.forEach(foreignKeyDto -> foreignKeyMap.put(foreignKeyDto.getForeignKey(), foreignKeyDto));

        List<ForeignKeyDto> newForeignKeyList = new ArrayList<>();
        // 找出要删除的
        for (ForeignKeyDto oldForeignKey : oldForeignKeys) {
            ForeignKeyDto foreignKeyDto = foreignKeyMap.get(oldForeignKey.getForeignKey());
            if (foreignKeyDto == null) {
                oldForeignKey.setAction(MysqlOperationEnum.DROP.getCode());
                newForeignKeyList.add(oldForeignKey);
            }
        }
        // 找出要添加或修改的
        for (ForeignKeyDto foreignKeyDto : foreignKeyDtos) {
            ForeignKeyDto oldForeignKey = oldForeignKeyMap.get(foreignKeyDto.getForeignKey());
            if (oldForeignKey == null) {
                // 添加新外键
                foreignKeyDto.setAction(MysqlOperationEnum.ADD.getCode());
                newForeignKeyList.add(foreignKeyDto);
                continue;
            }
            if (!foreignKeyDto.equals(oldForeignKey)) {
                // 修改外键，修改步骤为先删除，再创建
                oldForeignKey.setAction(MysqlOperationEnum.DROP.getCode());
                foreignKeyDto.setAction(MysqlOperationEnum.ADD.getCode());
                newForeignKeyList.add(oldForeignKey);
                newForeignKeyList.add(foreignKeyDto);
            }
        }

        if (!CollectionUtils.isEmpty(newForeignKeyList)) {
            JSONObject res = mysqlClient.saveTableForeignKeys(getPath(middlewareName, namespace), port, database, table, newForeignKeyList);
            if (!res.getBoolean("success")) {
                throw new BusinessException(ErrorMessage.ALTER_TABLE_FOREIGN_KEYS_FAILED, res.getString("message"));
            }
        }
    }

    @Override
    public List<UserDto> listUser(String clusterId, String namespace, String middlewareName, String keyword) {
        JSONArray dataAry = mysqlClient.listUser(getPath(middlewareName, namespace), port).getJSONArray("dataAry");
        return dataAry.stream().map(data -> {
            JSONObject obj = (JSONObject) data;
            UserDto userDto = new UserDto();
            userDto.setUser(obj.getString("User"));
            userDto.setGrantAble(MysqlUtil.convertGrantPriv(obj.getString("Grant_priv")));
            userDto.setAccountLocked(MysqlUtil.convertGrantPriv(obj.getString("account_locked")));
            return userDto;
        }).filter(userDto -> {
            if (!StringUtils.isEmpty(keyword)) {
                return userDto.getUser().contains(keyword);
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public void addUser(String clusterId, String namespace, String middlewareName, UserDto userDto) {
        if (checkUserExists(namespace, middlewareName, userDto.getUser())) {
            throw new BusinessException(ErrorMessage.MYSQL_USER_EXISTS);
        }
        JSONObject res = mysqlClient.createUser(getPath(middlewareName, namespace), port, userDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.CREATE_MYSQL_USER_FAILED, res.getString("message"));
        } else {
            if (userDto.isGrantAble()) {
                GrantOptionDto grantOptionDto = new GrantOptionDto();
                grantOptionDto.setPrivilegeType(MysqlPrivilegeEnum.GLOBAL_GRANT.getAuthority());
                grantOptionDto.setGrantAble(false);
                grantOptionDto.setUsername(userDto.getUser());
                grantOptionDto.setDb("*");
                grantDatabasePrivilege(clusterId, namespace, middlewareName, "*", grantOptionDto);
            }
        }
    }

    @Override
    public void dropUser(String clusterId, String namespace, String middlewareName, String username) {
        if (!checkUserExists(namespace, middlewareName, username)) {
            throw new BusinessException(ErrorMessage.MYSQL_USER_NOT_EXISTS);
        }
        JSONObject res = mysqlClient.dropUser(getPath(middlewareName,namespace), port, username);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.DELETE_MYSQL_USER_FAILED, res.getString("message"));
        }
    }

    @Override
    public void updateUser(String clusterId, String namespace, String middlewareName, String username, UserDto userDto) {
        if (!checkUserExists(namespace, middlewareName, username)) {
            throw new BusinessException(ErrorMessage.MYSQL_USER_NOT_EXISTS);
        }
        if (!StringUtils.isEmpty(userDto.getPassword())) {
            this.updatePassword(clusterId, namespace, middlewareName, username, userDto);
        }
        if (!StringUtils.isEmpty(userDto.getNewUser())) {
            this.updateUsername(clusterId, namespace, middlewareName, username, userDto);
        }
    }

    @Override
    public void updateUsername(String clusterId, String namespace, String middlewareName, String username, UserDto userDto) {
        JSONObject res = mysqlClient.updateUsername(getPath(middlewareName, namespace), port, username, userDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.FAILED_TO_UPDATE_USER, res.getString("message"));
        }
    }

    @Override
    public void updatePassword(String clusterId, String namespace, String middlewareName, String username, UserDto userDto) {
        if (!checkUserExists(namespace, middlewareName, username)) {
            throw new BusinessException(ErrorMessage.MYSQL_USER_NOT_EXISTS);
        }
        JSONObject res = mysqlClient.updatePassword(getPath(middlewareName,namespace), port, username, userDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.FAILED_TO_UPDATE_USER_PASSWORD, res.getString("message"));
        }
    }

    @Override
    public void lockUser(String clusterId, String namespace, String middlewareName, String username) {
        JSONObject res = mysqlClient.lockUser(getPath(middlewareName,namespace), port, username);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.LOCK_USER_FAILED, res.getString("message"));
        }
    }

    @Override
    public void unLockUser(String clusterId, String namespace, String middlewareName, String username) {
        JSONObject res = mysqlClient.unlockUser(getPath(middlewareName,namespace), port, username);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.UNLOCK_USER_FAILED, res.getString("message"));
        }
    }

    @Override
    public void grantDatabasePrivilege(String clusterId, String namespace, String middlewareName, String database, GrantOptionDto grantOptionDto) {
        grantOptionDto.setPrivilege(MysqlPrivilegeEnum.findDbPrivilege(grantOptionDto.getPrivilegeType(), grantOptionDto.getGrantAble()));
        JSONObject res = mysqlClient.grantDatabase(getPath(middlewareName, namespace), port, database, grantOptionDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.GRANT_DATABASE_FAILED, res.getString("message"));
        }
    }

    @Override
    public void grantTablePrivilege(String clusterId, String namespace, String middlewareName, String database, String table, GrantOptionDto grantOptionDto) {
        grantOptionDto.setPrivilege(MysqlPrivilegeEnum.findTablePrivilege(grantOptionDto.getPrivilegeType(), grantOptionDto.getGrantAble()));
        JSONObject res = mysqlClient.grantTable(getPath(middlewareName, namespace), port, database, table, grantOptionDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.GRANT_TABLE_FAILED, res.getString("message"));
        }
    }

    @Override
    public void revokePrivilege(String clusterId, String namespace, String middlewareName, String username, List<GrantOptionDto> grantOptionDtos) {
        grantOptionDtos.forEach(grantOptionDto -> {
            grantOptionDto.setUsername(username);
            // 如果table为空，则执行释放数据库权限,否则执行释放数据表权限
            if (StringUtils.isEmpty(grantOptionDto.getTable())) {
                revokeDatabasePrivilege(clusterId, namespace, middlewareName, grantOptionDto.getDb(), grantOptionDto);
            } else {
                revokeTablePrivilege(clusterId, namespace, middlewareName, grantOptionDto.getDb(), grantOptionDto.getTable(), grantOptionDto);
            }
        });
    }

    @Override
    public void revokeDatabasePrivilege(String clusterId, String namespace, String middlewareName, String database, GrantOptionDto grantOptionDto) {
        JSONObject res = mysqlClient.revokeDatabasePrivilege(getPath(middlewareName, namespace), port, database, grantOptionDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.REVOKE_DATABASE_FAILED, res.getString("message"));
        }
    }

    @Override
    public void revokeTablePrivilege(String clusterId, String namespace, String middlewareName, String database, String table, GrantOptionDto grantOptionDto) {
        JSONObject res = mysqlClient.revokeTablePrivilege(getPath(middlewareName, namespace), port, database, table, grantOptionDto);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.REVOKE_TABLE_FAILED, res.getString("message"));
        }
    }

    @Override
    public List<GrantOptionDto> listUserAuthority(String clusterId, String namespace, String middlewareName, String username) {
        JSONArray privilegeAry = new JSONArray();
        JSONArray databasePrivilegeAry = mysqlClient.showDatabasePrivilege(getPath(middlewareName, namespace), port, username).getJSONArray("dataAry");
        JSONArray tablePrivilegeAry = mysqlClient.showTablePrivilege(getPath(middlewareName, namespace), port, username).getJSONArray("dataAry");
        if (!CollectionUtils.isEmpty(databasePrivilegeAry)) {
            privilegeAry.addAll(databasePrivilegeAry);
        }
        if (!CollectionUtils.isEmpty(tablePrivilegeAry)) {
            privilegeAry.addAll(tablePrivilegeAry);
        }
        // todo 返回的结果如何排序？新的授权记录排在后面怎么办？
        return privilegeAry.stream().map(privilege -> {
            JSONObject obj = (JSONObject) privilege;
            GrantOptionDto grantOptionDto = new GrantOptionDto();
            grantOptionDto.setDb(obj.getString("TABLE_SCHEMA"));
            grantOptionDto.setTable(obj.getString("TABLE_NAME"));
            grantOptionDto.setPrivilege(obj.getString("PRIVILEGE"));
            grantOptionDto.setGrantAble(MysqlUtil.convertGrantAble(obj.getString("IS_GRANTABLE")));
            // todo 此处需将权限字符串转为权限类型编号
            grantOptionDto.setPrivilegeType(1);
            return grantOptionDto;
        }).collect(Collectors.toList());
    }

    @Override
    public byte[] exportTableSql(String clusterId, String namespace, String middlewareName, String database, String table, HttpServletRequest request, HttpServletResponse response) {
        try {
            JSONArray dataAry = mysqlClient.showTableScript(getPath(middlewareName, namespace), port, database, table).getJSONArray("dataAry");
            if (CollectionUtils.isEmpty(dataAry)) {
                throw new BusinessException(ErrorMessage.FAILED_TO_EXPORT_TABLE_SQL);
            }
            JSONObject obj = dataAry.getJSONObject(0);
            String sql = obj.getString("Create Table");

            String fileRealName = table + ".sql";
            byte[] sqlBytes = sql.getBytes(StandardCharsets.UTF_8);
            return sqlBytes;
//            FileDownloadUtil.downloadFile(request, response, fileRealName, sqlBytes);
        } catch (Exception e) {
            throw new BusinessException(ErrorMessage.FAILED_TO_EXPORT_TABLE_SQL);
        }
    }

    @Override
    public String showTableSql(String clusterId, String namespace, String middlewareName, String database, String table) {
        JSONArray dataAry = mysqlClient.showTableScript(getPath(middlewareName, namespace), port, database, table).getJSONArray("dataAry");
        if (CollectionUtils.isEmpty(dataAry)) {
            throw new BusinessException(ErrorMessage.FAILED_TO_EXPORT_TABLE_SQL);
        }
        JSONObject obj = dataAry.getJSONObject(0);
        return obj.getString("Create Table");
    }

    @Override
    public void exportTableExcel(String clusterId, String namespace, String middlewareName, String database, String table, HttpServletRequest request, HttpServletResponse response) {
        try {
            List<ColumnDto> columnDtos = listTableColumns(clusterId, namespace, middlewareName, database, table);
            String excelFilePath = ExcelUtil.createTableExcel(path, table, columnDtos);
            String fileRealName = table + ".xlsx";
            FileDownloadUtil.downloadFile(request, response, fileRealName, excelFilePath);
        } catch (Exception e) {
            log.error("导出表结构Excel文件失败", e);
            throw new BusinessException(ErrorMessage.FAILED_TO_EXPORT_TABLE_EXCEL);
        }
    }

    @Override
    public void exportDatabaseSql(String clusterId, String namespace, String middlewareName, String database, HttpServletRequest request, HttpServletResponse response) {
        List<TableDto> tableDtoList = listTables(clusterId, namespace, middlewareName, database);
        try {
            StringBuilder dbSqlBuf = new StringBuilder();
            for (TableDto tableDto : tableDtoList) {
                JSONArray dataAry = mysqlClient.showTableScript(getPath(middlewareName, namespace), port, database, tableDto.getTableName()).getJSONArray("dataAry");
                if (CollectionUtils.isEmpty(dataAry)) {
                    throw new BusinessException(ErrorMessage.FAILED_TO_EXPORT_TABLE_SQL);
                }
                JSONObject obj = dataAry.getJSONObject(0);
                dbSqlBuf.append(obj.getString("Create Table"));
                dbSqlBuf.append(";");
                dbSqlBuf.append("\n\n");
            }
            String fileRealName = database + ".sql";
            byte[] sqlBytes = dbSqlBuf.toString().getBytes(StandardCharsets.UTF_8);
            FileDownloadUtil.downloadFile(request, response, fileRealName, sqlBytes);
        } catch (Exception e) {
            throw new BusinessException(ErrorMessage.FAILED_TO_EXPORT_TABLE_SQL);
        }
    }

    @Override
    public void exportDatabaseExcel(String clusterId, String namespace, String middlewareName, String database, HttpServletRequest request, HttpServletResponse response) {
        List<TableDto> tableDtoList = listTables(clusterId, namespace, middlewareName, database);
        try {
            Map<String,List<ColumnDto>> tableMap = new LinkedHashMap<>();
            for (TableDto tableDto : tableDtoList) {
                List<ColumnDto> columnDtos = listTableColumns(clusterId, namespace, middlewareName, database, tableDto.getTableName());
                tableMap.put(tableDto.getTableName(), columnDtos);
            }
            String excelFilePath = ExcelUtil.createDatabaseExcel(path, database, tableMap);
            String fileRealName = database + ".xlsx";
            FileDownloadUtil.downloadFile(request, response, fileRealName, excelFilePath);
        } catch (Exception e) {
            throw new BusinessException(ErrorMessage.FAILED_TO_EXPORT_TABLE_SQL);
        }
    }

    @Override
    public boolean checkUserExists(String namespace, String middlewareName, String username) {
        if (StringUtils.isEmpty(username)) {
            return false;
        }
        JSONArray dataAry = mysqlClient.showUserDetail(getPath(middlewareName, namespace), port, username).getJSONArray("dataAry");
        return !CollectionUtils.isEmpty(dataAry);
    }

    @Override
    public boolean checkDatabaseExists(String namespace, String middlewareName, String database) {
        if (StringUtils.isEmpty(database)) {
            return false;
        }
        JSONArray dataAry = mysqlClient.showDatabaseDetail(getPath(middlewareName, namespace), port, database).getJSONArray("dataAry");
        return !CollectionUtils.isEmpty(dataAry);
    }

    @Override
    public ExecResult execSql(String clusterId, String namespace, String middlewareName, String database, String sql) {
        BeanSqlExecuteRecord record = new BeanSqlExecuteRecord();
        record.setClusterId(clusterId);
        record.setNamespace(namespace);
        record.setMiddlewareName(middlewareName);
        record.setExecDate(new Date());
        record.setTargetDatabase(database);
        record.setSqlStr(sql);

        SqlQuery sqlQuery = new SqlQuery(sql);
        sqlQuery.convertAndSetQuery();
        JSONObject res = mysqlClient.execSql(getPath(middlewareName, namespace), port, database, sqlQuery);
        record.setExecTime(res.getString("execTime"));
        record.setStatus(res.getString("success"));
        if (!res.getBoolean("success")) {
            record.setMessage(res.getString("message"));
            sqlExecuteRecordMapper.insert(record);
            throw new BusinessException(ErrorMessage.FAILED_TO_EXEC_QUERY, res.getString("message"));
        }
        JSONArray columnAry = res.getJSONArray("column");
        JSONArray dataAry = res.getJSONArray("dataAry");
        ExecResult execResult = new ExecResult();
        execResult.setColumns(columnAry);
        execResult.setData(dataAry);

        if (!CollectionUtils.isEmpty(dataAry)) {
            record.setLine(dataAry.size());
        } else {
            record.setLine(res.getInteger("rowsAffected"));
        }
        sqlExecuteRecordMapper.insert(record);
        return execResult;
    }

    @Override
    public List<BeanSqlExecuteRecord> listExecuteSql(String clusterId, String namespace, String middlewareName, Integer db, String keyword, String start, String end, Integer pageNum, Integer size) {
        return null;
    }

    /**
     * 获取主键操作编码
     */
    private int getPrimaryKeyActionCode(List<String> oldPrimaryKeys, List<String> newPrimaryKeys) {
        if (CollectionUtils.isEmpty(oldPrimaryKeys)) {
            if (!CollectionUtils.isEmpty(newPrimaryKeys)) {
                return MysqlOperationEnum.ADD.getCode();
            } else {
                return MysqlOperationEnum.NONE.getCode();
            }
        } else {
            if (oldPrimaryKeys.equals(newPrimaryKeys)) {
                return MysqlOperationEnum.NONE.getCode();
            } else {
                return MysqlOperationEnum.MODIFY.getCode();
            }
        }
    }

    /**
     * 纠正列信息
     * @param columnDtoList
     */
    private void correctColumn(List<ColumnDto> columnDtoList) {
        if (!CollectionUtils.isEmpty(columnDtoList)) {
            return;
        }
        for (ColumnDto columnDto : columnDtoList) {
            // 当某一列为主键时，必须设为不允许非空
            if (columnDto.isPrimary()) {
                columnDto.setNullable(false);
            }
        }
    }

    /**
     * 从列信息中找出主键列，并返回一个主键列表
     * @param columnDtoList
     * @return
     */
    private List<String> extractPrimaryKey(List<ColumnDto> columnDtoList) {
        return columnDtoList.stream().filter(ColumnDto::isPrimary).map(ColumnDto::getColumn).collect(Collectors.toList());
    }

    /**
     * 查询创建表SQL，并从中提取出索引信息：包括索引类型和存储类型(这两个字段无法通过直接查mysql系统表获取)
     * @return
     */
    private Map<String, IndexDto> extractTableIndex(String clusterId, String namespace, String middlewareName, String database, String table) {
        String tableSql = showTableSql(clusterId, namespace, middlewareName, database, table);
        tableSql = tableSql.substring(0, tableSql.lastIndexOf(")")).trim();

        List<String> list = Arrays.stream(tableSql.split("\\n")).filter(s -> s.contains("KEY") && !s.contains("FOREIGN")).collect(Collectors.toList());
        Map<String, IndexDto> indexMap = new HashMap<>();
        for (String singleIndexSql : list) {
            singleIndexSql = singleIndexSql.trim().replaceAll("`", "");
            String[] sqlAry = singleIndexSql.split(" ");
            IndexDto indexDto = new IndexDto();
            if (singleIndexSql.startsWith("KEY")) {
                indexDto.setType("INDEX");
                indexDto.setIndex(sqlAry[1]);
            } else if (singleIndexSql.startsWith("PRIMARY")) {
                indexDto.setIndex("PRIMARY");
                indexDto.setType("PRIMARY");
            } else {
                indexDto.setIndex(sqlAry[2]);
                indexDto.setType(sqlAry[0]);
            }
            if (singleIndexSql.contains("USING")) {
                indexDto.setStorageType(sqlAry[sqlAry.length - 1]);
            }
            indexMap.put(indexDto.getIndex(), indexDto);
        }
        return indexMap;
    }

    private String getPath(String middlewareName, String namespace) {
//        return middlewareName + "." + namespace;
        return "10.10.102.52";
    }
    
}
