package com.middleware.zeus.common.model.middleware;

import com.skyview.language.annotations.DirectTranslate;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author liyinlong
 * @since 2023/5/5 3:08 下午
 */
@NoArgsConstructor
@Accessors(chain = true)
@Data
@ApiModel("克隆记录")
public class MiddlewareBackupRestore {

    @ApiModelProperty("克隆记录名称")
    private String restoreName;

    @ApiModelProperty("所属可用区")
    @DirectTranslate(groupName="active_area",uniqueKeyName="activeArea", keyName="aliasName")
    private String activeArea;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("创建时间")
    private Date creationTime;

    @ApiModelProperty("克隆状态")
    private String phrase;

    @ApiModelProperty("失败信息")
    private String reason;

}
