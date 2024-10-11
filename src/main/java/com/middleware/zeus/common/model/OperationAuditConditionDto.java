package com.middleware.zeus.common.model;

import com.skyview.language.annotations.DirectTranslate;
import com.skyview.language.annotations.TranslateObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2024/10/11 4:02 PM
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "OperationAuditConditionDto")
public class OperationAuditConditionDto {

    @ApiModelProperty("方法名称列表")
    private List<String> methods;

    @ApiModelProperty("角色列表")
    @TranslateObject
    private List<Role> roles;

    @ApiModelProperty("模块列表")
    @TranslateObject
    private List<Modules> modulesList;

    @Accessors(chain = true)
    @Data
    public class Modules {

        @ApiModelProperty("模块名称")
        @DirectTranslate(groupName="operation_audit",uniqueKeyName="name", keyName="moduleChDesc")
        private String name;
        @ApiModelProperty("子模块")
        @TranslateObject
        private List<ChildModules> childModules;

        @Accessors(chain = true)
        @Data
        public class ChildModules {

            @ApiModelProperty("子模块名称")
            @DirectTranslate(groupName="operation_audit",uniqueKeyName="name", keyName="childModuleChDesc")
            private String name;
        }
    }

    @Accessors(chain = true)
    @Data
    public class Role {

        @ApiModelProperty("角色名称")
        @DirectTranslate(groupName="operation_audit",uniqueKeyName="name", keyName="roleName")
        private String name;
    }


}
