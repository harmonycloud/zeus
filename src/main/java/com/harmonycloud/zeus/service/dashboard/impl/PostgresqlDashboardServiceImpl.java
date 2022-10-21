package com.harmonycloud.zeus.service.dashboard.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.enums.PostgresqlPrivilegeEnum;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.middleware.ServicePortDTO;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.k8s.ServiceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.dashboard.*;
import com.harmonycloud.zeus.integration.dashboard.PostgresqlClient;
import com.harmonycloud.zeus.service.dashboard.PostgresqlDashboardService;
import com.harmonycloud.zeus.util.PostgresqlAuthorityUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2022/10/11 2:50 下午
 */
@Service
@Slf4j
@Operator(paramTypes4One = String.class)
public class PostgresqlDashboardServiceImpl implements PostgresqlDashboardService {

    public static final Map<String, String> POSTGRESQL_PORT_MAP = new ConcurrentHashMap<>();

    @Value("${system.middleware-api.postgresql.port:5432}")
    private String port;

    @Autowired
    private PostgresqlClient postgresqlClient;
    @Autowired
    private ServiceService serviceService;

    @Override
    public boolean support(String type) {
        return MiddlewareTypeEnum.POSTGRESQL.getType().equals(type);
    }

    @Override
    public String login(String clusterId, String namespace, String middlewareName, String username, String password) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject login = postgresqlClient.login(path, port, username, password);
        JSONObject err = login.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.LOGIN_FAILED, err.getString("Message"));
        }
        return login.getString("token");
    }

    @Override
    public List<DatabaseDto> listDatabases(String clusterId, String namespace, String middlewareName) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject listDatabases = postgresqlClient.listDatabases(path, port);
        List<Map<String, String>> databaseList = convertColumn(listDatabases);
        return databaseList.stream().map(database -> {
            DatabaseDto databaseDto = new DatabaseDto();
            databaseDto.setOid(database.get("oid"));
            databaseDto.setDatabaseName(database.get("datname"));
            databaseDto.setEncoding(database.get("encoding"));
            databaseDto.setCollate(database.get("datcollate"));
            databaseDto.setOwner(database.get("datdba"));
            return databaseDto;
        }).collect(Collectors.toList());
    }

    @Override
    public DatabaseDto getDatabase(String clusterId, String namespace, String middlewareName, String databaseName) {
        List<DatabaseDto> databaseDtoList = this.listDatabases(clusterId, namespace, middlewareName);
        databaseDtoList = databaseDtoList.stream()
            .filter(databaseDto -> databaseDto.getDatabaseName().equals(databaseName)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(databaseDtoList)) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_DATABASE_NOT_FOUND);
        }
        DatabaseDto databaseDto = databaseDtoList.get(0);
        // 查询owner
        List<MiddlewareUserDto> userDtoList = this.listUser(clusterId, namespace, middlewareName, null);
        userDtoList = userDtoList.stream().filter(userDto -> userDto.getId().equals(databaseDto.getOwner()))
            .collect(Collectors.toList());
        // 查询comment
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject getDatabaseNotes = postgresqlClient.getDatabaseNotes(path, port, databaseName, databaseDto.getOid());
        return databaseDto.setOwner(userDtoList.get(0).getUsername())
            .setComment(convertColumn(getDatabaseNotes).get(0).get("shobj_description"));
    }

    @Override
    public void addDatabase(String clusterId, String namespace, String middlewareName, DatabaseDto databaseDto) {
        if (StringUtils.isNotEmpty(databaseDto.getEncoding())) {
            databaseDto.setEncoding("UTF8");
        }
        if (StringUtils.isNotEmpty(databaseDto.getTablespace())) {
            databaseDto.setTablespace("pg_default");
        }
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject addUser = postgresqlClient.createDatabase(path, port, databaseDto);
        JSONObject err = addUser.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_CREATE_DATABASE_FAILED, err.getString("Message"));
        }
    }

    @Override
    public void updateDatabase(String clusterId, String namespace, String middlewareName, String databaseName,
        DatabaseDto databaseDto) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject updateDatabase = postgresqlClient.updateDatabase(path, port, databaseName, databaseDto);
        JSONObject err = updateDatabase.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_UPDATE_DATABASE_FAILED, err.getString("Message"));
        }
    }

    @Override
    public void deleteDatabase(String clusterId, String namespace, String middlewareName, String databaseName) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject addUser = postgresqlClient.dropDatabase(path, port, databaseName);
        JSONObject err = addUser.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_DELETE_DATABASE_FAILED, err.getString("Message"));
        }
    }

    @Override
    public List<SchemaDto> listSchemas(String clusterId, String namespace, String middlewareName, String databaseName) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject listSchemas = postgresqlClient.listSchemas(path, port, databaseName);
        List<Map<String, String>> schemaList = convertColumn(listSchemas);
        return schemaList.stream().map(schema -> {
            SchemaDto schemaDto = new SchemaDto();
            schemaDto.setOid(schema.get("oid"));
            schemaDto.setSchemaName(schema.get("nspname"));
            schemaDto.setDatabaseName(databaseName);
            return schemaDto;
        }).collect(Collectors.toList());
    }

    @Override
    public SchemaDto getSchema(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject listSchemas = postgresqlClient.listSchemas(path, port, databaseName);
        List<Map<String, String>> schemaList = convertColumn(listSchemas);
        schemaList =
            schemaList.stream().filter(schema -> schema.get("nspname").equals(schemaName)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(schemaList)) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_SCHEMA_NOT_FOUND);
        }
        // 获取schema
        Map<String, String> schema = schemaList.get(0);
        // 查询owner
        List<MiddlewareUserDto> userDtoList = this.listUser(clusterId, namespace, middlewareName, null);
        userDtoList = userDtoList.stream().filter(userDto -> userDto.getId().equals(schema.get("nspowner")))
            .collect(Collectors.toList());
        // 查询comment
        JSONObject getSchemaNotes = postgresqlClient.getSchemaNotes(path, port, databaseName, schema.get("nspname"));
        // return
        return new SchemaDto().setSchemaName(schema.get("nspname")).setDatabaseName(databaseName)
            .setOid(schema.get("oid")).setOwner(userDtoList.get(0).getUsername())
            .setComment(convertColumn(getSchemaNotes).get(0).get("obj_description"));
    }

    @Override
    public void addSchema(String clusterId, String namespace, String middlewareName, SchemaDto schemaDto) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject createSchema = postgresqlClient.createSchema(path, port, schemaDto.getDatabaseName(), schemaDto);
        JSONObject err = createSchema.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_CREATE_SCHEMA_FAILED, err.getString("Message"));
        }
    }

    @Override
    public void updateSchema(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName, SchemaDto schemaDto) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject updateSchema = postgresqlClient.updateSchema(path, port, databaseName, schemaName, schemaDto);
        JSONObject err = updateSchema.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_UPDATE_SCHEMA_FAILED, err.getString("Message"));
        }
    }

    @Override
    public void deleteSchema(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject dropSchemas = postgresqlClient.dropSchemas(path, port, databaseName, schemaName);
        JSONObject err = dropSchemas.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_DELETE_SCHEMA_FAILED, err.getString("Message"));
        }
    }

    @Override
    public List<TableDto> listTables(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject listTables = postgresqlClient.listTables(path, port, databaseName, schemaName);
        List<Map<String, String>> tableList = convertColumn(listTables);
        return tableList.stream().map(table -> {
            TableDto tableDto = new TableDto();
            tableDto.setOid(table.get("oid"));
            tableDto.setTableName(table.get("tablename"));
            tableDto.setOwner(table.get("tableowner"));
            tableDto.setTablespace(table.get("tablespace"));
            tableDto.setDescription(table.get("description"));
            tableDto.setDatabaseName(databaseName);
            tableDto.setSchemaName(schemaName);
            // 获取fillFactor 默认100
            tableDto.setFillFactor("100");
            String relOptions = table.get("reloptions");
            if (relOptions.contains("fillfactor")) {
                Matcher matcher = Pattern.compile("fillfactor=\\d+").matcher(relOptions);
                if (matcher.find()) {
                    tableDto.setFillFactor(matcher.group().split("=")[1]);
                }
            }
            return tableDto;
        }).collect(Collectors.toList());
    }

    @Override
    public TableDto getTable(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName, String tableName) {
        // 获取基本信息
        List<TableDto> tableDtoList = this.listTables(clusterId, namespace, middlewareName, databaseName, schemaName);
        tableDtoList = tableDtoList.stream().filter(tableDto -> tableDto.getTableName().equals(tableName))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tableDtoList)) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_TABLE_NOT_FOUND);
        }
        // 四个约束

        // 继承信息
        return null;
    }

    @Override
    public void addTable(String clusterId, String namespace, String middlewareName, TableDto tableDto) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> columnComment = new HashMap<>();
        StringBuilder pk = new StringBuilder();
        // 构造column sql语句
        for (ColumnDto columnDto : tableDto.getColumnDtoList()) {
            if (columnDto.getPrimaryKey() != null && columnDto.getPrimaryKey()) {
                pk.append(columnDto.getPrimaryKey()).append(",");
            }
            if (StringUtils.isNotEmpty(columnDto.getComment())) {
                columnComment.put(columnDto.getColumn(), columnDto.getComment());
            }
            sb.append(columnDto.getColumn()).append(" ");
            if (columnDto.getInc() != null && columnDto.getInc()) {
                sb.append("serial");
            } else {
                sb.append(columnDto.getDateType());
            }
            if (columnDto.getArray() != null && columnDto.getArray()) {
                sb.append("[]");
            }
            if (StringUtils.isNotEmpty(columnDto.getSize())) {
                sb.append("(").append(columnDto.getSize()).append(")");
            }
            sb.append(" ");
            if (StringUtils.isNotEmpty(columnDto.getDefaultValue())) {
                sb.append(columnDto.getDefaultValue());
            }
            if (columnDto.getNullable() != null && columnDto.getNullable()) {
                sb.append("not null ");
            }
            if (StringUtils.isNotEmpty(columnDto.getCollate())) {
                sb.append("COLLATE ").append(columnDto.getCollate());
            }
            sb.append(",");
        }
        // 外键约束
        for (TableForeignKey foreign : tableDto.getTableForeignKeyList()) {
            sb.append("CONSTRAINT ").append(foreign.getName()).append(" foreign key ").append(foreign.getColumnName())
                .append("REFERENCES ").append(foreign.getTargetSchema()).append(".").append(foreign.getTargetTable())
                .append("(").append(foreign.getTargetColumn()).append(")").append(" on update ")
                .append(foreign.getOnUpdate()).append(" on delete ").append(foreign.getOnDelete()).append(" ")
                .append(foreign.getDeferrablity());
            sb.append(",");
        }
        // 排它约束
        for (TableExclusion exclusion : tableDto.getTableExclusionList()) {
            sb.append("CONSTRAINT ").append(exclusion.getName()).append(" EXCLUDE using ").append(exclusion.getName())
                .append(" ").append(exclusion.getColumnName()).append(" with ").append(exclusion.getOperator());
            sb.append(",");
        }
        // 唯一约束
        for (TableUnique unique : tableDto.getTableUniqueList()) {
            sb.append("CONSTRAINT ").append(unique.getName()).append(" unique ").append("(")
                .append(unique.getColumnName()).append(")").append(unique.getDeferrablity());
            sb.append(",");
        }
        // 检查约束
        for (TableCheck check : tableDto.getTableCheckList()) {
            sb.append("CONSTRAINT ").append(check.getName()).append(" check ").append("(").append(check.getText())
                .append(") ");
            if (check.getNoInherit() != null && check.getNoInherit()) {
                sb.append("no inherit ");
            }
            if (check.getNotValid() != null && check.getNotValid()) {
                sb.append("not valid");
            }
            sb.append(",");
        }
        // 主键约束
        if (StringUtils.isNotEmpty(pk.toString())) {
            sb.append("CONSTRAINT ").append("pk_").append(tableDto.getSchemaName()).append("_")
                .append(tableDto.getTableName()).append(" PRIMARY KEY ").append("(").append(pk.toString()).append(")");
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        // 继承
        StringBuilder inherit = new StringBuilder();
        if (!CollectionUtils.isEmpty(tableDto.getTableInheritList())) {
            for (TableInherit tableInherit : tableDto.getTableInheritList()) {
                inherit.append(tableInherit.getSchemaName()).append(".").append(tableInherit.getTableName())
                    .append(",");
            }
            if (inherit.length() != 0) {
                inherit.deleteCharAt(sb.length() - 1);
            }
        }
        JSONObject table = new JSONObject();
        table.put("database", tableDto.getDatabaseName());
        table.put("schema", tableDto.getSchemaName());
        table.put("column", sb.toString());
        table.put("inherit", inherit);
        table.put("owner", tableDto.getOwner());
        table.put("comment", columnComment);
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject addTable =
            postgresqlClient.addTable(path, port, tableDto.getDatabaseName(), tableDto.getSchemaName(), table);
        JSONObject err = addTable.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_CREATE_TABLE_FAILED, err.getString("Message"));
        }
    }

    @Override
    public void dropTable(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName, String tableName) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject dropTable = postgresqlClient.dropTable(path, port, databaseName, schemaName, tableName);
        JSONObject err = dropTable.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_DELETE_TABLE_FAILED, err.getString("Message"));
        }
    }

    @Override
    public JSONObject getTableData(String clusterId, String namespace, String middlewareName, String databaseName, String schemaName, String tableName, Integer current, Integer size, Map<String, String> order) {

        return null;
    }

    @Override
    public List<ColumnDto> listColumns(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName, String table) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject listColumns = postgresqlClient.listColumns(path, port, databaseName, schemaName, table);
        List<Map<String, String>> columnList = convertColumn(listColumns);
        return columnList.stream().map(column -> {
            ColumnDto columnDto = new ColumnDto();
            columnDto.setDatabaseName(databaseName);
            columnDto.setSchemaName(schemaName);
            columnDto.setTableName(table);
            String column_default = column.get("column_default");
            // 判断是否自增
            if (column_default.contains("nextval")) {
                columnDto.setInc(true);
            }
            columnDto.setDefaultValue(column_default);
            columnDto.setNullable("YES".equals(column.get("is_nullable")));
            // 判断数据类型以及是否为数组
            String data_type = column.get("data_type");
            if ("ARRAY".equals(data_type)) {
                columnDto.setArray(true);
                columnDto.setDateType(column.get("array_data_type"));
            } else {
                columnDto.setArray(false);
                columnDto.setDateType(data_type);
            }
            columnDto.setNum(column.get("ordinal_position"));
            columnDto.setSize(column.get("character_maximum_length"));
            columnDto.setComment(column.get("col_description"));
            return columnDto;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateForeignKey(String clusterId, String namespace, String middlewareName, TableDto tableDto) {
        if (CollectionUtils.isEmpty(tableDto.getTableForeignKeyList())) {
            return;
        }
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        for (TableForeignKey key : tableDto.getTableForeignKeyList()) {
            // 创建外键
            if ("add".equals(key.getOperator())) {
                JSONObject createForeignKey = postgresqlClient.createForeignKey(path, port, tableDto.getDatabaseName(),
                    tableDto.getSchemaName(), tableDto.getTableName(), key);
                if (createForeignKey.get("err") != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_CREATE_FOREIGN_KEY_FAILED,
                        createForeignKey.getJSONObject("err").getString("Message"));
                }
            } else if ("delete".equals(key.getOperator())) {
                // 删除外键
                dropConstraint(path, tableDto.getDatabaseName(), tableDto.getSchemaName(), tableDto.getTableName(),
                    key.getName());
            }
        }
    }

    @Override
    public void updateExclusion(String clusterId, String namespace, String middlewareName, TableDto tableDto) {
        if (CollectionUtils.isEmpty(tableDto.getTableExclusionList())) {
            return;
        }
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        for (TableExclusion key : tableDto.getTableExclusionList()) {
            // 创建排它约束
            if ("add".equals(key.getOperator())) {
                JSONObject createForeignKey = postgresqlClient.createExclusion(path, port, tableDto.getDatabaseName(),
                    tableDto.getSchemaName(), tableDto.getTableName(), key);
                if (createForeignKey.get("err") != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_CREATE_EXCLUDE_FAILED,
                        createForeignKey.getJSONObject("err").getString("Message"));
                }
            } else if ("delete".equals(key.getOperator())) {
                // 删除约束
                dropConstraint(path, tableDto.getDatabaseName(), tableDto.getSchemaName(), tableDto.getTableName(),
                    key.getName());
            }
        }
    }

    @Override
    public void updateUnique(String clusterId, String namespace, String middlewareName, TableDto tableDto) {
        if (CollectionUtils.isEmpty(tableDto.getTableUniqueList())) {
            return;
        }
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        for (TableUnique key : tableDto.getTableUniqueList()) {
            // 创建唯一约束
            if ("add".equals(key.getOperator())) {
                JSONObject createForeignKey = postgresqlClient.createUnique(path, port, tableDto.getDatabaseName(),
                    tableDto.getSchemaName(), tableDto.getTableName(), key);
                if (createForeignKey.get("err") != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_CREATE_UNIQUE_FAILED,
                        createForeignKey.getJSONObject("err").getString("Message"));
                }
            } else if ("delete".equals(key.getOperator())) {
                // 删除约束
                dropConstraint(path, tableDto.getDatabaseName(), tableDto.getSchemaName(), tableDto.getTableName(),
                    key.getName());
            }
        }
    }

    @Override
    public void updateCheck(String clusterId, String namespace, String middlewareName, TableDto tableDto) {
        if (CollectionUtils.isEmpty(tableDto.getTableCheckList())) {
            return;
        }
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        for (TableCheck key : tableDto.getTableCheckList()) {
            // 创建检查约束
            if ("add".equals(key.getOperator())) {
                JSONObject createForeignKey = postgresqlClient.createCheck(path, port, tableDto.getDatabaseName(),
                    tableDto.getSchemaName(), tableDto.getTableName(), key);
                if (createForeignKey.get("err") != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_CREATE_CHECK_FAILED,
                        createForeignKey.getJSONObject("err").getString("Message"));
                }
            } else if ("delete".equals(key.getOperator())) {
                // 删除约束
                dropConstraint(path, tableDto.getDatabaseName(), tableDto.getSchemaName(), tableDto.getTableName(),
                    key.getName());
            }
        }
    }

    @Override
    public void updateInherit(String clusterId, String namespace, String middlewareName, TableDto tableDto) {
        if (CollectionUtils.isEmpty(tableDto.getTableInheritList())) {
            return;
        }
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        for (TableInherit key : tableDto.getTableInheritList()) {
            // 添加继承关系
            if ("add".equals(key.getOperator())) {
                JSONObject createForeignKey = postgresqlClient.addInherit(path, port, tableDto.getDatabaseName(),
                    tableDto.getSchemaName(), tableDto.getTableName(), key.getSchemaName(), key.getTableName());
                if (createForeignKey.get("err") != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_ADD_TABLE_INHERIT_FAILED,
                        createForeignKey.getJSONObject("err").getString("Message"));
                }
            } else if ("delete".equals(key.getOperator())) {
                // 取消继承关系
                JSONObject dropInherit = postgresqlClient.dropInherit(path, port, tableDto.getDatabaseName(),
                    tableDto.getSchemaName(), tableDto.getTableName(), key.getSchemaName(), key.getTableName());
                if (dropInherit.get("err") != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_DROP_TABLE_INHERIT_FAILED,
                        dropInherit.getJSONObject("err").getString("Message"));
                }
            }
        }
    }

    @Override
    public List<MiddlewareUserDto> listUser(String clusterId, String namespace, String middlewareName, String keyword) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject listUsers = postgresqlClient.listUsers(path, port);
        List<Map<String, String>> res = convertColumn(listUsers);

        return res.stream().map(map -> {
            MiddlewareUserDto middlewareUserDto = new MiddlewareUserDto();
            middlewareUserDto.setId(map.get("oid"));
            middlewareUserDto.setUsername(map.get("rolname"));
            middlewareUserDto.setUsable(Boolean.parseBoolean(map.get("rolcanlogin")));
            middlewareUserDto.setInherit(Boolean.parseBoolean(map.get("rolinherit")));
            return middlewareUserDto;
        }).collect(Collectors.toList());
    }

    @Override
    public void addUser(String clusterId, String namespace, String middlewareName,
        MiddlewareUserDto middlewareUserDto) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject addUser =
            postgresqlClient.addUser(path, port, middlewareUserDto.getUsername(), middlewareUserDto.getPassword());
        JSONObject err = addUser.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_CREATE_USER_FAILED, err.getString("Message"));
        }
    }

    @Override
    public void dropUser(String clusterId, String namespace, String middlewareName, String username) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject dropUser = postgresqlClient.dropUser(path, port, username);
        JSONObject err = dropUser.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_DELETE_USER_FAILED, err.getString("Message"));
        }
    }

    @Override
    public void grantUser(String clusterId, String namespace, String middlewareName, String username,
        MiddlewareUserAuthority middlewareUserAuthority) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        String grantOption = "";
        if (middlewareUserAuthority.getGrantAble()) {
            grantOption = "with grant option";
        }
        if (StringUtils.isNotEmpty(middlewareUserAuthority.getTable())) {
            // 赋权table
            String privileges =
                PostgresqlPrivilegeEnum.findTablePrivilege(middlewareUserAuthority.getAuthority()).getPrivilege();
            JSONObject grantUserTable =
                postgresqlClient.grantUserTable(path, port, username, middlewareUserAuthority.getDatabase(),
                    middlewareUserAuthority.getSchema(), middlewareUserAuthority.getTable(), privileges, grantOption);
            JSONObject err = grantUserTable.getJSONObject("err");
            if (err != null) {
                throw new BusinessException(ErrorMessage.POSTGRESQL_GRANT_USER_TABLE_FAILED, err.getString("Message"));
            }
        } else if (StringUtils.isNotEmpty(middlewareUserAuthority.getSchema())) {
            // 赋权schema
            String privileges =
                PostgresqlPrivilegeEnum.findSchemaPrivilege(middlewareUserAuthority.getAuthority()).getPrivilege();
            JSONObject grantUserSchema = postgresqlClient.grantUserSchema(path, port, username,
                middlewareUserAuthority.getDatabase(), middlewareUserAuthority.getSchema(), privileges, grantOption);
            JSONObject err = grantUserSchema.getJSONObject("err");
            if (err != null) {
                throw new BusinessException(ErrorMessage.POSTGRESQL_GRANT_USER_SCHEMA_FAILED, err.getString("Message"));
            }
        } else if (StringUtils.isNotEmpty(middlewareUserAuthority.getDatabase())) {
            // 赋权database
            String privileges =
                PostgresqlPrivilegeEnum.findDatabasePrivilege(middlewareUserAuthority.getAuthority()).getPrivilege();
            JSONObject grantUserDatabase = postgresqlClient.grantUserDatabase(path, port, username,
                middlewareUserAuthority.getDatabase(), privileges, grantOption);
            JSONObject err = grantUserDatabase.getJSONObject("err");
            if (err != null) {
                throw new BusinessException(ErrorMessage.POSTGRESQL_GRANT_USER_DATABASE_FAILED, err.getString("Message"));
            }
        }
    }

    @Override
    public void revokeUser(String clusterId, String namespace, String middlewareName,
        MiddlewareUserDto middlewareUserDto) {
        String username = middlewareUserDto.getUsername();
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        for (MiddlewareUserAuthority middlewareUserAuthority : middlewareUserDto.getAuthorityList()) {
            if (StringUtils.isNotEmpty(middlewareUserAuthority.getTable())) {
                // 赋权table
                String privileges =
                    PostgresqlPrivilegeEnum.findTablePrivilege(middlewareUserAuthority.getAuthority()).getPrivilege();
                JSONObject revokeUserTable =
                    postgresqlClient.revokeUserTable(path, port, username, middlewareUserAuthority.getDatabase(),
                        middlewareUserAuthority.getSchema(), middlewareUserAuthority.getTable(), privileges);
                JSONObject err = revokeUserTable.getJSONObject("err");
                if (err != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_REVOKE_USER_TABLE_FAILED, err.getString("Message"));
                }
            } else if (StringUtils.isNotEmpty(middlewareUserAuthority.getSchema())) {
                // 赋权schema
                String privileges =
                    PostgresqlPrivilegeEnum.findSchemaPrivilege(middlewareUserAuthority.getAuthority()).getPrivilege();
                JSONObject revokeUserSchema = postgresqlClient.revokeUserSchema(path, port, username,
                    middlewareUserAuthority.getDatabase(), middlewareUserAuthority.getSchema(), privileges);
                JSONObject err = revokeUserSchema.getJSONObject("err");
                if (err != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_REVOKE_USER_SCHEMA_FAILED, err.getString("Message"));
                }
            } else if (StringUtils.isNotEmpty(middlewareUserAuthority.getDatabase())) {
                // 赋权database
                String privileges = PostgresqlPrivilegeEnum
                    .findDatabasePrivilege(middlewareUserAuthority.getAuthority()).getPrivilege();
                JSONObject revokeUserDatabase = postgresqlClient.revokeUserDatabase(path, port, username,
                    middlewareUserAuthority.getDatabase(), privileges);
                JSONObject err = revokeUserDatabase.getJSONObject("err");
                if (err != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_REVOKE_USER_DATABASE_FAILED, err.getString("Message"));
                }
            }
        }
    }

    @Override
    public List<MiddlewareUserAuthority> userAuthority(String clusterId, String namespace, String middlewareName,
        String username) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject listDatabases = postgresqlClient.listDatabases(path, port);
        List<Map<String, String>> databaseList = convertColumn(listDatabases);

        List<MiddlewareUserAuthority> userAuthorityList = new ArrayList<>();
        databaseList.forEach(database -> {
            String datAcl = database.get("datacl");
            if (StringUtils.isEmpty(datAcl) || !datAcl.contains(username + "=")) {
                // todo 查询owner
                return;
            }
            MiddlewareUserAuthority userAuthority = new MiddlewareUserAuthority();
            userAuthority.setDatabase(database.get("datname"));
            userAuthority.setAuthority(PostgresqlAuthorityUtil.checkAuthority(datAcl, username));
            userAuthorityList.add(userAuthority);
        });
        return userAuthorityList;
    }

    @Override
    public void resetPassword(String clusterId, String namespace, String middlewareName, String username) {
        // todo
    }

    public List<Map<String, String>> convertColumn(JSONObject jsonObject) {
        JSONObject err = jsonObject.getJSONObject("err");
        if (err != null){
            String errString = err.getJSONObject("Err").getString("Err");
            throw new BusinessException(ErrorMessage.POSTGRESQL_SELECT_FAILED, errString);
        }
        JSONArray data = jsonObject.getJSONArray("data");
        if (data == null){
            return new ArrayList<>();
        }
        JSONArray column = jsonObject.getJSONArray("column");

        List<Map<String, String>> res = new ArrayList<>();
        List<String> columnList = column.toJavaList(String.class);
        for (int i = 0; i < data.size(); ++i) {
            List<String> dataList = data.getJSONArray(i).toJavaList(String.class);
            Map<String, String> map = new HashMap<>();
            for (int j = 0; j < dataList.size(); ++j) {
                map.put(columnList.get(j), dataList.get(j));
            }
            res.add(map);
        }
        return res;
    }

    public void dropConstraint(String path, String databaseName, String schemaName, String tableName,
        String constraintName) {
        // 删除外键
        JSONObject deleteConstraint =
            postgresqlClient.deleteConstraint(path, port, databaseName, schemaName, tableName, constraintName);
        if (deleteConstraint.get("err") != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_DROP_CONSTRAINT_FAILED,
                deleteConstraint.getJSONObject("err").getString("Message"));
        }
    }

    public void setPort(String clusterId, String namespace, String middlewareName) {
        if (POSTGRESQL_PORT_MAP.containsKey(middlewareName)) {
            port = POSTGRESQL_PORT_MAP.get(middlewareName);
            return;
        }
        ServicePortDTO servicePortDTO = serviceService.get(clusterId, namespace, middlewareName);
        if (servicePortDTO != null && !CollectionUtils.isEmpty(servicePortDTO.getPortDetailDtoList())) {
            port = servicePortDTO.getPortDetailDtoList().get(0).getPort();
            POSTGRESQL_PORT_MAP.put(middlewareName, port);
        }
    }

    public String getPath(String middlewareName, String namespace) {
       // return middlewareName + "." + namespace;
        return middlewareName;
    }
}
