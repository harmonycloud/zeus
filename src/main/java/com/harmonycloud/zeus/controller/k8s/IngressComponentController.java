package com.harmonycloud.zeus.controller.k8s;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.middleware.IngressDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.service.k8s.IngressComponentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/11/22 5:39 下午
 */
@Api(tags = {"系统管理","基础资源"}, value = "ingress-controller", description = "ingress-controller")
@RestController
@RequestMapping("/clusters/{clusterId}/ingress")
public class IngressComponentController {
    
    @Autowired
    private IngressComponentService ingressComponentService;

    @ApiOperation(value = "添加集群ingress", notes = "添加集群ingress")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ingressComponentDto", value = "集群ingress组件信息", paramType = "query", dataTypeClass = IngressComponentDto.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
    })
    @PostMapping
    public BaseResult install(@RequestBody IngressComponentDto ingressComponentDto,
                              @PathVariable("clusterId") String clusterId) {
        ingressComponentDto.setClusterId(clusterId);
        ingressComponentService.install(ingressComponentDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "对接集群ingress组件", notes = "对接集群ingress组件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "cluster", value = "集群信息", paramType = "query", dataTypeClass = MiddlewareClusterDTO.class)
    })
    @PutMapping
    public BaseResult integrate(@PathVariable("clusterId") String clusterId,
                                @RequestBody MiddlewareClusterDTO cluster) {
        cluster.setId(clusterId);
        ingressComponentService.integrate(cluster);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取ingress组件列表", notes = "获取ingress组件列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult<List<IngressComponentDto>> list(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(ingressComponentService.list(clusterId));
    }

    @ApiOperation(value = "删除指定ingress组件", notes = "删除指定ingress组件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressName", value = "ingress名称", paramType = "path", dataTypeClass = String.class)
    })
    @DeleteMapping("/{ingressName}")
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("ingressName") String ingressName) {
        ingressComponentService.delete(clusterId, ingressName);
        return BaseResult.ok();
    }

}
