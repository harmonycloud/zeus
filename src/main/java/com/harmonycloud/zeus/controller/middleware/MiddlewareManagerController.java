package com.harmonycloud.zeus.controller.middleware;

import java.io.File;
import java.util.List;

import com.harmonycloud.caas.common.model.middleware.MiddlewareStatusDto;
import com.harmonycloud.zeus.service.middleware.MiddlewareManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.harmonycloud.caas.common.base.BaseResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @Author: zack chen
 * @Date: 2021/5/14 11:16 上午
 */
@Api(tags = {"中间件市场"}, value = "中间件管理", description = "中间件管理")
@RestController
@RequestMapping("/clusters/{clusterId}/middlewares")
public class MiddlewareManagerController {

    @Autowired
    private MiddlewareManagerService middlewareManagerService;

    @PostMapping("/upload")
    @ResponseBody
    @ApiOperation(value = "中间件上架", notes = "上传中间件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "file", value = "文件", paramType = "form",dataTypeClass = File.class)
    })
    public BaseResult upload(@PathVariable String clusterId, @RequestParam("file") MultipartFile file) {
        middlewareManagerService.upload(clusterId, file);
        return BaseResult.ok("上传成功");
    }

    @ApiOperation(value = "中间件operator发布", notes = "中间件operator发布")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartName", value = "chart名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartVersion", value = "chart版本", paramType = "query", dataTypeClass = String.class)
    })
    @PostMapping("/install")
    public BaseResult install(@PathVariable("clusterId") String clusterId,
                              @RequestParam("chartName") String chartName,
                              @RequestParam("chartVersion") String chartVersion) throws Exception {
        middlewareManagerService.install(clusterId, chartName, chartVersion);
        return BaseResult.ok();
    }

    @ApiOperation(value = "中间件卸载", notes = "中间件卸载")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartName", value = "chart名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartVersion", value = "chart版本", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping("/delete")
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                                 @RequestParam("chartName") String chartName,
                                 @RequestParam("chartVersion") String chartVersion) {
        middlewareManagerService.delete(clusterId, chartName, chartVersion);
        return BaseResult.ok();
    }

    @ApiOperation(value = "中间件更新升级", notes = "中间件更新升级")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartName", value = "chart名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartVersion", value = "chart版本", paramType = "query", dataTypeClass = String.class)
    })
    @PutMapping("/update")
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @RequestParam("chartName") String chartName,
                             @RequestParam("chartVersion") String chartVersion){
        middlewareManagerService.update(clusterId, chartName, chartVersion);
        return BaseResult.ok();
    }
}
