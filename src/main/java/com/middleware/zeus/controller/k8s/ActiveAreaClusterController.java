package com.middleware.zeus.controller.k8s;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.ActiveAreaClusterDto;
import com.middleware.zeus.service.k8s.ActiveAreaClusterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/8/13 1:42 下午
 */
@Api(tags = {"集群管理","可用区"}, value = "集群可用区", description = "集群可用区")
@RestController
@RequestMapping("/area/clusters")
public class ActiveAreaClusterController {

    @Autowired
    private ActiveAreaClusterService activeAreaClusterService;

    @ApiOperation(value = "查询集群列表", notes = "查询集群列表")
    @ApiImplicitParams({
    })
    @GetMapping
    public BaseResult<List<ActiveAreaClusterDto>> list() {
        return BaseResult.ok(activeAreaClusterService.list());
    }

    @ApiOperation(value = "开启可用区", notes = "开启可用区")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "Path", dataTypeClass = String.class)
    })
    @GetMapping("/{clusterId}")
    public BaseResult update(@PathVariable("clusterId") String clusterId) {
        activeAreaClusterService.update(clusterId, true);
        return BaseResult.ok();
    }

}
