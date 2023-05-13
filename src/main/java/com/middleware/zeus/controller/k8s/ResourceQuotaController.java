package com.middleware.zeus.controller.k8s;

import com.middleware.zeus.common.model.ResourceQuotaDo;
import com.middleware.zeus.service.k8s.ResourceQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.middleware.zeus.common.base.BaseResult;

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
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/quota")
public class ResourceQuotaController {

    @Autowired
    private ResourceQuotaService resourceQuotaService;

    @ApiOperation(value = "查询分区配额", notes = "查询分区配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageClass", value = "存储类型", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<ResourceQuotaDo> list(@PathVariable("clusterId") String clusterId,
                                            @PathVariable("namespace") String namespace,
                                            @RequestParam(value = "storageClass", required = false) String storageClass) {
        return BaseResult.ok(resourceQuotaService.list(clusterId, namespace, storageClass));
    }

    @ApiOperation(value = "创建/修改分区配额", notes = "创建/修改分区配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "quotaDo", value = "资源配额", paramType = "query", dataTypeClass = ResourceQuotaDo.class),
    })
    @PutMapping
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestBody ResourceQuotaDo quotaDo) {
        //resourceQuotaService.update(clusterId, namespace, quotaDo);
        return BaseResult.ok();
    }

}
