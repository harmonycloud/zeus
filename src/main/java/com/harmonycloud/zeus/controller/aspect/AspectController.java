package com.harmonycloud.zeus.controller.aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.middleware.QuestionYaml;
import com.harmonycloud.zeus.service.aspect.AspectService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @Date 2021/11/12 11:36 上午
 */
@Api(tags = "aspect", value = "外接表单", description = "外接表单")
@RestController
@RequestMapping("/aspect/form")
public class AspectController {

    @Autowired
    private AspectService aspectService;

    @ApiOperation(value = "获取外接动态表单", notes = "获取外接动态表单")
    @GetMapping
    public BaseResult<QuestionYaml> get() {
        return BaseResult.ok(aspectService.dynamic());
    }

}
