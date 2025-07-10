package com.middleware.zeus.controller.k8s;

import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.middleware.IngressDTO;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.service.k8s.IngressService;
import com.middleware.zeus.service.k8s.NodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.middleware.zeus.common.constants.CommonConstant.ASTERISK;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = {"服务列表", "服务暴露"}, value = "中间件对外访问")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares")
public class IngressController {

    @Autowired
    private IngressService ingressService;

    @ApiOperation(value = "查询中间件对外访问列表", notes = "查询中间件对外访问列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "模糊搜索", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/ingress")
    @Authority(power = 1)
    public BaseResult<List<IngressDTO>> list(@PathVariable("clusterId") String clusterId,
                                             @PathVariable(value = "namespace") String namespace,
                                             @RequestParam(value = "keyword", required = false) String keyword,
                                             @RequestParam(value = "organId", required = false) String organId,
                                             @RequestParam(value = "projectId", required = false) String projectId) {
        if (namespace.equals(ASTERISK)){
            namespace = null;
        }
        return BaseResult.ok(ingressService.listAllMiddlewareIngress(clusterId, namespace, keyword, organId, projectId));
    }

    @ApiOperation(value = "创建中间件对外访问", notes = "创建中间件对外访问")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingress", value = "对外访问信息", paramType = "query", dataTypeClass = IngressDTO.class)
    })
    @PostMapping("/{middlewareName}/ingress")
    @Authority(power = 1)
    public BaseResult create(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String middlewareName,
                             @RequestBody IngressDTO ingress) {
        ingressService.create(clusterId, namespace, middlewareName, ingress);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除中间件对外访问", notes = "删除中间件对外访问")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingressName", value = "对外访问名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "ingress", value = "对外访问信息", paramType = "query", dataTypeClass = IngressDTO.class)
    })
    @DeleteMapping("/{middlewareName}/ingress/{ingressName}")
    @Authority(power = 1)
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable(value = "namespace") String namespace,
                             @PathVariable("middlewareName") String middlewareName,
                             @PathVariable("ingressName") String ingressName,
                             @RequestBody IngressDTO ingress) {
        ingressService.delete(clusterId, namespace, middlewareName, ingressName, ingress);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取中间件对外访问", notes = "获取中间件对外访问")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/{middlewareName}/ingress")
    @Authority(power = 1)
    public BaseResult<List<IngressDTO>> get(@PathVariable("clusterId") String clusterId,
                                      @PathVariable("namespace") String namespace,
                                      @PathVariable("middlewareName") String middlewareName,
                                      @RequestParam("type") String type) {
        return BaseResult.ok(ingressService.getMiddlewareIngress(clusterId, namespace, type, middlewareName));
    }

    @ApiOperation(value = "获取中间件HostNetwork对外访问", notes = "获取中间件HostNetwork对外访问")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/{middlewareName}/hostNetworkAddress")
    @Authority(power = 1)
    public BaseResult<List<IngressDTO>> getHostNetworkAddress(@PathVariable("clusterId") String clusterId,
                                            @PathVariable("namespace") String namespace,
                                            @PathVariable("middlewareName") String middlewareName,
                                            @RequestParam("type") String type) {
        return BaseResult.ok(ingressService.getHostNetworkAddress(clusterId, namespace, type, middlewareName));
    }

    @ApiOperation(value = "port校验", notes = "port校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startPort", value = "起始端口", paramType = "query", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "endPort", value = "末尾端口", paramType = "query", dataTypeClass = Integer.class)
    })
    @GetMapping("/{middlewareName}/portCheck")
    @Authority(power = 1)
    public BaseResult<String> portCheck(@PathVariable("clusterId") String clusterId,
                                        @PathVariable("namespace") String namespace,
                                        @PathVariable("middlewareName") String middlewareName,
                                        @RequestParam("startPort") Integer startPort,
                                        @RequestParam("endPort") Integer endPort) {
        return BaseResult.ok(ingressService.portCheck(clusterId, namespace, middlewareName, startPort, endPort));
    }

}
