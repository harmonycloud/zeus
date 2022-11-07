package com.harmonycloud.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.MysqlEngineEnum;
import com.harmonycloud.caas.common.enums.MysqlPrivilegeEnum;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.enums.middleware.MysqlDataTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.dashboard.mysql.*;
import com.harmonycloud.zeus.annotation.Operator;
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
        // SCHEMA_NAME,DEFAULT_CHARACTER_SET_NAME,DEFAULT_COLLATION_NAME
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
            // todo 查询每张表的记录数，即：行数
            tableDto.setRows(0);
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
    public void updateTableName(String clusterId, String namespace, String middlewareName, String database, String table, TableDto databaseDto) {

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
        tableDto.setCharset(tableObj.getString(MysqlUtil.extractCharset(tableObj.getString("TABLE_COLLATION"))));
        tableDto.setAutoIncrement(tableObj.getInteger("AUTO_INCREMENT"));
        tableDto.setMinRows(tableObj.getInteger(""));
        tableDto.setMaxRows(tableObj.getInteger(""));

        tableDto.setColumns(listTableColumns(clusterId, namespace, middlewareName, database, table));
        tableDto.setIndices(listTableIndices(clusterId, namespace, middlewareName, database, table));
        tableDto.setForeignKeys(listTableForeignKeys(clusterId, namespace, middlewareName, database, table));
        return tableDto;
    }

    @Override
    public JSONArray showTableData(String clusterId, String namespace, String middlewareName, String database, String table,QueryInfo queryInfo) {
        return mysqlClient.showTableData(getPath(middlewareName, namespace), port, database, table, queryInfo).getJSONArray("dataAry");
    }

    @Override
    public List<ColumnDto> listTableColumns(String clusterId, String namespace, String middlewareName, String database, String table) {
        JSONArray dataAry = mysqlClient.listTableColumns(getPath(middlewareName,namespace), port, database, table).getJSONArray("dataAry");
        return dataAry.stream().map(data -> {
            JSONObject obj = (JSONObject) data;
            ColumnDto columnDto = new ColumnDto();
            columnDto.setColumn(obj.getString("COLUMN_NAME"));
            columnDto.setDateType(obj.getString("COLUMN_TYPE"));
            columnDto.setComment(obj.getString("COLUMN_COMMENT"));
            columnDto.setNullable(MysqlUtil.convertColumnNullable(obj.getString("IS_NULLABLE")));
            columnDto.setColumnDefault(obj.getString("COLUMN_DEFAULT"));
            columnDto.setPrimary(MysqlUtil.convertColumnPrimary(obj.getString("COLUMN_KEY")));
            columnDto.setAutoIncrement(MysqlUtil.convertAutoIncrement(obj.getString("EXTRA")));
            columnDto.setSize(MysqlUtil.convertColumnDataSize(columnDto.getDateType()));
            columnDto.setCollate(obj.getString("COLLATION_NAME"));
            columnDto.setCharset(MysqlUtil.extractCharset(columnDto.getCollate()));
            return columnDto;
        }).collect(Collectors.toList());
    }

    @Override
    public void saveTableColumn(String clusterId, String namespace, String middlewareName, String database, String table, List<ColumnDto> columnDtoList) {
        JSONObject res = mysqlClient.saveTableColumns(getPath(middlewareName,namespace), port, database, table, columnDtoList);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.ALTER_TABLE_COLUMN_FAILED, res.getString("message"));
        }
    }

    @Override
    public List<IndexDto> listTableIndices(String clusterId, String namespace, String middlewareName, String database, String table) {
        JSONArray dataAry = mysqlClient.listTableIndices(getPath(middlewareName,namespace), port, database, table).getJSONArray("dataAry");
        List<IndexDto> indexList = new ArrayList<>();
        dataAry.forEach(data -> {
            JSONObject obj = (JSONObject) data;
            IndexColumnDto indexColumnDto = new IndexColumnDto();
            indexColumnDto.setKeyName(obj.getString("Key_name"));
            indexColumnDto.setColumnName(obj.getString("Column_name"));
            indexColumnDto.setIndexType(obj.getString("Index_type"));
            indexColumnDto.setSubPart(obj.getInteger("Sub_part"));
            indexColumnDto.setIndexComment(obj.getString("Index_comment"));
            IndexDto indexDto = new IndexDto();
            indexDto.setIndex(indexColumnDto.getKeyName());
            if (indexList.contains(indexDto)) {
                IndexDto tempIndexDto = indexList.get(indexList.indexOf(indexDto));
                tempIndexDto.getIndexColumns().add(indexColumnDto);
            } else {
                indexDto.setType(indexColumnDto.getIndexType());
                indexDto.setComment(indexColumnDto.getIndexComment());
                List<IndexColumnDto> columnDtoList = new ArrayList<>();
                columnDtoList.add(indexColumnDto);
                indexDto.setIndexColumns(columnDtoList);
                indexList.add(indexDto);
            }
        });
        return indexList;
    }

    @Override
    public void saveTableIndex(String clusterId, String namespace, String middlewareName, String database, String table, List<IndexDto> indexDtoList) {
        JSONObject res = mysqlClient.saveTableIndices(getPath(middlewareName,namespace), port, database, table, indexDtoList);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.ALTER_TABLE_INDICES_FAILED, res.getString("message"));
        }
    }

    @Override
    public List<ForeignKeyDto> listTableForeignKeys(String clusterId, String namespace, String middlewareName, String database, String table) {
        JSONArray dataAry = mysqlClient.listTableForeignKeys(getPath(middlewareName,namespace), port, database, table).getJSONArray("dataAry");
        List<ForeignKeyDto> foreignKeyDtoList = new ArrayList<>();
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
            if (foreignKeyDtoList.contains(foreignKeyDto)) {
                ForeignKeyDto tempForeignKeyDto = foreignKeyDtoList.get(foreignKeyDtoList.indexOf(foreignKeyDto));
                tempForeignKeyDto.getDetails().add(foreignKeyDetailDto);
            } else {
                foreignKeyDto.setOnDeleteOption(obj.getString("DELETE_RULE"));
                foreignKeyDto.setOnUpdateOption(obj.getString("UPDATE_RULE"));
                List<ForeignKeyDetailDto> columnDtoList = new ArrayList<>();
                columnDtoList.add(foreignKeyDetailDto);
                foreignKeyDto.setDetails(columnDtoList);
                foreignKeyDtoList.add(foreignKeyDto);
            }
        });
        return foreignKeyDtoList;
    }

    @Override
    public void saveTableForeignKey(String clusterId, String namespace, String middlewareName, String database, String table, List<ForeignKeyDto> foreignKeyDtos) {
        JSONObject res = mysqlClient.saveTableForeignKeys(getPath(middlewareName,namespace), port, database, table, foreignKeyDtos);
        if (!res.getBoolean("success")) {
            throw new BusinessException(ErrorMessage.ALTER_TABLE_FOREIGN_KEYS_FAILED, res.getString("message"));
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

    private String getPath(String middlewareName, String namespace) {
        // todo 提交时此处代码需删除
//        return middlewareName + "." + namespace;
        return temppath;
    }
    
}
