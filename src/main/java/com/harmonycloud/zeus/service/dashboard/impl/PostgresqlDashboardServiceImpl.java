package com.harmonycloud.zeus.service.dashboard.impl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.http.ForestResponse;
import com.harmonycloud.caas.common.enums.EncodingEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.PostgresqlPrivilegeEnum;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.enums.middleware.PostgresqlCollateEnum;
import com.harmonycloud.caas.common.enums.middleware.PostgresqlDataTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.dashboard.*;
import com.harmonycloud.caas.common.model.dashboard.mysql.QueryInfo;
import com.harmonycloud.caas.common.model.middleware.ServicePortDTO;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.bean.BeanSqlExecuteRecord;
import com.harmonycloud.zeus.integration.dashboard.PostgresqlClient;
import com.harmonycloud.zeus.service.dashboard.ExecuteSqlService;
import com.harmonycloud.zeus.service.dashboard.PostgresqlDashboardService;
import com.harmonycloud.zeus.service.k8s.ServiceService;
import com.harmonycloud.zeus.util.ExcelUtil;
import com.harmonycloud.zeus.util.FileDownloadUtil;
import com.harmonycloud.zeus.util.PostgresqlAuthorityUtil;

import lombok.extern.slf4j.Slf4j;

import static com.harmonycloud.caas.common.constants.middleware.PostgresqlDashboardConstant.TEMPLATE0;
import static com.harmonycloud.caas.common.constants.middleware.PostgresqlDashboardConstant.TEMPLATE1;

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
    @Value("${system.upload.path:/usr/local/zeus-pv/upload}")
    private String uploadPath;

    @Autowired
    private PostgresqlClient postgresqlClient;
    @Autowired
    private ServiceService serviceService;
    @Autowired
    private ExecuteSqlService executeSqlService;

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
    public void logout(String clusterId, String namespace, String middlewareName) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        postgresqlClient.logout(path, port);
    }

    @Override
    public ExecuteSqlDto executeSql(String clusterId, String namespace, String middlewareName, String databaseName, String sql) {
        ExecuteSqlDto executeSqlDto = new ExecuteSqlDto();
        executeSqlDto.setDate(new Date());
        executeSqlDto.setSql(sql);
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        // 判断sql类型
        boolean query = executeSqlDto.getSql().toLowerCase().contains("select");
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        if (query) {
            sql += " limit 3000";
        }
        ForestResponse<JSONObject> response = postgresqlClient.sqlExecute(path, port, databaseName, sql, query);
        JSONObject sqlExecute = response.getResult();
        // 获取执行结果和异常信息（若存在）
        if (sqlExecute.getJSONObject("err") != null) {
            executeSqlDto.setStatus("failed");
            executeSqlDto.setErr(sqlExecute.getJSONObject("err"));
            // 处理异常信息
            JSONObject err = sqlExecute.getJSONObject("err");
            if (err.containsKey("Message")) {
                executeSqlDto.setMessage(err.getString("Message"));
            } else {
                executeSqlDto.setMessage(dealWithErr(sqlExecute, false));
            }
        } else {
            if (query) {
                executeSqlDto.setData(convertColumn(sqlExecute));
            }
            executeSqlDto.setStatus("success");
            executeSqlDto.setMessage("success");
        }
        executeSqlDto.setTime(response.getTimeAsMillisecond() + "ms");
        executeSqlDto.setDatabase(databaseName);
        // 执行结果记入数据库
        BeanSqlExecuteRecord record = new BeanSqlExecuteRecord();
        record.setClusterId(clusterId).setNamespace(namespace).setMiddlewareName(middlewareName)
            .setTargetDatabase(databaseName).setSqlStr(executeSqlDto.getSql()).setExecStatus(executeSqlDto.getStatus())
            .setExecDate(executeSqlDto.getDate()).setMessage(executeSqlDto.getMessage())
            .setExecTime(executeSqlDto.getTime());
        executeSqlService.insert(record);
        return executeSqlDto;
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
            databaseDto.setEncoding(database.get("pg_encoding_to_char"));
            databaseDto.setCollate(database.get("datcollate"));
            databaseDto.setOwner(database.get("rolname"));
            databaseDto.setTablespace(database.get("spcname"));
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
    public List<String> getTablespace(String clusterId, String namespace, String middlewareName, String databaseName) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject getTablespace = postgresqlClient.getTablespace(path, port, databaseName);
        List<Map<String, String>> tablespaceList = convertColumn(getTablespace);
        return tablespaceList.stream().map(tablespace -> {
            return tablespace.get("spcname");
        }).collect(Collectors.toList());
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
            schemaDto.setOwner(schema.get("rolname"));
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
        if (listTables.getJSONArray("data") == null) {
            return new ArrayList<>();
        }
        List<Map<String, String>> tableList = convertColumn(listTables);
        return tableList.stream().map(table -> {
            TableDto tableDto = new TableDto();
            tableDto.setOid(table.get("oid"));
            tableDto.setTableName(table.get("tablename"));
            tableDto.setOwner(table.get("tableowner"));
            tableDto.setTablespace(table.get("tablespace"));
            if (StringUtils.isEmpty(tableDto.getTablespace())){
                tableDto.setTablespace("pg_default");
            }
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
        TableDto tableDto = tableDtoList.get(0);
        // 四个约束
        String path = getPath(middlewareName, namespace);
        JSONObject getConstraint =
            postgresqlClient.getConstraint(path, port, databaseName, schemaName, tableName, tableDto.getOid());
        List<Map<String, String>> constraintList = convertColumn(getConstraint);

        List<TableForeignKey> tableForeignKeyList = new ArrayList<>();
        List<TableExclusion> tableExclusionList = new ArrayList<>();
        List<TableUnique> tableUniqueList = new ArrayList<>();
        List<TableCheck> tableCheckList = new ArrayList<>();

        for (Map<String, String> constraint : constraintList) {
            if ("f".equals(constraint.get("contype"))) {
                String condef = constraint.get("condef");
                TableForeignKey foreignKey = new TableForeignKey();
                String pattern =
                    "FOREIGN KEY \\(([A-Za-z0-9_,\\s]+)\\) REFERENCES (\\w+\\.)?(\\w+)\\(([A-Za-z0-9_,\\s]+)\\)(.+)?";
                Matcher matcher = Pattern.compile(pattern).matcher(condef);
                if (matcher.find()) {
                    // 获取列和目标列
                    List<TableForeignKey.Content> contentList = new ArrayList<>();
                    String[] column = matcher.group(1).split(", ");
                    String[] target = matcher.group(4).split(", ");
                    for (int i = 0; i < column.length; ++i) {
                        TableForeignKey.Content content = new TableForeignKey.Content();
                        content.setColumnName(column[i]);
                        content.setTargetColumn(target[i]);
                        contentList.add(content);
                    }
                    // 获取策略
                    if (StringUtils.isNotEmpty(matcher.group(5))) {
                        String[] config = matcher.group(5).split(" ");
                        for (int i = 0; i < config.length; ++i) {
                            if ("ON".equals(config[i]) && "UPDATE".equals(config[i + 1])) {
                                foreignKey.setOnUpdate(config[i + 2]);
                            }
                            // 删除时策略
                            if ("ON".equals(config[i]) && "DELETE".equals(config[i + 1])) {
                                foreignKey.setOnDelete(config[i + 2]);
                            }
                        }
                    }
                    foreignKey.setContentList(contentList);
                    foreignKey.setTargetSchema(
                        StringUtils.isEmpty(matcher.group(2)) ? "public" : matcher.group(2).replace(",", ""));
                    foreignKey.setTargetTable(matcher.group(3));
                }
                // 设置是否可延迟
                foreignKey.setDeferrablity(getDeferrable(constraint));
                foreignKey.setName(constraint.get("conname"));
                tableForeignKeyList.add(foreignKey);
            } else if ("x".equals(constraint.get("contype"))) {
                TableExclusion exclusion = new TableExclusion();
                String[] condefs = constraint.get("condef").split(" ");
                for (int i = 0; i < condefs.length; ++i) {
                    if ("EXCLUDE".equals(condefs[i]) && "USING".equals(condefs[i + 1])) {
                        exclusion.setIndexMethod(condefs[i + 2]);
                    }
                }
                String condef = constraint.get("condef");
                String[] exclude = condef.substring(condef.indexOf("(") + 1, condef.indexOf(")")).split(", ");
                List<TableExclusion.Content> contentList = new ArrayList<>();
                for (String ex : exclude) {
                    TableExclusion.Content content = new TableExclusion.Content();
                    String[] temp = ex.split(" ");
                    content.setColumnName(temp[0]);
                    for (int j = 1; j < temp.length; ++j) {
                        if ("desc".equals(temp[j]) || "asc".equals(temp[j])) {
                            content.setOrder(temp[j]);
                        }
                        if ("with".equalsIgnoreCase(temp[j])) {
                            content.setSymbol(temp[j + 1]);
                        }
                    }
                    contentList.add(content);
                }
                exclusion.setName(constraint.get("conname"));
                exclusion.setContentList(contentList);
                tableExclusionList.add(exclusion);
            } else if ("u".equals(constraint.get("contype"))) {
                TableUnique unique = new TableUnique();
                String[] condef = constraint.get("condef").split(" ");
                for (int i = 0; i < condef.length; ++i) {
                    if ("UNIQUE".equals(condef[i])) {
                        unique.setColumnName(condef[i + 1].replace("(", "").replace(")", ""));
                    }
                }
                unique.setDeferrablity(getDeferrable(constraint));
                unique.setName(constraint.get("conname"));
                tableUniqueList.add(unique);
            } else if ("c".equals(constraint.get("contype"))) {
                TableCheck check = new TableCheck();
                String condef = constraint.get("condef");
                String text = condef.substring(condef.indexOf("(") + 1, condef.lastIndexOf(")"));
                check.setText(text);
                if (condef.contains("NO INHERIT")) {
                    check.setNoInherit(true);
                }
                if (condef.contains("NOT VALID")) {
                    check.setNotValid(true);
                }
                check.setName(constraint.get("conname"));
                tableCheckList.add(check);
            }
            tableDto.setTableForeignKeyList(tableForeignKeyList);
            tableDto.setTableExclusionList(tableExclusionList);
            tableDto.setTableUniqueList(tableUniqueList);
            tableDto.setTableCheckList(tableCheckList);
        }
        // 继承信息
        List<TableInherit> tableInheritList =
            getTableInherit(path, port, databaseName, schemaName, tableName, tableDto.getOid());
        tableDto.setTableInheritList(tableInheritList);
        // 获取列信息
        tableDto.setColumnDtoList(
            this.listColumns(clusterId, namespace, middlewareName, databaseName, schemaName, tableName));
        return tableDto;
    }

    @Override
    public void addTable(String clusterId, String namespace, String middlewareName, TableDto tableDto) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> columnComment = new HashMap<>();
        StringBuilder pk = new StringBuilder();
        // 构造column sql语句
        for (ColumnDto columnDto : tableDto.getColumnDtoList()) {
            if (columnDto.getPrimaryKey() != null && columnDto.getPrimaryKey()) {
                pk.append(columnDto.getColumn()).append(",");
            }
            if (StringUtils.isNotEmpty(columnDto.getComment())) {
                columnComment.put(columnDto.getColumn(), columnDto.getComment());
            }
            sb.append(turnColumnToSql(columnDto)).append(",");
        }
        // 外键约束
        if (!CollectionUtils.isEmpty(tableDto.getTableForeignKeyList())) {
            for (TableForeignKey foreign : tableDto.getTableForeignKeyList()) {
                if (StringUtils.isEmpty(foreign.getTargetSchema())) {
                    foreign.setTargetSchema(tableDto.getSchemaName());
                }
                if (StringUtils.isEmpty(foreign.getOnDelete())) {
                    foreign.setOnDelete("NO ACTION");
                }
                if (StringUtils.isEmpty(foreign.getOnUpdate())) {
                    foreign.setOnUpdate("NO ACTION");
                }
                sb.append("CONSTRAINT \"").append(foreign.getName()).append("\" foreign key ( ")
                    .append(foreign.getColumn()).append(") ").append("REFERENCES ").append(foreign.getTargetSchema())
                    .append(".").append(foreign.getTargetTable()).append("(").append(foreign.getTarget()).append(")")
                    .append(" on update ").append(foreign.getOnUpdate()).append(" on delete ")
                    .append(foreign.getOnDelete()).append(" ").append(foreign.getDeferrablity());
                sb.append(",");
            }
        }
        // 排它约束
        if (!CollectionUtils.isEmpty(tableDto.getTableExclusionList())) {
            for (TableExclusion exclusion : tableDto.getTableExclusionList()) {
                sb.append("CONSTRAINT \"").append(exclusion.getName()).append("\" EXCLUDE using ")
                    .append(exclusion.getIndexMethod()).append(" ( ").append(exclusion.getContent()).append(" )");
                sb.append(",");
            }
        }
        // 唯一约束
        if (!CollectionUtils.isEmpty(tableDto.getTableUniqueList())) {
            for (TableUnique unique : tableDto.getTableUniqueList()) {
                sb.append("CONSTRAINT \"").append(unique.getName()).append("\" unique ").append("(")
                    .append(unique.getColumnName()).append(") ").append(unique.getDeferrablity());
                sb.append(",");
            }
        }
        // 检查约束
        if (!CollectionUtils.isEmpty(tableDto.getTableCheckList())) {
            for (TableCheck check : tableDto.getTableCheckList()) {
                sb.append("CONSTRAINT \"").append(check.getName()).append("\" check ").append("(")
                    .append(check.getText()).append(") ");
                if (check.getNoInherit() != null && check.getNoInherit()) {
                    sb.append("no inherit ");
                }
                if (check.getNotValid() != null && check.getNotValid()) {
                    sb.append("not valid");
                }
                sb.append(",");
            }
        }
        // 主键约束
        if (StringUtils.isNotEmpty(pk.toString())) {
            pk.deleteCharAt(pk.length() - 1);
            sb.append("CONSTRAINT \"").append("pk_").append(tableDto.getSchemaName()).append("_")
                .append(tableDto.getTableName()).append("\" PRIMARY KEY ").append("(").append(pk.toString())
                .append(")");
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
                inherit.deleteCharAt(inherit.length() - 1);
            }
        }
        // table comment
        if (tableDto.getDescription() != null) {
            columnComment.put(tableDto.getSchemaName() + "_" + tableDto.getTableName() + "_comment",
                tableDto.getDescription());
        }
        // fillFactory
        if (StringUtils.isEmpty(tableDto.getFillFactor())) {
            tableDto.setFillFactor("100");
        }
        // Tablespace
        if (StringUtils.isEmpty(tableDto.getTablespace())) {
            tableDto.setTablespace("pg_default");
        }
        JSONObject table = new JSONObject();
        table.put("database", tableDto.getDatabaseName());
        table.put("schema", tableDto.getSchemaName());
        table.put("table", tableDto.getTableName());
        table.put("column", sb.toString());
        table.put("inherit", inherit);
        table.put("owner", tableDto.getOwner());
        table.put("comment", columnComment);
        table.put("tableSpace", tableDto.getTablespace());
        table.put("fillFactory", tableDto.getFillFactor());
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
    public void updateTable(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName, String tableName, TableDto tableDto) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        // 修改表基本信息
        JSONObject updateTable =
            postgresqlClient.updateTable(path, port, databaseName, schemaName, tableName, tableDto);
        JSONObject err = updateTable.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_UPDATE_TABLE_FAILED, err.getString("Message"));
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
    public Integer getTableDataCount(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName, String tableName) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject tableDataCount = postgresqlClient.getTableDataCount(path, port, databaseName, schemaName, tableName);
        return Integer.valueOf(convertColumn(tableDataCount).get(0).get("count"));
    }

    @Override
    public List<Map<String, String>> getTableData(String clusterId, String namespace, String middlewareName,
        String databaseName, String schemaName, String tableName, QueryInfo queryInfo) {
        Integer index = 1;
        Integer pageSize = 10;
        if (queryInfo.getIndex() != null) {
            index = queryInfo.getIndex();
        }
        if (queryInfo.getPageSize() != null) {
            pageSize = queryInfo.getPageSize();
        }
        Integer offset = (index - 1) * index;
        StringBuilder sb = new StringBuilder();
        if (!CollectionUtils.isEmpty(queryInfo.getOrderDtoList())) {
            sb.append("order+by+");
            queryInfo.getOrderDtoList().forEach(
                orderDto -> sb.append(orderDto.getColumn()).append("+").append(orderDto.getOrder()).append(","));
            sb.deleteCharAt(sb.length() - 1);
        }
        String path = getPath(middlewareName, namespace);
        log.info(sb.toString());
        setPort(clusterId, namespace, middlewareName);
        JSONObject getTableData = postgresqlClient.getTableData(path, port, databaseName, schemaName, tableName,
            pageSize, offset, sb.toString());
        return convertColumn(getTableData);
    }

    @Override
    public void getTableCreateSql(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName, String tableName, HttpServletResponse response) throws IOException {
        TableDto tableDto = this.getTable(clusterId, namespace, middlewareName, databaseName, schemaName, tableName);
        StringBuilder sb = new StringBuilder();
        sb.append("create table ").append(tableDto.getTableName()).append(" (\n");
        for (int i = 0; i < tableDto.getColumnDtoList().size(); ++i) {
            ColumnDto columnDto = tableDto.getColumnDtoList().get(i);
            sb.append(columnDto.getColumn()).append(" ").append(columnDto.getDataType());
            if (StringUtils.isNotEmpty(columnDto.getSize()) && !"0".equals(columnDto.getSize())) {
                sb.append("(").append(columnDto.getSize()).append(")");
            }
            if (columnDto.getArray()) {
                sb.append("[] ");
            }
            if (!columnDto.getNullable()) {
                sb.append("not null ");
            }
            if (StringUtils.isNotEmpty(columnDto.getDefaultValue())) {
                sb.append("default ").append(columnDto.getDefaultValue());
            }
            if (i == tableDto.getColumnDtoList().size() - 1) {
                sb.append("\n");
            } else {
                sb.append(",").append("\n");
            }
        }
        sb.append(");");
        String fileRealName = tableName + ".sql";;
        FileDownloadUtil.downloadFile(response, uploadPath, fileRealName, sb.toString());
    }

    @Override
    public void getTableExcel(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName, String tableName, HttpServletResponse response) {
        try {
            List<ColumnDto> columnDtoList =
                listColumns(clusterId, namespace, middlewareName, databaseName, schemaName, tableName);
            ExcelUtil.createTableExcel(uploadPath, tableName, columnDtoList.stream().map(columnDto -> {
                com.harmonycloud.caas.common.model.dashboard.mysql.ColumnDto column =
                    new com.harmonycloud.caas.common.model.dashboard.mysql.ColumnDto();
                BeanUtils.copyProperties(columnDto, column);
                column.setColumnDefault(columnDto.getDefaultValue());
                return column;
            }).collect(Collectors.toList()));
            String fileName = tableName + ".xlsx";
            FileDownloadUtil.downloadFile(response, uploadPath, fileName);
        } catch (Exception e) {
            log.error("导出表结构Excel文件失败", e);
            throw new BusinessException(ErrorMessage.FAILED_TO_EXPORT_TABLE_EXCEL);
        }
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
            columnDto.setColumn(column.get("column_name"));
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
                columnDto.setDataType(column.get("array_data_type"));
            } else {
                columnDto.setArray(false);
                columnDto.setDataType(data_type);
            }
            columnDto.setNum(column.get("ordinal_position"));
            columnDto.setSize(column.get("character_maximum_length"));
            columnDto.setComment(column.get("col_description"));
            if (column.get("constraint_type") != null && "PRIMARY KEY".equals(column.get("constraint_type"))) {
                columnDto.setPrimaryKey(true);
            } else {
                columnDto.setPrimaryKey(false);
            }
            return columnDto;
        }).sorted(Comparator.comparing(columnDto -> Integer.valueOf(columnDto.getNum()))).collect(Collectors.toList());
    }

    @Override
    public void updateColumns(String clusterId, String namespace, String middlewareName, String databaseName,
        String schemaName, String tableName, TableDto tableDto) {
        List<ColumnDto> newColumnList = tableDto.getColumnDtoList();
        // 查询列信息
        String path = getPath(middlewareName, namespace);

        List<ColumnDto> columnDtoList =
            listColumns(clusterId, namespace, middlewareName, databaseName, schemaName, tableName);
        Map<String, ColumnDto> columnDtoMap =
            columnDtoList.stream().collect(Collectors.toMap(ColumnDto::getNum, columnDto -> columnDto));
        // 比较列信息 根据内容进行修改
        Map<String, Map<String, String>> change = new HashMap<>();
        // 获取新增或修改内容
        for (int i = 0; i < newColumnList.size(); ++i) {
            Map<String, String> anchor = new HashMap<>();
            ColumnDto newColumn = newColumnList.get(i);
            String num = newColumn.getNum();
            if (!columnDtoMap.containsKey(num) || Integer.parseInt(num) > newColumnList.size()) {
                // 新增列
                Map<String, String> add = new HashMap<>();
                add.put("add", turnColumnToSql(newColumn));
                change.put(newColumn.getColumn(), add);
                continue;
            }
            // 比较列不同
            ColumnDto column = columnDtoMap.get(num);
            // 比较是否开关数组/修改数据类型、修改数据长度
            if (!column.getArray().equals(newColumn.getArray()) || !column.getDataType().equals(newColumn.getDataType())
                || !column.getSize().equals(newColumn.getSize())) {
                anchor.put("dataType", newColumn.getDataType());
                anchor.put("array", newColumn.getArray().toString());
                if (!"0".equals(newColumn.getSize())) {
                    anchor.put("size", newColumn.getSize());
                }
            }
            // 比较是否可空
            if (!column.getNullable().equals(newColumn.getNullable())) {
                anchor.put("nullAble", newColumn.getNullable().toString());
            }
            // 比较是否主键
            if (!column.getPrimaryKey().equals(newColumn.getPrimaryKey())) {
                anchor.put("primary", newColumn.getPrimaryKey().toString());
            }
            // 比较默认值是否相同
            if (!column.getDefaultValue().equals(newColumn.getDefaultValue())) {
                anchor.put("default", newColumn.getDefaultValue());
            }
            // 比较备注
            if (!column.getComment().equals(newColumn.getComment())) {
                anchor.put("comment", newColumn.getComment());
            }
            // 比较列名称
            if (!column.getColumn().equals(newColumn.getColumn())) {
                anchor.put("name", column.getColumn());
                anchor.put("newName", newColumn.getColumn());
            }
            change.put(column.getColumn(), anchor);
            columnDtoMap.remove(num);
        }
        // 获取删除列
        if (!CollectionUtils.isEmpty(columnDtoMap)) {
            Map<String, String> delete = new HashMap<>();
            columnDtoMap.forEach((k, v) -> {
                delete.put(v.getColumn(), "delete");
                change.put(v.getColumn(), delete);
            });
        }
        // 更新列信息
        JSONObject object = new JSONObject();
        object.put("change", change);
        JSONObject updateColumn =
            postgresqlClient.updateColumn(path, port, databaseName, schemaName, tableName, object);
        JSONObject err = updateColumn.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_UPDATE_TABLE_FAILED, err.getString("Message"));
        }
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
                // 转化列和目标列为字符串
                key.setColumnName(key.getColumn());
                key.setTargetColumn(key.getTarget());
                if (StringUtils.isEmpty(key.getTargetSchema())) {
                    key.setTargetSchema(tableDto.getSchemaName());
                }
                // 创建外键
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
            // 合成排它约束内容
            key.setExclude(key.getContent());
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
                JSONObject createUnique =
                    postgresqlClient.createUnique(path, port, tableDto.getDatabaseName(), tableDto.getSchemaName(),
                        tableDto.getTableName(), key.getName(), key.getColumnName(), key.getDeferrablity());
                if (createUnique.get("err") != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_CREATE_UNIQUE_FAILED,
                        createUnique.getJSONObject("err").getString("Message"));
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
                String inherit = "";
                String vaild = "";
                if (key.getNoInherit()) {
                    inherit = "NO INHERIT";
                }
                if (key.getNotValid()) {
                    vaild = "NOT VALID";
                }
                JSONObject createCheck = postgresqlClient.createCheck(path, port, tableDto.getDatabaseName(),
                    tableDto.getSchemaName(), tableDto.getTableName(), key.getName(), key.getText(), inherit, vaild);
                if (createCheck.get("err") != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_CREATE_CHECK_FAILED,
                        createCheck.getJSONObject("err").getString("Message"));
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
        // 查询已有的继承关系
        List<TableInherit> existInheritList = getTableInherit(path, port, tableDto.getDatabaseName(),
            tableDto.getSchemaName(), tableDto.getTableName(), tableDto.getOid());
        // 添加继承关系
        for (TableInherit key : tableDto.getTableInheritList()) {
            if (existInheritList.stream().noneMatch(tableInherit -> tableInherit.equals(key))) {
                JSONObject createForeignKey = postgresqlClient.addInherit(path, port, tableDto.getDatabaseName(),
                    tableDto.getSchemaName(), tableDto.getTableName(), key.getSchemaName(), key.getTableName());
                if (createForeignKey.get("err") != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_ADD_TABLE_INHERIT_FAILED,
                        createForeignKey.getJSONObject("err").getString("Message"));
                }
            }
        }
        // 取消继承关系
        for (TableInherit key : existInheritList) {
            if (tableDto.getTableInheritList().stream().noneMatch(tableInherit -> tableInherit.equals(key))) {
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
        String inherit = middlewareUserDto.isInherit() ? "INHERIT" : "NOINHERIT";
        JSONObject addUser = postgresqlClient.addUser(path, port, middlewareUserDto.getUsername(),
            middlewareUserDto.getPassword(), inherit);
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
                throw new BusinessException(ErrorMessage.POSTGRESQL_GRANT_USER_DATABASE_FAILED,
                    err.getString("Message"));
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
                    throw new BusinessException(ErrorMessage.POSTGRESQL_REVOKE_USER_TABLE_FAILED,
                        err.getString("Message"));
                }
            } else if (StringUtils.isNotEmpty(middlewareUserAuthority.getSchema())) {
                // 赋权schema
                String privileges =
                    PostgresqlPrivilegeEnum.findSchemaPrivilege(middlewareUserAuthority.getAuthority()).getPrivilege();
                JSONObject revokeUserSchema = postgresqlClient.revokeUserSchema(path, port, username,
                    middlewareUserAuthority.getDatabase(), middlewareUserAuthority.getSchema(), privileges);
                JSONObject err = revokeUserSchema.getJSONObject("err");
                if (err != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_REVOKE_USER_SCHEMA_FAILED,
                        err.getString("Message"));
                }
            } else if (StringUtils.isNotEmpty(middlewareUserAuthority.getDatabase())) {
                // 赋权database
                String privileges = PostgresqlPrivilegeEnum
                    .findDatabasePrivilege(middlewareUserAuthority.getAuthority()).getPrivilege();
                JSONObject revokeUserDatabase = postgresqlClient.revokeUserDatabase(path, port, username,
                    middlewareUserAuthority.getDatabase(), privileges);
                JSONObject err = revokeUserDatabase.getJSONObject("err");
                if (err != null) {
                    throw new BusinessException(ErrorMessage.POSTGRESQL_REVOKE_USER_DATABASE_FAILED,
                        err.getString("Message"));
                }
            }
        }
    }

    @Override
    public List<MiddlewareUserAuthority> userAuthority(String clusterId, String namespace, String middlewareName,
        String username, String oid) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        // 获取库权限
        JSONObject listDatabases = postgresqlClient.listDatabases(path, port);
        List<Map<String, String>> databaseList = convertColumn(listDatabases);
        List<MiddlewareUserAuthority> userDatabaseAuthorityList = databaseList.stream().map(database -> {
            MiddlewareUserAuthority databaseAuthority = new MiddlewareUserAuthority();
            databaseAuthority.setDatabase(database.get("datname"));
            if (database.get("datdba").equals(oid)) {
                databaseAuthority.setAuthority("owner");
            } else {
                String datAcl = database.get("datacl");
                String authority = PostgresqlAuthorityUtil.checkAuthority(datAcl, username);
                if (authority.contains("*")) {
                    databaseAuthority.setGrantAble(true);
                    authority = authority.replace("*", "");
                }
                databaseAuthority.setAuthority(authority);
            }
            return databaseAuthority;
        }).filter(database -> !database.getDatabase().equals(TEMPLATE0) && !database.getDatabase().equals(TEMPLATE1))
            .collect(Collectors.toList());
        // 获取模式权限
        List<MiddlewareUserAuthority> userSchemaAuthorityList = new ArrayList<>();
        for (MiddlewareUserAuthority database : userDatabaseAuthorityList) {
            JSONObject listSchemas = postgresqlClient.listSchemas(path, port, database.getDatabase());
            List<Map<String, String>> schemaList = convertColumn(listSchemas);
            schemaList.forEach(schema -> {
                MiddlewareUserAuthority schemaAuthority = new MiddlewareUserAuthority();
                schemaAuthority.setDatabase(database.getDatabase());
                schemaAuthority.setSchema(schema.get("nspname"));
                if (schema.get("nspowner").equals(oid)) {
                    schemaAuthority.setAuthority("owner");
                } else {
                    String nspAcl = schema.get("nspacl");
                    String authority = PostgresqlAuthorityUtil.checkAuthority(nspAcl, username);
                    if (authority.contains("*")) {
                        schemaAuthority.setGrantAble(true);
                        authority = authority.replace("*", "");
                    }
                    schemaAuthority.setAuthority(authority);
                }
                userSchemaAuthorityList.add(schemaAuthority);
            });
        }
        // 获取table权限
        List<MiddlewareUserAuthority> userTableAuthorityList = new ArrayList<>();
        for (MiddlewareUserAuthority databaseSchema : userDatabaseAuthorityList) {
            JSONObject listTables = postgresqlClient.listAllTables(path, port, databaseSchema.getDatabase());
            List<Map<String, String>> tableList = convertColumn(listTables);
            tableList = tableList.stream()
                .filter(table -> userSchemaAuthorityList.stream()
                    .anyMatch(schemaAuthority -> table.get("nspname").equals(schemaAuthority.getSchema())
                        && schemaAuthority.getDatabase().equals(databaseSchema.getAuthority())))
                .collect(Collectors.toList());
            tableList.forEach(table -> {
                MiddlewareUserAuthority tableAuthority = new MiddlewareUserAuthority();
                tableAuthority.setDatabase(databaseSchema.getDatabase());
                tableAuthority.setSchema(table.get("nspname"));
                if (table.get("relowner").equals(oid)) {
                    tableAuthority.setAuthority("owner");
                } else {
                    String relAcl = table.get("relacl");
                    String authority = PostgresqlAuthorityUtil.checkAuthority(relAcl, username);
                    if (authority.contains("*")) {
                        tableAuthority.setGrantAble(true);
                        authority = authority.replace("*", "");
                    }
                    tableAuthority.setAuthority(authority);
                }
                userTableAuthorityList.add(tableAuthority);
            });
        }
        // 汇总权限
        List<MiddlewareUserAuthority> userAuthorityList = new ArrayList<>();
        userAuthorityList.addAll(userDatabaseAuthorityList);
        userAuthorityList.addAll(userSchemaAuthorityList);
        userAuthorityList.addAll(userTableAuthorityList);
        return userAuthorityList;
    }

    @Override
    public void resetPassword(String clusterId, String namespace, String middlewareName, String username) {
        this.updatePassword(clusterId, namespace, middlewareName, username, "zeus123.com");
    }

    @Override
    public void updatePassword(String clusterId, String namespace, String middlewareName, String username,
        String password) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject updatePassword = postgresqlClient.updatePassword(path, port, username, password);
        JSONObject err = updatePassword.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_DELETE_USER_FAILED, err.getString("Message"));
        }
    }

    @Override
    public void enableUser(String clusterId, String namespace, String middlewareName, String username, Boolean enable) {
        String path = getPath(middlewareName, namespace);
        setPort(clusterId, namespace, middlewareName);
        JSONObject enableUser = postgresqlClient.enableUser(path, port, username, enable);
        JSONObject err = enableUser.getJSONObject("err");
        if (err != null) {
            throw new BusinessException(ErrorMessage.POSTGRESQL_DELETE_USER_FAILED, err.getString("Message"));
        }
    }

    @Override
    public List<String> listEncoding() {
        return Arrays.stream(EncodingEnum.values()).map(EncodingEnum::getName).collect(Collectors.toList());
    }

    @Override
    public List<String> listDataType() {
        return Arrays.stream(PostgresqlDataTypeEnum.values()).map(PostgresqlDataTypeEnum::getName)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> listCollate() {
        return Arrays.stream(PostgresqlCollateEnum.values()).map(PostgresqlCollateEnum::getName)
            .collect(Collectors.toList());
    }

    /**
     * 获取表继承关系
     */
    public List<TableInherit> getTableInherit(String path, String port, String databaseName, String schemaName,
        String tableName, String tableOid) {
        JSONObject getInherit = postgresqlClient.getInherit(path, port, databaseName, schemaName, tableName, tableOid);
        if (getInherit.getJSONArray("data") == null) {
            return new ArrayList<>();
        }
        List<Map<String, String>> inheritList = convertColumn(getInherit);
        List<TableInherit> tableInheritList = new ArrayList<>();
        inheritList.forEach(inherit -> {
            TableInherit tableInherit = new TableInherit();
            tableInherit.setOid(inherit.get("inhparent"));
            tableInherit.setTableName(inherit.get("relname"));
            tableInherit.setSchemaName(inherit.get("nspname"));
            tableInheritList.add(tableInherit);
        });
        return tableInheritList;
    }

    public List<Map<String, String>> convertColumn(JSONObject jsonObject) {
        dealWithErr(jsonObject, true);

        JSONArray data = jsonObject.getJSONArray("data");
        JSONArray column = jsonObject.getJSONArray("column");

        List<Map<String, String>> res = new ArrayList<>();
        List<String> columnList = column.toJavaList(String.class);
        if (data == null) {
            // 处理data为空的情况
            Map<String, String> map = new HashMap<>();
            for (String s : columnList) {
                map.put(s, "");
            }
            res.add(map);
        } else {
            for (int i = 0; i < data.size(); ++i) {
                List<String> dataList = data.getJSONArray(i).toJavaList(String.class);
                Map<String, String> map = new HashMap<>();
                for (int j = 0; j < dataList.size(); ++j) {
                    map.put(columnList.get(j), dataList.get(j));
                }
                res.add(map);
            }
        }
        return res;
    }

    public String turnColumnToSql(ColumnDto columnDto) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(columnDto.getColumn()).append("\" ");
        if (columnDto.getInc() != null && columnDto.getInc()) {
            sb.append("serial");
        } else {
            sb.append(columnDto.getDataType());
        }
        if (columnDto.getArray() != null && columnDto.getArray()) {
            sb.append("[]");
        }
        if (StringUtils.isNotEmpty(columnDto.getSize())) {
            sb.append("(").append(columnDto.getSize()).append(")");
        }
        sb.append(" ");
        if (StringUtils.isNotEmpty(columnDto.getDefaultValue())) {
            sb.append("default ").append("'").append(columnDto.getDefaultValue()).append("'");
        }
        sb.append(" ");
        if (columnDto.getNullable() != null && !columnDto.getNullable()) {
            sb.append("not null ");
        }
        if (StringUtils.isNotEmpty(columnDto.getCollate())) {
            sb.append("COLLATE ").append(columnDto.getCollate());
        }
        return sb.toString();
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

    public String dealWithErr(JSONObject jsonObject, Boolean exception) {
        String errString = "";
        JSONObject err = jsonObject.getJSONObject("err");
        if (err != null) {
            if (err.containsKey("Message")) {
                errString = err.getString("Message");
            } else {
                errString = err.getJSONObject("Err").getString("Err");
            }
            if (exception) {
                throw new BusinessException(ErrorMessage.POSTGRESQL_SELECT_FAILED, errString);
            }
        }
        return errString;
    }

    public String getDeferrable(Map<String, String> constraint) {
        if (constraint.get("condef").contains("DEFERRABLE INITIALLY DEFERRED")) {
            return "DEFERRABLE INITIALLY DEFERRED";
        } else if (constraint.get("condef").contains("DEFERRABLE INITIALLY IMMEDIATE")) {
            return "DEFERRABLE INITIALLY IMMEDIATE";
        } else {
            return "NOT DEFERRABLE";
        }
    }

    public String getPath(String middlewareName, String namespace) {
        return middlewareName + "." + namespace;
        //return middlewareName;
    }

}
