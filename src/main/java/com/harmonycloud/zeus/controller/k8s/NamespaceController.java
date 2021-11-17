package com.harmonycloud.zeus.controller.k8s;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.filters.enumm.LanguageEnum;
import com.harmonycloud.zeus.service.k8s.NamespaceService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Api(tags = {"系统管理","基础资源"}, value = "命名空间", description = "命名空间")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces")
public class NamespaceController {

    @Autowired
    private NamespaceService namespaceService;
    
    @ApiOperation(value = "查询命名空间列表", notes = "查询命名空间列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "all", value = "是否查询所有命名空间，默认false", paramType = "query", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "withQuota", value = "是否返回命名空间配额，默认false", paramType = "query", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "withMiddleware", value = "是否返回中间件实例信息，默认false", paramType = "query", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "keyword", value = "模糊搜索关键词", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult<List<Namespace>> list(@PathVariable("clusterId") String clusterId,
                                            @RequestParam(value = "all", defaultValue = "false") boolean all,
                                            @RequestParam(value = "withQuota", defaultValue = "false") boolean withQuota,
                                            @RequestParam(value = "withMiddleware", defaultValue = "false") boolean withMiddleware,
                                            @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(namespaceService.list(clusterId, all, withQuota, withMiddleware, keyword));
    }


    @ApiOperation(value = "创建命名空间", notes = "创建命名空间")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "分区名称", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping
    public BaseResult create(@PathVariable("clusterId") String clusterId,
                             @RequestParam("name") String name){
        namespaceService.save(clusterId, name, null, true);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除命名空间", notes = "删除命名空间")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "分区名称", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{name}")
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("name") String name){
        namespaceService.delete(clusterId, name);
        return BaseResult.ok();
    }

    @ApiOperation(value = "注册命名空间", notes = "注册命名空间")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/{name}")
    public BaseResult registry(@PathVariable("clusterId") String clusterId,
                               @PathVariable("name") String name,
                               @RequestParam Boolean registered){
        namespaceService.registry(clusterId, name, registered);
        return BaseResult.ok();
    }
    
}
