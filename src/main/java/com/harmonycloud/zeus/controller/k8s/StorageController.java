package com.harmonycloud.zeus.controller.k8s;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.StorageDto;
import com.harmonycloud.zeus.service.k8s.StorageService;
import com.middleware.caas.common.model.middleware.MiddlewareStorageInfoDto;
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
@Api(tags = {"存储管理", "存储管理"}, value = "存储服务", description = "存储服务")
@RestController
@RequestMapping("/clusters/{clusterId}/storage")
public class StorageController {

    @Autowired
    private StorageService storageService;

    @ApiOperation(value = "查询存储类型", notes = "查询存储类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/type")
    public BaseResult<List<String>> getType(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(storageService.getType());
    }


    @ApiOperation(value = "查询存储列表", notes = "查询存储列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "key", value = "关键词搜索", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "存储类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "all", value = "是否全部", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<List<StorageDto>> list(@PathVariable("clusterId") String clusterId,
                                             @RequestParam(value = "key", required = false) String key,
                                             @RequestParam(value = "type", required = false) String type,
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
        storageService.addOrUpdate(storageDto);
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
            @ApiImplicitParam(name = "storageName", value = "存储名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageDto", value = "存储业务对象", paramType = "query", dataTypeClass = StorageDto.class)
    })
    @PutMapping("/{storageName}")
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("storageName") String storageName,
                             @RequestBody StorageDto storageDto) {
        storageDto.setClusterId(clusterId);
        storageDto.setName(storageName);
        storageService.addOrUpdate(storageDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取存储详情", notes = "获取存储详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageName", value = "存储名称", paramType = "query", dataTypeClass = String.class)
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
    public BaseResult<List<MiddlewareStorageInfoDto>> middlewares(@PathVariable("clusterId") String clusterId,
                                                                  @PathVariable("storageName") String storageName) {
        return BaseResult.ok(storageService.middlewares(clusterId, storageName));
    }

}
