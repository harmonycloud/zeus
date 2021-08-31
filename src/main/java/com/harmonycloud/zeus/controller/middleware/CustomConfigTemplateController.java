package com.harmonycloud.zeus.controller.middleware;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.CustomConfigTemplateDTO;
import com.harmonycloud.zeus.service.middleware.ConfigTemplateService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @Date 2021/4/25 10:28 上午
 */
@Api(tags = "CustomConfigTemplate", value = "自定义配置模板", description = "自定义配置模板")
@RestController
@RequestMapping("/middlewares/template")
public class CustomConfigTemplateController {

    @Autowired
    private ConfigTemplateService configTemplateService;

    @ApiOperation(value = "获取自定义配置模板", notes = "获取自定义配置模板")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult<List<CustomConfigTemplateDTO>> list(@RequestParam("type") String type) {
        return BaseResult.ok(configTemplateService.list(type));
    }

    @ApiOperation(value = "获取自定义配置模板", notes = "获取自定义配置模板")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "templateName", value = "模板名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{templateName}")
    public BaseResult<List<CustomConfigTemplateDTO>> get(@RequestParam("type") String type,
                                                         @PathVariable("templateName") String templateName) {
        return BaseResult.ok(configTemplateService.get(type, templateName));
    }

}
