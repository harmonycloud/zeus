package com.middleware.zeus.common.enums.middleware;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2022/10/13 2:14 下午
 */
public enum PostgresqlAuthorityEnum {

    SELECT("r"),
    INSERT("a"),
    UPDATE("w"),
    DELETE("d"),
    TRUNCATE("D"),
    REFERENCES("x"),
    TRIGGER( "t"),
    CREATE("C"),
    CONNECT("c"),
    TEMPORARY( "T"),
    EXECUTE( "X"),
    USAGE( "U")
    ;

    private String authority;

    PostgresqlAuthorityEnum(String authority){
        this.authority = authority;
    }

    private static final Map<String, PostgresqlAuthorityEnum> authorityEnumMap = new HashMap<>();

    static {
        for (PostgresqlAuthorityEnum postgresqlAuthorityEnum : PostgresqlAuthorityEnum.values()){
            authorityEnumMap.put(postgresqlAuthorityEnum.getAuthority(), postgresqlAuthorityEnum);
        }
    }

    public static List<String> getAuthorityList(String[] acls){
        return Arrays.stream(acls).map(acl -> authorityEnumMap.get(acl).authority).collect(Collectors.toList());
    }

    public String getAuthority(){
        return authority;
    }



}
