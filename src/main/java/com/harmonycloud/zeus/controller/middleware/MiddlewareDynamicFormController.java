package com.harmonycloud.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.middleware.QuestionYaml;
import com.harmonycloud.zeus.annotation.Authority;
import com.harmonycloud.zeus.service.middleware.MiddlewareDynamicFormService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author xutianhong
 * @Date 2021/6/8 3:27 下午
 */

@Api(tags = "middleware dynamic form", value = "中间件动态表单", description = "中间件动态表单")
@RestController
@RequestMapping("/clusters/{clusterId}/dynamic")
public class MiddlewareDynamicFormController {

    @Autowired
    private MiddlewareDynamicFormService dynamicFormService;

    @ApiOperation(value = "获取动态表单", notes = "获取动态表单")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartName", value = "chart包名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartVersion", value = "chart包版本", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    @Authority(power = 1)
    public BaseResult<QuestionYaml> dynamicForm(@PathVariable("clusterId") String clusterId,
                                                @RequestParam("chartName") String chartName,
                                                @RequestParam("chartVersion") String chartVersion) {
        return BaseResult.ok(dynamicFormService.dynamicForm(clusterId, chartName, chartVersion));
    }

}
