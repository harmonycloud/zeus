package com.middleware.zeus.common.model.user;

import com.middleware.zeus.common.model.BackupServerDTO;
import com.middleware.zeus.common.model.ResourceQuotaDo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/7 7:38 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("组织配额")
public class OrganizationQuota {

    @ApiModelProperty("组织id")
    private String organId;

    @ApiModelProperty("资源配额")
    private List<ResourceQuotaDo> quotaList;

    @ApiModelProperty("备份服务器列表")
    private List<BackupServerDTO> backupServerDTOList;
}
