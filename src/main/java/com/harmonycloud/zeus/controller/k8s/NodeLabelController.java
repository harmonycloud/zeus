package com.harmonycloud.zeus.controller.k8s;

import java.util.List;

import com.harmonycloud.zeus.service.k8s.NodeLabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.Node;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@Api(tags = "nodeLabel", value = "主机标签", description = "主机标签")
@RestController
@RequestMapping("/clusters/{clusterId}/nodes")
public class NodeLabelController {

    @Autowired
    private NodeLabelService nodeLabelService;

    @ApiOperation(value = "查询节点列表", notes = "查询节点列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/labels")
    public BaseResult<List<Node>> restart(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(nodeLabelService.list(clusterId));
    }
}
