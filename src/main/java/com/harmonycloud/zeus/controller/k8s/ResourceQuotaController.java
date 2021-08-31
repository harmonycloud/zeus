package com.harmonycloud.zeus.controller.k8s;

import com.harmonycloud.zeus.service.k8s.ResourceQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harmonycloud.caas.common.base.BaseResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @Date 2021/4/12 10:00 上午
 */
@Api(tags = "resourceQuota", value = "命名空间配额", description = "命名空间配额")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces")
public class ResourceQuotaController {

    @Autowired
    private ResourceQuotaService resourceQuotaService;

    @ApiOperation(value = "查询分区配额", notes = "查询分区配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{namespace}/quota")
    public BaseResult list(@PathVariable("clusterId") String clusterId,
                           @PathVariable("namespace") String namespace) {
        return BaseResult.ok(resourceQuotaService.list(clusterId, namespace));
    }

}
