package com.middleware.zeus.controller.k8s;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.IngressComponentDto;
import com.middleware.caas.common.model.middleware.IngressDTO;
import com.middleware.caas.common.model.middleware.MiddlewareValues;
import com.middleware.caas.common.model.middleware.PodInfo;
import com.middleware.zeus.service.k8s.IngressComponentService;
import com.middleware.zeus.service.k8s.NodeService;
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
    @Autowired
    private NodeService nodeService;

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
            @ApiImplicitParam(name = "ingressComponentDto", value = "ingress信息", paramType = "query", dataTypeClass = IngressComponentDto.class)
    })
    @PutMapping
    public BaseResult integrate(@PathVariable("clusterId") String clusterId,
                                @RequestBody IngressComponentDto ingressComponentDto) {
        ingressComponentDto.setClusterId(clusterId);
        ingressComponentService.integrate(ingressComponentDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新集群ingress组件信息", notes = "更新集群ingress组件信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressComponentDto", value = "ingress信息", paramType = "query", dataTypeClass = IngressComponentDto.class),
            @ApiImplicitParam(name = "ingressClassName", value = "ingress名称", paramType = "path", dataTypeClass = IngressComponentDto.class),
    })
    @PutMapping("/{ingressClassName}")
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @RequestBody IngressComponentDto ingressComponentDto,
                             @PathVariable("ingressClassName") String ingressClassName) {
        ingressComponentDto.setIngressClassName(ingressClassName).setClusterId(clusterId);
        ingressComponentService.update(ingressComponentDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取ingress组件列表", notes = "获取ingress组件列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult<List<IngressComponentDto>> list(@PathVariable("clusterId") String clusterId, @RequestParam(value = "filterUnavailable", defaultValue = "false") boolean filterUnavailable) {
        return BaseResult.ok(ingressComponentService.list(clusterId, filterUnavailable));
    }

    @ApiOperation(value = "删除指定ingress组件", notes = "删除指定ingress组件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressClassName", value = "ingress名称", paramType = "path", dataTypeClass = String.class)
    })
    @DeleteMapping("/{ingressClassName}")
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("ingressClassName") String ingressClassName) {
        ingressComponentService.delete(clusterId, ingressClassName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询vip列表", notes = "查询vip列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/vip")
    public BaseResult<List<String>> vipList(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(ingressComponentService.vipList(clusterId));
    }

    @ApiOperation(value = "查询ingress详情", notes = "查询ingress详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressName", value = "ingress名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{ingressClassName}/detail")
    public BaseResult<IngressComponentDto> detail(@PathVariable("clusterId") String clusterId,
                                                  @PathVariable("ingressClassName") String ingressClassName) {
        return BaseResult.ok(ingressComponentService.detail(clusterId, ingressClassName));
    }

    @ApiOperation(value = "查询ingress pod列表", notes = "查询ingress pod列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressName", value = "ingress名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{ingressClassName}/pods")
    public BaseResult<List<PodInfo>> pods(@PathVariable("clusterId") String clusterId,
                                          @PathVariable("ingressClassName") String ingressClassName) {
        return BaseResult.ok(ingressComponentService.pods(clusterId, ingressClassName));
    }

    @ApiOperation(value = "查询ingress port使用列表", notes = "查询ingress port使用列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressName", value = "ingress名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{ingressClassName}/ports")
    public BaseResult<List<IngressDTO>> ports(@PathVariable("clusterId") String clusterId,
                            @PathVariable("ingressClassName") String ingressClassName){
        return BaseResult.ok(ingressComponentService.ports(clusterId, ingressClassName));
    }

    @ApiOperation(value = "重启pod", notes = "重启pod")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressClassName", value = "ingressClassName", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "podName", value = "pod名称", paramType = "path", dataTypeClass = String.class)
    })
    @DeleteMapping("/{ingressClassName}/pods/{podName}")
    public BaseResult restart(@PathVariable("clusterId") String clusterId,
                              @PathVariable("ingressClassName") String ingressClassName,
                              @PathVariable("podName") String podName) {
        ingressComponentService.restartPod(clusterId, ingressClassName, podName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查看pod yaml", notes = "查看pod yaml")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressClassName", value = "ingressClassName", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "podName", value = "pod名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{ingressClassName}/pods/{podName}/yaml")
    public BaseResult<String> podYaml(@PathVariable("clusterId") String clusterId,
                                      @PathVariable("ingressClassName") String ingressClassName,
                                      @PathVariable("podName") String podName) {
        return BaseResult.ok(ingressComponentService.podYaml(clusterId, ingressClassName, podName));
    }

    @ApiOperation(value = "查看ingress yaml", notes = "查看ingress yaml")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressClassName", value = "ingressClassName", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{ingressClassName}/values")
    public BaseResult<String> values(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("ingressClassName") String ingressClassName) {
        return BaseResult.ok(ingressComponentService.values(clusterId, ingressClassName));
    }

    @ApiOperation(value = "保存ingress yaml", notes = "保存ingress yaml")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressClassName", value = "ingressClassName", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareValues", value = "中间件values", paramType = "query", dataTypeClass = MiddlewareValues.class),
    })
    @PutMapping("/{ingressClassName}/values")
    public BaseResult upgrade(@PathVariable("clusterId") String clusterId,
                              @PathVariable("ingressClassName") String ingressClassName,
                              @RequestBody MiddlewareValues middlewareValues) {
        middlewareValues.setClusterId(clusterId);
        middlewareValues.setName(ingressClassName);
        ingressComponentService.upgrade(middlewareValues);
        return BaseResult.ok();
    }

    @ApiOperation(value = "端口校验", notes = "端口校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startPort", value = "startPort", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endPort", value = "endPort", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/check")
    public BaseResult<List<String>> portCheck(@PathVariable("clusterId") String clusterId,
                                              @RequestParam("startPort") Integer startPort,
                                              @RequestParam("endPort") Integer endPort) {
        return BaseResult.ok(ingressComponentService.portCheck(clusterId, startPort, endPort));
    }

}
