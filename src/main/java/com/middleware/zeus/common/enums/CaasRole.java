package com.middleware.zeus.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/3/28 1:57 下午
 */
public enum CaasRole {

    ADMIN("admin", 1, 1, "超级管理员"),
    PM("pm", 2, 3, "项目管理员"),
    DEV("dev", 3, 4, "运维人员"),
    TEST("test", 3, 4, "运维人员"),
    OPS("ops", 3, 4, "运维人员"),
    UAT("uat", 3, 4, "运维人员"),
    TM("tm", 5, 2, "租户管理员"),
    ;

    private String name;
    private Integer id;
    private Integer weight;
    private String roleName;

    private static final Map<String, CaasRole> caasRoleMap = new HashMap<>();

    static {
        for (CaasRole caasRole : CaasRole.values()) {
            caasRoleMap.put(caasRole.getName(), caasRole);
        }
    }

    public static CaasRole findByName(String name) {
        return caasRoleMap.get(name);
    }


    CaasRole(String name, Integer id, Integer weight, String roleName) {
        this.name = name;
        this.id = id;
        this.weight = weight;
        this.roleName = roleName;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }



}
