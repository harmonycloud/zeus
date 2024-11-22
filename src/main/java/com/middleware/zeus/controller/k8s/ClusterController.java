package com.middleware.zeus.controller.k8s;

import com.github.pagehelper.PageInfo;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.ClusterNamespaceResourceDto;
import com.middleware.zeus.common.model.ClusterNodeResourceDto;
import com.middleware.zeus.common.model.middleware.ClusterQuotaDTO;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.MiddlewareResourceInfo;
import com.middleware.zeus.common.model.middleware.Registry;
import com.middleware.zeus.service.k8s.ClusterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Slf4j
@Api(tags = {"系统管理","基础资源"}, value = "集群", description = "集群")
@RestController
@RequestMapping("/clusters")
public class ClusterController {

    @Autowired
    private ClusterService clusterService;
    
    @ApiOperation(value = "查询集群列表", notes = "查询集群列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "detail", value = "是否返回集群明细信息", paramType = "query", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "key", value = "关键词过滤", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<List<MiddlewareClusterDTO>> list(@RequestParam(value = "detail", defaultValue = "false") boolean detail,
                                                       @RequestParam(value = "key", required = false) String key,
                                                       @RequestParam(value = "organId", required = false) String organId,
                                                       @RequestParam(value = "projectId", required = false) String projectId) {
        List<MiddlewareClusterDTO> list = clusterService.listClusters(detail, key, organId, projectId);
        list.forEach(this::desensitize);
        return BaseResult.ok(list);
    }

    @ApiOperation(value = "查询集群详情", notes = "查询集群详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "visible", value = "是否返回敏感信息", paramType = "query", dataTypeClass = Boolean.class)
    })
    @GetMapping("/{clusterId}")
    public BaseResult<MiddlewareClusterDTO> get(@PathVariable(value = "clusterId") String clusterId,
                                                @RequestParam(value = "visible", required = false) boolean visible) {
        MiddlewareClusterDTO cluster = clusterService.detail(clusterId);
        if (!visible) {
            desensitize(cluster);
        }
        return BaseResult.ok(cluster);
    }

    @ApiOperation(value = "添加集群", notes = "添加集群")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "cluster", value = "集群信息", paramType = "query", dataTypeClass = MiddlewareClusterDTO.class)
    })
    @PostMapping
    public BaseResult add(@RequestBody MiddlewareClusterDTO cluster) {
        clusterService.addCluster(cluster);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改集群", notes = "修改集群")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "cluster", value = "集群信息", paramType = "query", dataTypeClass = MiddlewareClusterDTO.class)
    })
    @PutMapping("/{clusterId}")
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @RequestBody MiddlewareClusterDTO cluster) {
        cluster.setId(clusterId);
        clusterService.updateCluster(cluster);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除集群", notes = "删除集群")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @DeleteMapping("/{clusterId}")
    public BaseResult delete(@PathVariable("clusterId") String clusterId) {
        clusterService.removeCluster(clusterId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询集群下中间件资源详情", notes = "查询集群下中间件资源详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "target", value = "目标: cpu/memory/storage", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "根据中间件名称进行检索", required = false, paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "当前页", required = false, paramType = "query", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "size", value = "每页记录数", required = false, paramType = "query", dataTypeClass = Long.class),

    })
    @GetMapping("/{clusterId}/middleware/resource")
    public BaseResult<PageInfo<MiddlewareResourceInfo>> getMwResource(@PathVariable(value = "clusterId") String clusterId,
                                                                      @RequestParam("target") String target,
                                                                      @RequestParam(value = "keyword", required = false) String keyword,
                                                                      @RequestParam(value = "current", required = false, defaultValue = "1") Integer current,
                                                                      @RequestParam(value = "size", required = false, defaultValue = "5") Integer size) throws Exception {
        return BaseResult.ok(clusterService.getMwResource(clusterId, target, keyword, current, size));
    }

    @ApiOperation(value = "查询集群下node资源详情", notes = "查询集群下node资源详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{clusterId}/node/resource")
    public BaseResult<List<ClusterNodeResourceDto>> getNodeResource(@PathVariable(value = "clusterId") String clusterId) throws Exception {
        return BaseResult.ok(clusterService.getNodeResource(clusterId));
    }

    @ApiOperation(value = "查询集群下namespace资源详情", notes = "查询集群下namespace资源详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "target", value = "目标: cpu/memory/storage", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{clusterId}/namespace/resource")
    public BaseResult<List<ClusterNamespaceResourceDto>> getNamespaceResource(@PathVariable(value = "clusterId") String clusterId,
                                                                              @RequestParam("target") String target) throws Exception {
        return BaseResult.ok(clusterService.getNamespaceResource(clusterId, target));
    }

    @ApiOperation(value = "获取集群纳管指令", notes = "获取集群纳管指令")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "集群名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "apiAddress", value = "接口访问前缀", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "protocol", value = "协议", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "address", value = "地址", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "port", value = "端口", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "user", value = "用户", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", value = "密码", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/clusterJoinCommand")
    public BaseResult clusterJoinCommand(@RequestParam(value = "name") String name,
                                         @RequestParam(value = "apiAddress") String apiAddress,
                                         @RequestParam(value = "protocol", required = false) String protocol,
                                         @RequestParam(value = "address", required = false) String address,
                                         @RequestParam(value = "port", required = false) Integer port,
                                         @RequestParam(value = "user", required = false) String user,
                                         @RequestParam(value = "password", required = false) String password,
                                         HttpServletRequest request) {
        String userToken = request.getHeader("userToken");
        Registry registry = null;
        if (StringUtils.isNoneEmpty(protocol, address, user, password)){
            registry = new Registry();
            registry.setProtocol(protocol);
            registry.setAddress(address);
            registry.setPort(port);
            registry.setUser(user);
            registry.setPassword(password);
        }
        return BaseResult.ok(clusterService.getClusterJoinCommand(name, apiAddress, userToken, registry));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "adminConf", value = "集群名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "集群名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "protocol", value = "协议", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "address", value = "地址", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "port", value = "端口", paramType = "query", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "user", value = "用户", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", value = "密码", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("/quickAdd")
    public BaseResult quickAdd(@RequestParam("adminConf") MultipartFile adminConf,
                               @RequestParam("name") String name,
                               @RequestParam(value = "protocol", required = false) String protocol,
                               @RequestParam(value = "address", required = false) String address,
                               @RequestParam(value = "port", required = false) Integer port,
                               @RequestParam(value = "user", required = false) String user,
                               @RequestParam(value = "password", required = false) String password) {
        log.info("name{}", name);
        Registry registry = null;
        if (StringUtils.isNoneEmpty(protocol, address, user, password)){
            registry = new Registry();
            registry.setProtocol(protocol);
            registry.setAddress(address);
            registry.setPort(port);
            registry.setUser(user);
            registry.setPassword(password);
        }
        return clusterService.quickAdd(adminConf, name, registry);
    }

    @ApiOperation(value = "查询集群资源使用情况", notes = "查询集群资源使用情况")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{clusterId}/monitoring")
    public BaseResult<ClusterQuotaDTO> monitoring(@PathVariable(value = "clusterId") String clusterId) {
        return BaseResult.ok(clusterService.monitoring(clusterId));
    }

    @ApiOperation(value = "查询集群下资源配额情况", notes = "查询集群下资源配额情况")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "detail", value = "获取分配情况", paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping("/{clusterId}/quota")
    public BaseResult getResourceQuotaInfo(@PathVariable("clusterId") String clusterId,
                                           @RequestParam(value = "detail",defaultValue = "false") Boolean detail){
        return BaseResult.ok(clusterService.getResourceQuotaInfo(clusterId, detail));
    }

    /**
     ffx     * 数据脱敏
     */
    private void desensitize(MiddlewareClusterDTO cluster) {
        cluster.setAccessToken(null);
        cluster.setCert(null);
    }

    @GetMapping("{clusterId}/monitor")
    public BaseResult getClusterMonitors(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(clusterService.getClusterMonitors(clusterId));
    }

}
