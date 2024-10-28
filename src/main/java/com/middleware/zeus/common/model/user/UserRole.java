package com.middleware.zeus.common.model.user;

import com.skyview.language.annotations.Translate;
import com.skyview.language.annotations.TranslateGroupInfo;
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
@TranslateGroupInfo(name="role",uniqueKeyName="weight")
public class UserRole {

    private String userName;

    private String organId;

    private String projectId;

    private Integer roleId;

    @Translate(keyName="name")
    private String roleName;

    private String roleType;

    private Integer weight;

    private Map<String, String> power;

    private String organName;

    private String projectName;

}
