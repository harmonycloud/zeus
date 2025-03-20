package com.middleware.zeus.common.model.middleware.mongodb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2025/3/20 9:42 AM
 */
@Accessors(chain = true)
@Data
@ApiModel("企业版mongodb用户")
@NoArgsConstructor
public class MongodbOrgInviteUserDo extends MongodbBaseDo {

    @ApiModelProperty("组织id")
    private String organizationId;
    
    @ApiModelProperty("用户名称")
    private String username;

    @ApiModelProperty("角色")
    private List<String> roles;
    
    public MongodbOrgInviteUserDo(String protocol, String path, String port, String publicKey, String privateKey,
        String organizationId, String username, List<String> roles) {
        super(protocol, path, port, publicKey, privateKey);
        this.organizationId = organizationId;
        this.username = username;
        this.roles = roles;
    }

}
