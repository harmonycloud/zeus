package com.harmonycloud.zeus.controller.k8s;

import com.harmonycloud.zeus.service.k8s.StorageClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.caas.common.base.BaseResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@Api(tags = "存储服务", value = "存储服务", description = "存储服务")
@RestController
@RequestMapping("/clusters/{clusterId}/storageclasses")
public class StorageClassController {

    @Autowired
    private StorageClassService storageClassService;

    @ApiOperation(value = "查询存储列表", notes = "查询存储列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "onlyMiddleware", value = "是否只返回中间件相关的存储", paramType = "query", defaultValue = "true", dataTypeClass = Boolean.class),
    })
    @GetMapping
    public BaseResult list(@PathVariable("clusterId") String clusterId,
                           @RequestParam(value = "onlyMiddleware", defaultValue = "true") boolean onlyMiddleware,
                           @RequestParam(value = "namespace", required = false) String namespace) {
        return BaseResult.ok(storageClassService.list(clusterId, namespace, onlyMiddleware));
    }

}
