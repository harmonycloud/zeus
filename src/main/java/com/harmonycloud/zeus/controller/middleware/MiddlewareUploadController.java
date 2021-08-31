package com.harmonycloud.zeus.controller.middleware;

import java.io.File;

import com.harmonycloud.zeus.service.middleware.MiddlewareUploadService;
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
@Api(tags = {"工作台","服务目录"}, value = "中间件上架", description = "中间件上架")
@RestController
@RequestMapping("/clusters/{clusterId}/middlewares/upload")
public class MiddlewareUploadController {

    @Autowired
    private MiddlewareUploadService middlewareUploadService;

    @PostMapping
    @ResponseBody
    @ApiOperation(value = "中间件上架", notes = "上传中间件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "file", value = "文件", paramType = "form",dataTypeClass = File.class)
    })
    public BaseResult upload(@PathVariable String clusterId, @RequestParam("file") MultipartFile file) {
        middlewareUploadService.upload(clusterId, file);
        return BaseResult.ok("上传成功");
    }
}
