package com.middleware.zeus.common.model.middleware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/6/2 9:18 上午
 */
@Data
@Accessors(chain = true)
@ApiModel("自定义参数yaml文件")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value=JsonInclude.Include.NON_NULL)
public class CustomConfigParameters {
    private List<Map<String, List<CustomConfigParameter>>> parameters;
}
