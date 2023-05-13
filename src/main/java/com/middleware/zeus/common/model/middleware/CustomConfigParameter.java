package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/6/2 9:20 上午
 */
@Data
@Accessors(chain = true)
@ApiModel("自定义参数yaml文件")
public class CustomConfigParameter {

    private String name;

    private String value;

}
