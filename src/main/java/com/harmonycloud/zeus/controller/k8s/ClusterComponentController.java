package com.harmonycloud.zeus.controller.k8s;

import static com.harmonycloud.caas.common.constants.NameConstant.DEFAULT;

import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.MultipleComponentsInstallDto;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareInfoDTO;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

import java.util.List;

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
    @Autowired
    private ClusterService clusterService;

    @ApiOperation(value = "获取组件列表", notes = "获取组件列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
           })
    @GetMapping
    public BaseResult<List<ClusterComponentsDto>> list(@PathVariable("clusterId") String clusterId) throws Exception {
        return BaseResult.ok(clusterComponentService.list(clusterId));
    }

    @ApiOperation(value = "部署集群组件", notes = "部署集群组件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "componentName", value = "集群组件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterComponentsDto", value = "集群组件对象", paramType = "query", dataTypeClass = ClusterComponentsDto.class),
    })
    @PostMapping("/{componentName}")
    public BaseResult deploy(@PathVariable("clusterId") String clusterId,
                             @PathVariable("componentName") String componentName,
                             @RequestBody ClusterComponentsDto clusterComponentsDto) {
        clusterComponentsDto.setClusterId(clusterId);
        clusterComponentsDto.setComponent(componentName);
        clusterComponentService.deploy(clusterService.findById(clusterId), clusterComponentsDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除集群组件", notes = "删除集群组件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "componentName", value = "集群组件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "status", value = "状态", paramType = "query", dataTypeClass = Integer.class),
    })
    @DeleteMapping("/{componentName}")
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("componentName") String componentName,
                             @RequestParam("status") Integer status) {
        clusterComponentService.delete(clusterService.findById(clusterId), componentName, status);
        return BaseResult.ok();
    }

    @ApiOperation(value = "批量部署集群组件", notes = "批量部署集群组件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "multipleComponentsInstallDto", value = "组件对象list", paramType = "query", dataTypeClass = MultipleComponentsInstallDto.class)
    })
    @PostMapping("/multiple")
    public BaseResult multipleDeploy(@PathVariable("clusterId") String clusterId,
                                     @RequestBody MultipleComponentsInstallDto multipleComponentsInstallDto) throws Exception {
        clusterComponentService.multipleDeploy(clusterService.findById(clusterId), multipleComponentsInstallDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新集群组件信息", notes = "更新集群组件信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "componentName", value = "集群组件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterComponentsDto", value = "集群组件信息", paramType = "query", dataTypeClass = ClusterComponentsDto.class)
    })
    @PutMapping("/{componentName}")
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("componentName") String componentName,
                             @RequestBody ClusterComponentsDto clusterComponentsDto) {
        clusterComponentsDto.setClusterId(clusterId);
        clusterComponentsDto.setComponent(componentName);
        clusterComponentService.integrate(clusterComponentsDto, true);
        return BaseResult.ok();
    }

    @ApiOperation(value = "对接集群组件", notes = "对接集群组件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "componentName", value = "集群组件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterComponentsDto", value = "集群信息", paramType = "集群组件信息", dataTypeClass = ClusterComponentsDto.class)
    })
    @PutMapping("/{componentName}/integrate")
    public BaseResult integrate(@PathVariable("clusterId") String clusterId,
                                @PathVariable("componentName") String componentName,
                                @RequestBody ClusterComponentsDto clusterComponentsDto) {
        clusterComponentsDto.setClusterId(clusterId);
        clusterComponentsDto.setComponent(componentName);
        clusterComponentService.integrate(clusterComponentsDto, false);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询日志采集组件的安装情况", notes = "查询日志采集组件的安装情况")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("logging/logCollect")
    public BaseResult<Boolean> logCollect(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(clusterComponentService.logCollect(clusterId));
    }

}
