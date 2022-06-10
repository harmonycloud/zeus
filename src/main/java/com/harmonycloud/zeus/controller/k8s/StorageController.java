package com.harmonycloud.zeus.controller.k8s;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.StorageClassDTO;
import com.harmonycloud.caas.common.model.StorageDto;
import com.harmonycloud.caas.common.model.middleware.CustomConfigTemplateDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareResourceInfo;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.annotation.Authority;
import com.harmonycloud.zeus.service.k8s.StorageService;
import com.harmonycloud.zeus.service.middleware.MiddlewareCrTypeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/6/6 4:23 下午
 */
@Api(tags = "storage", value = "存储服务", description = "存储服务")
@RestController
@RequestMapping("/clusters/{clusterId}/storage")
public class StorageController {

    @Autowired
    private StorageService storageService;
    @Autowired
    private MiddlewareCrTypeService middlewareCrTypeService;

    @ApiOperation(value = "查询存储列表", notes = "查询存储列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "key", value = "关键词搜索", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "存储类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "all", value = "是否全部", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<List<StorageDto>> list(@PathVariable("clusterId") String clusterId,
                                             @RequestParam("key") String key,
                                             @RequestParam("type") String type,
                                             @RequestParam("all") Boolean all) {
        return BaseResult.ok(storageService.list(clusterId, key, type, all));
    }

    @ApiOperation(value = "添加存储", notes = "添加存储")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageDto", value = "存储业务对象", paramType = "query", dataTypeClass = StorageDto.class)
    })
    @PostMapping
    public BaseResult add(@PathVariable("clusterId") String clusterId,
                          @RequestBody StorageDto storageDto) {
        storageDto.setClusterId(clusterId);
        storageService.add(storageDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除存储", notes = "删除存储")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageName", value = "存储名称", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping("/{storageName}")
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("storageName") String storageName) {
        storageService.delete(clusterId, storageName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新存储信息", notes = "更新存储信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageDto", value = "存储业务对象", paramType = "query", dataTypeClass = StorageDto.class)
    })
    @PutMapping("/{storageName}")
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @RequestBody StorageDto storageDto) {
        storageDto.setClusterId(clusterId);
        storageService.update(storageDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取存储详情", notes = "获取存储详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "customConfigTemplateDTO", value = "模板对象", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/{storageName}")
    public BaseResult<StorageDto> detail(@PathVariable("clusterId") String clusterId,
                                         @PathVariable("storageName") String storageName) {
        return BaseResult.ok(storageService.detail(clusterId, storageName));
    }

    @ApiOperation(value = "获取中间件存储使用情况", notes = "获取中间件存储使用情况")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageName", value = "存储名称", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/{storageName}/middlewares")
    public BaseResult<List<MiddlewareResourceInfo>> middlewares(@PathVariable("clusterId") String clusterId,
                                                                @PathVariable("storageName") String storageName) {
        return BaseResult.ok(storageService.middlewares(clusterId, storageName));
    }

    @ApiOperation(value = "获取中间件存储使用详情", notes = "获取中间件存储使用详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群ID", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageName", value = "存储名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{storageName}/middlewares/{middlewareName}")
    public BaseResult<List<PodInfo>> pods(@PathVariable("clusterId") String clusterId,
                                          @PathVariable("storageName") String storageName,
                                          @PathVariable("middlewareName") String middlewareName){
        return BaseResult.ok(storageService.pods(clusterId, storageName, middlewareName));
    }

    @ApiOperation(value = "获取中间件存储使用情况", notes = "获取中间件存储使用情况")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageName", value = "存储名称", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/test")
    public BaseResult<List<MiddlewareResourceInfo>> test(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(middlewareCrTypeService.findByType("mysql"));
    }

}
