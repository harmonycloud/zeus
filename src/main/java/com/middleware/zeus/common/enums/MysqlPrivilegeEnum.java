package com.middleware.zeus.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2022/3/29 10:01 上午
 */
public enum MysqlPrivilegeEnum {

    // 全局权限
    GLOBAL_READ_ONLY(1, "PROCESS,REPLICATION SLAVE,REPLICATION CLIENT"),
    GLOBAL_READ_WRITE(2, "CREATE ROUTINE,ALTER ROUTINE,PROCESS,REPLICATION SLAVE,REPLICATION CLIENT "),
    GLOBAL_DDL(3, "CREATE ROUTINE,ALTER ROUTINE,PROCESS,REPLICATION SLAVE,REPLICATION CLIENT"),
    GLOBAL_DML(4, "PROCESS,REPLICATION SLAVE,REPLICATION CLIENT"),

    // 数据库权限
    DB_READ_ONLY(1, "SELECT,LOCK TABLES,SHOW VIEW"),
    DB_READ_WRITE(2, "SELECT,INSERT,UPDATE,DELETE,EXECUTE,SHOW VIEW"),
    DB_DML(3, "SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,REFERENCES,INDEX,ALTER,CREATE TEMPORARY TABLES," +
            "LOCK TABLES,EXECUTE,CREATE VIEW,SHOW VIEW,EVENT,TRIGGER,CREATE ROUTINE,ALTER ROUTINE"),
    GLOBAL_GRANT(4, " GRANT OPTION "),

    // 表权限
    TABLE_READ_ONLY(1, "SELECT,SHOW VIEW"),
    TABLE_READ_WRITE(2, "SELECT,INSERT,UPDATE,DELETE"),
    TABLE_DML(3, "SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,ALTER,SHOW VIEW,TRIGGER");

    private final int authority;
    private final String privilege;

    private static final Map<Integer, MysqlPrivilegeEnum> globalPrivilegeMap = new HashMap<>();
    private static final Map<Integer, MysqlPrivilegeEnum> dbPrivilegeMap = new HashMap<>();
    private static final Map<Integer, MysqlPrivilegeEnum> tablePrivilegeMap = new HashMap<>();

    static {
        globalPrivilegeMap.put(1, GLOBAL_READ_ONLY);
        globalPrivilegeMap.put(2, GLOBAL_READ_WRITE);
        globalPrivilegeMap.put(3, GLOBAL_DDL);
        globalPrivilegeMap.put(4, GLOBAL_DML);


        dbPrivilegeMap.put(1, DB_READ_ONLY);
        dbPrivilegeMap.put(2, DB_READ_WRITE);
        dbPrivilegeMap.put(3, DB_DML);
        dbPrivilegeMap.put(4, GLOBAL_GRANT);

        tablePrivilegeMap.put(1, TABLE_READ_ONLY);
        tablePrivilegeMap.put(2, TABLE_READ_WRITE);
        tablePrivilegeMap.put(3, TABLE_DML);
    }

    public static String findGlobalPrivilege(int authority) {
        return globalPrivilegeMap.get(authority).privilege + ",GRANT OPTION ";
    }

    public static String findGlobalPrivilege(int authority, Boolean grantAble) {
        if (grantAble != null && grantAble) {
            return globalPrivilegeMap.get(authority).privilege + ",GRANT OPTION ";
        } else {
            return globalPrivilegeMap.get(authority).privilege;
        }
    }

    public static String findDbPrivilege(int authority) {
        return dbPrivilegeMap.get(authority).privilege + ",GRANT OPTION ";
    }

    public static String findDbPrivilege(int authority, Boolean grantAble) {
        if (grantAble != null && grantAble) {
            return dbPrivilegeMap.get(authority).privilege + ",GRANT OPTION ";
        } else {
            return dbPrivilegeMap.get(authority).privilege;
        }
    }

    public static String findTablePrivilege(int authority) {
        return tablePrivilegeMap.get(authority).privilege + ",GRANT OPTION ";
    }

    public static String findTablePrivilege(int authority, Boolean grantAble) {
        if (grantAble != null && grantAble) {
            return tablePrivilegeMap.get(authority).privilege + ",GRANT OPTION ";
        } else {
            return tablePrivilegeMap.get(authority).privilege;
        }
    }

    MysqlPrivilegeEnum(int authority, String privilege) {
        this.authority = authority;
        this.privilege = privilege;
    }

    public int getAuthority() {
        return authority;
    }

    public String getPrivilege() {
        return privilege;
    }

}
