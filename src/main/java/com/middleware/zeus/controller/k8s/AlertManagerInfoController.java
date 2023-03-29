package com.middleware.zeus.controller.k8s;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.ClusterComponentsDto;
import com.middleware.zeus.service.k8s.AlertManagerInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/29 11:16 上午
 */
@Api(tags = {"系统管理","基础资源"}, value = "集群组件", description = "集群组件")
@RestController
@RequestMapping("/alertmanager")
public class AlertManagerInfoController {

    @Autowired
    private AlertManagerInfoService alertManagerInfoService;

    @ApiOperation(value = "获取组件列表", notes = "获取组件列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", required = false, value = "集群id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<List<ClusterComponentsDto>> list(@RequestParam(value = "clusterId", required = false) String clusterId) throws Exception {
        return BaseResult.ok(alertManagerInfoService.list(clusterId));
    }


}
