package com.middleware.zeus.common.enums.middleware;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/04/06
 */
public enum ElasticSearchRoleEnum {

    /**
     * es的角色
     */
    MASTER("master", "master"),
    KIBANA("kibana", "kibana"),
    DATA("data", "数据"),
    CLIENT("client", "客户端"),
    COLD("cold", "冷节点"),
    ;

    private static final Map<String, ElasticSearchRoleEnum> map = new HashMap<>();

    private final String role;
    private final String name;

    static {
        for (ElasticSearchRoleEnum roleEnum : ElasticSearchRoleEnum.values()) {
            map.put(roleEnum.getRole(), roleEnum);
        }
    }

    public static ElasticSearchRoleEnum findByMode(String role) {
        if (StringUtils.isBlank(role)) {
            throw new IllegalArgumentException("elasticSearch role is illegal");
        }
        ElasticSearchRoleEnum roleEnum = map.get(role);
        if (roleEnum == null) {
            throw new IllegalArgumentException("elasticSearch role is illegal");
        }
        return roleEnum;
    }

    public static Map<String, ElasticSearchRoleEnum> getValues() {
        return map;
    }

    ElasticSearchRoleEnum(String role, String name) {
        this.role = role;
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

}
