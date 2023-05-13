package com.middleware.zeus.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/10/20 1:59 下午
 */
public enum PostgresqlPrivilegeEnum {

    DATABASE_READ_ONLY("readOnly", "CONNECT"),
    DATABASE_READ_WRITE("readWrite", "CONNECT,CREATE,TEMPORARY"),
    SCHEMA_READ_ONLY("readOnly", "USAGE"),
    SCHEMA_READ_WRITE("readWrite", "USAGE,CREATE"),
    TABLE_READ_ONLY("readOnly", "SELECT"),
    TABLE_READ_WRITE("readWrite", "SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER")
    ;


    private final String authority;
    private final String privilege;

    private static final Map<String, PostgresqlPrivilegeEnum> databasePrivilegeMap = new HashMap<>();
    private static final Map<String, PostgresqlPrivilegeEnum> schemaPrivilegeMap = new HashMap<>();
    private static final Map<String, PostgresqlPrivilegeEnum> tablePrivilegeMap = new HashMap<>();

    static {
        databasePrivilegeMap.put("readOnly", DATABASE_READ_ONLY);
        databasePrivilegeMap.put("readWrite", DATABASE_READ_WRITE);

        schemaPrivilegeMap.put("readOnly", SCHEMA_READ_ONLY);
        schemaPrivilegeMap.put("readWrite",SCHEMA_READ_WRITE);

        tablePrivilegeMap.put("readOnly", TABLE_READ_ONLY);
        tablePrivilegeMap.put("readWrite", TABLE_READ_WRITE);
    }

    public static PostgresqlPrivilegeEnum findDatabasePrivilege(String authority){
        return databasePrivilegeMap.get(authority);
    }

    public static PostgresqlPrivilegeEnum findSchemaPrivilege(String authority){
        return schemaPrivilegeMap.get(authority);
    }

    public static PostgresqlPrivilegeEnum findTablePrivilege(String authority){
        return tablePrivilegeMap.get(authority);
    }

    PostgresqlPrivilegeEnum(String authority, String privilege) {
        this.authority = authority;
        this.privilege = privilege;
    }

    public String getAuthority() {
        return authority;
    }

    public String getPrivilege() {
        return privilege;
    }


}
