package com.harmonycloud.zeus.controller.k8s;

import java.io.InputStream;
import java.util.List;

import com.harmonycloud.caas.common.model.ClusterNamespaceResourceDto;
import com.harmonycloud.caas.common.model.ClusterNodeResourceDto;
import com.harmonycloud.caas.common.model.Node;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareResourceInfo;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

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
            @ApiImplicitParam(name = "detail", value = "是否返回集群明细信息", paramType = "query", dataTypeClass = Boolean.class)
    })
    @GetMapping
    public BaseResult<List<MiddlewareClusterDTO>> list(@RequestParam(value = "detail", defaultValue = "false") boolean detail,
                                                       @RequestParam(value = "key", required = false) String key,
                                                       @RequestParam(value = "projectId", required = false) String projectId) {
        List<MiddlewareClusterDTO> list = clusterService.listClusters(detail, key, projectId);
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
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{clusterId}/middleware/resource")
    public BaseResult<List<MiddlewareResourceInfo>> getMwResource(@PathVariable(value = "clusterId") String clusterId) throws Exception {
        List<MiddlewareResourceInfo> mwResourceInfoList = clusterService.getMwResource(clusterId);
        return BaseResult.ok(mwResourceInfoList);
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
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{clusterId}/namespace/resource")
    public BaseResult<List<ClusterNamespaceResourceDto>> getNamespaceResource(@PathVariable(value = "clusterId") String clusterId) throws Exception {
        return BaseResult.ok(clusterService.getNamespaceResource(clusterId));
    }

    @ApiOperation(value = "获取集群纳管指令", notes = "获取集群纳管指令")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "集群名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "apiAddress", value = "接口访问前缀", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/clusterJoinCommand")
    public BaseResult clusterJoinCommand(@RequestParam(value = "name") String name, @RequestParam(value = "apiAddress") String apiAddress, HttpServletRequest request) {
        String userToken = request.getHeader("userToken");
        return BaseResult.ok(clusterService.getClusterJoinCommand(name, apiAddress, userToken));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "adminConf", value = "集群名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "集群名称", paramType = "query", dataTypeClass = String.class)
    })
    @PostMapping("/quickAdd")
    public BaseResult quickAdd(@RequestParam("adminConf") MultipartFile adminConf,@RequestParam("name") String name) {
        log.info("name{}", name);
        return clusterService.quickAdd(adminConf, name);
    }

    /**
     ffx     * 数据脱敏
     */
    private void desensitize(MiddlewareClusterDTO cluster) {
        cluster.setAccessToken(null);
        if (cluster.getRegistry() != null) {
            cluster.getRegistry().setPassword(null);
        }
        cluster.setCert(null);
    }

}
