package com.middleware.zeus.common.model.middleware.mongodb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2025/3/3 10:24 AM
 */
@Accessors(chain = true)
@Data
@ApiModel("企业版mongodb用户")
@NoArgsConstructor
public class MongodbUserDo extends MongodbBaseDo {

    @ApiModelProperty("用户id")
    private String id;

    @ApiModelProperty("组织id")
    private String organizationId;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("用户名称")
    private String username;

    @ApiModelProperty("邮箱地址")
    private String emailAddress;

    @ApiModelProperty("名")
    private String firstName;

    @ApiModelProperty("姓")
    private String lastName;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("角色")
    private List<MongodbRoleDo> roles;
    
    public MongodbUserDo(String protocol, String path, String port, String publicKey, String privateKey, String id,
        String username, String emailAddress, String firstName, String lastName, String password,
        List<MongodbRoleDo> roles) {
        super(protocol, path, port, publicKey, privateKey);
        this.id = id;
        this.username = username;
        this.emailAddress = emailAddress;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.roles = roles;
    }
}
