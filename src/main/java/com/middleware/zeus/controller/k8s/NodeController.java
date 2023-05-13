package com.middleware.zeus.controller.k8s;

import com.middleware.zeus.common.model.Node;
import com.middleware.zeus.service.k8s.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.middleware.zeus.common.base.BaseResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@Api(tags = "node", value = "主机/节点", description = "主机/节点")
@RestController
@RequestMapping("/clusters/{clusterId}/nodes")
public class NodeController {

    @Autowired
    private NodeService nodeService;

    @ApiOperation(value = "查询节点列表", notes = "查询节点列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult<List<Node>> list(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(nodeService.list(clusterId));
    }

    @ApiOperation(value = "查询可用区节点列表", notes = "查询可用区节点列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "zone", value = "可用区", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/zone")
    public BaseResult<List<Node>> listActive(@PathVariable("clusterId") String clusterId,
                                             @RequestParam(value = "zone") String zone) {
        return BaseResult.ok(nodeService.listActive(clusterId, zone));
    }

    @ApiOperation(value = "获取可分配可用区的节点", notes = "获取可分配可用区的节点")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/allocatable")
    public BaseResult<List<Node>> listAllocatableNode(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(nodeService.listAllocatableNode(clusterId));
    }

    @ApiOperation(value = "查询节点污点", notes = "查询节点污点")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/taints")
    public BaseResult taints(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(nodeService.listTaints(clusterId));
    }

}
