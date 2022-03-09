package com.harmonycloud.zeus.controller.middleware;

import java.util.List;

import com.harmonycloud.caas.common.model.middleware.CustomConfig;
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
@RequestMapping("/middlewares/{type}/template")
public class CustomConfigTemplateController {

    @Autowired
    private ConfigTemplateService configTemplateService;

    @ApiOperation(value = "创建自定义配置模板", notes = "创建自定义配置模板")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "customConfigTemplateDTO", value = "模板对象", paramType = "query", dataTypeClass = String.class)
    })
    @PostMapping
    public BaseResult create(@PathVariable("type") String type,
                             @RequestBody CustomConfigTemplateDTO customConfigTemplateDTO) {
        customConfigTemplateDTO.setType(type);
        configTemplateService.create(customConfigTemplateDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取自定义配置模板列表", notes = "获取自定义配置模板列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult<List<CustomConfigTemplateDTO>> list(@PathVariable("type") String type) {
        return BaseResult.ok(configTemplateService.list(type));
    }

    @ApiOperation(value = "获取自定义配置模板内容", notes = "获取自定义配置模板内容")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "templateName", value = "模板名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{uid}")
    public BaseResult<CustomConfigTemplateDTO> get(@PathVariable("type") String type,
                                                   @PathVariable("uid") String uid,
                                                   @RequestParam("chartVersion") String chartVersion) {
        return BaseResult.ok(configTemplateService.get(type, uid, chartVersion));
    }

    @ApiOperation(value = "获取初始化模板", notes = "获取初始化模板")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartVersion", value = "中间件版本", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/init")
    public BaseResult<List<CustomConfig>> get(@PathVariable("type") String type,
                                              @RequestParam("chartVersion") String chartVersion) {
        return BaseResult.ok(configTemplateService.get(type, chartVersion));
    }

    @ApiOperation(value = "更新自定义配置模板", notes = "更新自定义配置模板")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "uid", value = "模板id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "customConfigTemplateDTO", value = "模板对象", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/{uid}")
    public BaseResult update(@PathVariable("type") String type,
                             @PathVariable("uid") String uid,
                             @RequestBody CustomConfigTemplateDTO customConfigTemplateDTO) {
        customConfigTemplateDTO.setUid(uid).setType(type);
        configTemplateService.update(customConfigTemplateDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除自定义配置模板", notes = "删除自定义配置模板")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "uids", value = "模板id", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping
    public BaseResult delete(@PathVariable("type") String type,
                             @RequestParam("uids") String uids) {
        configTemplateService.delete(type, uids);
        return BaseResult.ok();
    }

}
