package com.middleware.zeus.common.enums.registry;

/**
 * @author dengyulong
 * @date 2020/12/11
 * harbor角色
 */
public enum HarborRoleEnum {

    NONE(0, "无权限"),
    PROJECT_ADMIN(1, "项目管理员"),
    DEVELOPER(2, "开发人员"),
    GUEST(3, "访客"),
    MASTER(4, "维护人员"),
    LIMITED_GUEST(5, "访客"),
    ;

    private final int roleId;
    private final String roleNameCh;

    HarborRoleEnum(int roleId, String roleNameCh) {
        this.roleId = roleId;
        this.roleNameCh = roleNameCh;
    }

    public int getRoleId() {
        return roleId;
    }

    public String getRoleNameCh() {
        return roleNameCh;
    }

}
