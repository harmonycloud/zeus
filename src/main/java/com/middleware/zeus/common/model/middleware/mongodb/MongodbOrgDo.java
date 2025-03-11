package com.middleware.zeus.common.model.middleware.mongodb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/3/3 10:33 AM
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
@ApiModel("企业版mongodb角色")
public class MongodbOrgDo extends MongodbBaseDo {

    @ApiModelProperty("组织id")
    private String id;

    @ApiModelProperty("组织名称")
    private String name;

    @ApiModelProperty("是否已删除")
    private Boolean isDeleted;

    public MongodbOrgDo(String protocol, String path, String port, String publicKey, String privateKey, String id, String name) {
        super(protocol, path, port, publicKey, privateKey);
        this.id = id;
        this.name = name;
    }

}
