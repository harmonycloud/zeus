package com.middleware.zeus.controller.k8s;

import com.middleware.zeus.service.k8s.PvcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.caas.common.base.BaseResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @since 2021/6/23 10:15 上午
 */
@Api(tags = "pvc", value = "资源服务", description = "资源服务")
@RestController
@RequestMapping("/clusters/{clusterId}//namespaces/{namespace}/pvc")
public class PVCController {

    @Autowired
    private PvcService pvcService;

    @ApiOperation(value = "查询pvc列表", notes = "查询pvc列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult list(@PathVariable("clusterId") String clusterId,
                           @PathVariable("namespace") String namespace) {
        return BaseResult.ok(pvcService.list(clusterId, namespace));
    }
}
