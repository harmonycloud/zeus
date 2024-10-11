package com.middleware.zeus.controller.k8s;

import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.QuotaBase;
import com.middleware.zeus.common.model.StorageDto;
import com.middleware.zeus.common.model.middleware.MiddlewareStorageInfoDto;
import com.middleware.zeus.service.k8s.StorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/6/6 4:23 下午
 */
@Api(tags = {"平台管理", "存储管理"}, value = "存储服务", description = "存储服务")
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
            @ApiImplicitParam(name = "storageId", value = "存储名称", paramType = "path", dataTypeClass = String.class)
    })
    @DeleteMapping("/{storageId}")
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("storageId") String storageId) {
        storageService.delete(clusterId, storageId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新存储信息", notes = "更新存储信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageId", value = "存储id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageDto", value = "存储业务对象", paramType = "query", dataTypeClass = StorageDto.class)
    })
    @PutMapping("/{storageId}")
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("storageId") String storageId,
                             @RequestBody StorageDto storageDto) {
        storageDto.setClusterId(clusterId);
        storageDto.setStorageId(storageId);
        storageService.addOrUpdate(storageDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取存储详情", notes = "获取存储详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageName", value = "存储名称", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/{storageName}")
    public BaseResult<StorageDto> get(@PathVariable("clusterId") String clusterId,
                                      @PathVariable("storageName") String storageName) {
        return BaseResult.ok(storageService.get(clusterId, storageName));
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

    @ApiOperation(value = "获取存储资源", notes = "获取存储资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/monitor")
    public BaseResult<Map<String, Map<String, QuotaBase>>> monitor(@PathVariable("clusterId") String clusterId) {
        return BaseResult.ok(storageService.monitorStorageQuota(clusterId));
    }

}
