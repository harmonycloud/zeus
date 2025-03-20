package com.middleware.zeus.common.model.middleware.mongodb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2025/3/20 10:12 AM
 */
@Accessors(chain = true)
@Data
@ApiModel("企业版mongodb用户")
@NoArgsConstructor
public class MongodbProjectInviteUserDo extends MongodbBaseDo {

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("用户列表")
    public List<MongodbUserDo> users;
    
    public MongodbProjectInviteUserDo(String protocol, String path, String port, String publicKey, String privateKey,
        List<MongodbUserDo> users) {
        super(protocol, path, port, publicKey, privateKey);
        this.users = users;
    }
}
