package com.middleware.zeus.common.model.k8s;

import com.middleware.zeus.common.model.middleware.PortDetailDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/7/3 3:37 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("service业务对象")
public class ServiceDo {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("service名称")
    private String name;

    @ApiModelProperty("服务名")
    private String serviceName;

    @ApiModelProperty("cluster ip")
    private String clusterIp;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("service类型")
    private String type;

    @ApiModelProperty("标签")
    private Map<String, String> labels;

    @ApiModelProperty("注解")
    private Map<String, String> annotations;

    @ApiModelProperty("注解")
    private Map<String, String> selector;

    @ApiModelProperty("服务")
    private List<PortDetailDTO> portDetailDtoList;

    @ApiModelProperty("绑定上级对象")
    private List<OwnerReferencesDo> ownerReferencesDoList;
}
