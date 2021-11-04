package com.harmonycloud.zeus.controller.k8s;

import static com.harmonycloud.caas.common.constants.NameConstant.DEFAULT;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author dengyulong
 * @date 2021/05/18
 */
@Api(tags = {"系统管理","基础资源"}, value = "集群组件", description = "集群组件")
@RestController
@RequestMapping("/clusters/{clusterId}/components")
public class ClusterComponentController {

    @Autowired
    private ClusterComponentService clusterComponentService;

    @ApiOperation(value = "部署集群组件", notes = "部署集群组件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "componentName", value = "集群组件名称", paramType = "path", dataTypeClass = String.class)
    })
    @PostMapping("/{componentName}")
    public BaseResult deploy(@PathVariable("clusterId") String clusterId,
                             @PathVariable("componentName") String componentName) {
        clusterComponentService.deploy(clusterId, componentName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "对接集群组件", notes = "对接集群组件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "componentName", value = "集群组件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "cluster", value = "集群信息", paramType = "query", dataTypeClass = MiddlewareClusterDTO.class)
    })
    @PutMapping("/{componentName}")
    public BaseResult integrate(@PathVariable("clusterId") String clusterId,
                                @PathVariable("componentName") String componentName,
                                @RequestBody MiddlewareClusterDTO cluster) {
        cluster.setId(clusterId);
        clusterComponentService.integrate(cluster, componentName);
        return BaseResult.ok();
    }

}
