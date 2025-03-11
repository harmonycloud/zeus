package com.middleware.zeus.common.model.middleware.mongodb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/3/3 10:49 AM
 */
@Accessors(chain = true)
@Data
@ApiModel("企业版mongodb项目信息")
@NoArgsConstructor
public class MongodbProjectDo extends MongodbBaseDo {

    @ApiModelProperty("项目id")
    private String id;

    @ApiModelProperty("组织id")
    private String orgId;

    @ApiModelProperty("项目名称")
    private String name;

    public MongodbProjectDo(String protocol, String path, String port, String publicKey, String privateKey, String id, String orgId, String name) {
        super(protocol, path, port, publicKey, privateKey);
        this.id = id;
        this.orgId = orgId;
        this.name = name;
    }

}
