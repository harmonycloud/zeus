package com.middleware.zeus.common.enums;

import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/7/25 8:39 下午
 */
@Slf4j
public enum RoleBindingEnum {

    PROJECT_MANAGER(2, "zeus-project-manager"),
    OPERATIONS_STAFF(3, "zeus-operations-staff"),
    NORMAL_USER(4, "zeus-normal-user")
    ;

    private Integer roleId;
    private String clusterRole;

    RoleBindingEnum(Integer roleId, String clusterRole){
        this.roleId = roleId;
        this.clusterRole = clusterRole;
    }

    private static final Map<Integer, RoleBindingEnum> map = new HashMap<>();

    static {
        for (RoleBindingEnum roleBindingEnum : RoleBindingEnum.values()) {
            map.put(roleBindingEnum.getRoleId(), roleBindingEnum);
        }
    }

    public static RoleBindingEnum findByRoleId(Integer roleId){
        if (roleId == null || map.get(roleId) == null) {
            return null;
        }
        return map.get(roleId);
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getClusterRole() {
        return clusterRole;
    }

    public void setClusterRole(String clusterRole) {
        this.clusterRole = clusterRole;
    }
}
