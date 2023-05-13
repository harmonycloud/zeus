package com.middleware.zeus.common.model;

import com.middleware.zeus.common.model.middleware.Namespace;
import com.middleware.zeus.common.model.user.UserDto;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/01/19
 */
@Accessors(chain = true)
@Data
public class ProjectDTO {

    private String tenantId;
    private String tenantName;
    private String tenantAliasName;
    private String projectId;
    private String projectName;
    private String projectAliasName;

    private Integer userRoleId;
    private String description;
    private Date createTime;
    private List<Namespace> namespaces;
    private Integer memberCount;
    private Integer namespaceCount;
    private List<UserDto> userDtos;

}
