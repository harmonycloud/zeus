package com.harmonycloud.zeus.integration.registry.bean.harbor;

import com.alibaba.fastjson.annotation.JSONField;

import lombok.Data;

/**
 * @author dengyulong
 * @date 2020/11/25
 */
@Data
public class V1CurrentUser {

    @JSONField(name = "user_id")
    private Integer userId;

    private String username;

    @JSONField(name = "realname")
    private String realName;

    private String email;

    private String password;

    @JSONField(name = "password_version")
    private String passwordVersion;

    private String comment;

    @JSONField(name = "role_id")
    private Integer roleId;

    @JSONField(name = "role_name")
    private String roleName;

    @JSONField(name = "has_admin_role")
    private boolean hasAdminRole;

    private boolean deleted;

    @JSONField(name = "reset_uuid")
    private String resetUuid;

    @JSONField(name = "creation_time")
    private String creationTime;

    @JSONField(name = "update_time")
    private String updateTime;

}
