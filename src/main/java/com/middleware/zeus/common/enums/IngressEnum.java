package com.middleware.zeus.common.enums;

/**
 * @author xutianhong
 * @Date 2021/10/29 3:29 下午
 */
public enum IngressEnum {

    NGINX("nginx"),
    TRAEFIK("traefik");

    private String name;

    IngressEnum(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
