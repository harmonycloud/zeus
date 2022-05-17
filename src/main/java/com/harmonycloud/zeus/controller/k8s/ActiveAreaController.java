package com.harmonycloud.zeus.controller.k8s;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.ActiveAreaDto;
import com.harmonycloud.caas.common.model.ActivePoolDto;
import com.harmonycloud.caas.common.model.ClusterNodeResourceDto;
import com.harmonycloud.zeus.service.k8s.ActiveAreaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/5/10 2:17 下午
 */
@Api(tags = {"集群管理","可用区"}, value = "集群可用区", description = "集群可用区")
@RestController
@RequestMapping("/clusters/{clusterId}/area")
public class ActiveAreaController {

    @Autowired
    private ActiveAreaService activeAreaService;

    @ApiOperation(value = "划分可用区", notes = "划分可用区")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "activeAreaDto", value = "集群id", paramType = "path", dataTypeClass = ActiveAreaDto.class),
    })
    @PostMapping
    public BaseResult divideActiveArea(@PathVariable("clusterId") String clusterId,
                                       @RequestBody ActiveAreaDto activeAreaDto) {
        activeAreaService.divideActiveArea(clusterId, activeAreaDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "划分可用区", notes = "划分可用区")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "activePoolDto", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "nodeName", value = "节点名称", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{areaName}")
    public BaseResult removeActiveAreaNode(@PathVariable("clusterId") String clusterId,
                                           @PathVariable("areaName") String areaName,
                                           @RequestParam("nodeName") String nodeName) {
        activeAreaService.removeActiveAreaNode(clusterId, areaName, nodeName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取可用区列表", notes = "获取可用区列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<List<ActiveAreaDto>> list(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(activeAreaService.list(clusterId));
    }

    @ApiOperation(value = "更新可用区", notes = "更新可用区")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "areaName", value = "可用区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "aliasName", value = "可用区别名", paramType = "path", dataTypeClass = String.class),
    })
    @PutMapping("/{areaName}")
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("areaName") String areaName,
                             @RequestParam("aliasName") String aliasName) {
        activeAreaService.update(clusterId, areaName, aliasName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取可用区资源情况", notes = "获取可用区资源情况")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "areaName", value = "可用区名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{areaName}")
    public BaseResult<ActiveAreaDto> getAreaResource(@PathVariable("clusterId") String clusterId,
                                                     @PathVariable("areaName") String areaName) {
        return BaseResult.ok(activeAreaService.getAreaResource(clusterId, areaName));
    }

    @ApiOperation(value = "获取可用区节点列表", notes = "获取可用区节点列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "areaName", value = "可用区名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{areaName}/node")
    public BaseResult<List<ClusterNodeResourceDto>> getAreaNode(@PathVariable("clusterId") String clusterId,
                                                                @PathVariable("areaName") String areaName) {
        return BaseResult.ok(activeAreaService.getAreaNode(clusterId, areaName));
    }
}
