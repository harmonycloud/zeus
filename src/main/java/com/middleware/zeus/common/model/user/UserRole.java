package com.middleware.zeus.common.model.user;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/7/23 10:25 上午
 */
@Accessors(chain = true)
@Data
@ApiModel("用户角色信息表")
public class UserRole {

    private String userName;

    private String organId;

    private String projectId;

    private Integer roleId;

    private String roleName;

    private Integer weight;

    private Map<String, String> power;

    private String organName;

    private String projectName;

}
