package com.middleware.zeus.common.enums.middleware;

/**
 * @author xutianhong
 * @Date 2022/10/27 2:02 下午
 */
public enum PostgresqlCollateEnum {

    EN_US_UTF8("en_US.utf8"),
    ;

    private String name;


    PostgresqlCollateEnum(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

}
