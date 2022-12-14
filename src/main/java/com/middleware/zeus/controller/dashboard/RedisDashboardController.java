package com.middleware.zeus.controller.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.dashboard.DatabaseDto;
import com.middleware.caas.common.model.dashboard.redis.KeyValueDto;
import com.middleware.caas.common.model.dashboard.redis.ScanResult;
import com.middleware.zeus.service.dashboard.RedisDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/10/24 9:23 上午
 */
@Api(tags = {"服务列表", "Redis管理面板"}, value = "redis中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/redis/{middlewareName}")
public class RedisDashboardController {

    @Autowired
    private RedisDashboardService redisDashboardService;

    @ApiOperation(value = "分页查询key", notes = "分页查询key")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "cursor", value = "下一页游标", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "count", value = "每页数量", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pod", value = "下一页pod", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "preCursor", value = "上一页游标", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "prePod", value = "上一页pod", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/keys")
    public BaseResult<ScanResult> scanKeys(@PathVariable("clusterId") String clusterId,
                                           @PathVariable("namespace") String namespace,
                                           @PathVariable("middlewareName") String middlewareName,
                                           @PathVariable("database") Integer database,
                                           @RequestParam(value = "keyword", defaultValue = "") String keyword,
                                           @RequestParam(value = "cursor", defaultValue = "0") Integer cursor,
                                           @RequestParam(value = "count", defaultValue = "10") Integer count,
                                           @RequestParam(value = "pod", defaultValue = "") String pod,
                                           @RequestParam(value = "preCursor", defaultValue = "0") Integer preCursor,
                                           @RequestParam(value = "prePod", defaultValue = "") String prePod) {
        return BaseResult.ok(redisDashboardService.scan(clusterId, namespace, middlewareName, database, keyword, cursor, count, pod, preCursor, prePod));
    }

    @ApiOperation(value = "获取全部库", notes = "获取全部库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/databases")
    public BaseResult<List<DatabaseDto>> getDBList(@PathVariable("clusterId") String clusterId,
                                                      @PathVariable("namespace") String namespace,
                                                      @PathVariable("middlewareName") String middlewareName) {
        return BaseResult.ok(redisDashboardService.getDBList(clusterId, namespace, middlewareName));
    }

    @ApiOperation(value = "查询指定key的value", notes = "查询指定key的value")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "key", value = "key", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/keys/{key}")
    public BaseResult<KeyValueDto> getKeyValue(@PathVariable("clusterId") String clusterId,
                                               @PathVariable("namespace") String namespace,
                                               @PathVariable("middlewareName") String middlewareName,
                                               @PathVariable("database") Integer database,
                                               @PathVariable("key") String key) {
        return BaseResult.ok(redisDashboardService.getKeyValue(clusterId, namespace, middlewareName, database, key));
    }

    @ApiOperation(value = "保存key-value", notes = "保存key-value")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "key", value = "key", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyValueDto", value = "redis参数", paramType = "query", dataTypeClass = KeyValueDto.class),
    })
    @PostMapping("/databases/{database}/keys/{key}")
    public BaseResult setValue(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("middlewareName") String middlewareName,
                               @PathVariable("database") Integer database,
                               @PathVariable("key") String key,
                               @RequestBody KeyValueDto keyValueDto) {
        redisDashboardService.setKeyValue(clusterId, namespace, middlewareName, database, key, keyValueDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新value", notes = "更新value")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "key", value = "key", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyValueDto", value = "redis参数", paramType = "query", dataTypeClass = KeyValueDto.class),
    })
    @PutMapping("/databases/{database}/keys/{key}/value")
    public BaseResult updateValue(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("middlewareName") String middlewareName,
                               @PathVariable("database") Integer database,
                               @PathVariable("key") String key,
                               @RequestBody KeyValueDto keyValueDto) {
        redisDashboardService.updateValue(clusterId, namespace, middlewareName, database, key, keyValueDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除key", notes = "删除key")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "key", value = "key", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/databases/{database}/keys/{key}")
    public BaseResult deleteKey(@PathVariable("clusterId") String clusterId,
                                             @PathVariable("namespace") String namespace,
                                             @PathVariable("middlewareName") String middlewareName,
                                             @PathVariable("database") Integer database,
                                             @PathVariable("key") String key) {
        redisDashboardService.deleteKey(clusterId, namespace, middlewareName, database, key);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改key名", notes = "修改key名")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "key", value = "key", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyValueDto", value = "redis参数", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/databases/{database}/keys/{key}/rename")
    public BaseResult updateKey(@PathVariable("clusterId") String clusterId,
                                @PathVariable("namespace") String namespace,
                                @PathVariable("middlewareName") String middlewareName,
                                @PathVariable("database") Integer database,
                                @PathVariable("key") String key,
                                @RequestBody KeyValueDto keyValueDto) {
        redisDashboardService.renameKey(clusterId, namespace, middlewareName, database, key, keyValueDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改key过期时间", notes = "修改key过期时间")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "key", value = "key", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyValueDto", value = "redis参数", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/databases/{database}/keys/{key}/expiration")
    public BaseResult expireKey(@PathVariable("clusterId") String clusterId,
                                @PathVariable("namespace") String namespace,
                                @PathVariable("middlewareName") String middlewareName,
                                @PathVariable("database") Integer database,
                                @PathVariable("key") String key,
                                @RequestBody KeyValueDto keyValueDto) {
        redisDashboardService.expireKey(clusterId, namespace, middlewareName, database, key, keyValueDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除value", notes = "删除value,非string类型的数据才支持删除value")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "key", value = "key", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "value值", value = "redis参数", paramType = "query", dataTypeClass = KeyValueDto.class),
    })
    @DeleteMapping("/databases/{database}/keys/{key}/value")
    public BaseResult deleteValue(@PathVariable("clusterId") String clusterId,
                                  @PathVariable("namespace") String namespace,
                                  @PathVariable("middlewareName") String middlewareName,
                                  @PathVariable("database") Integer database,
                                  @PathVariable("key") String key,
                                  @RequestBody KeyValueDto keyValueDto) {
        redisDashboardService.deleteValue(clusterId, namespace, middlewareName, database, key, keyValueDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "执行cmd", notes = "执行cmd")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "cmd", value = "命令", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("/databases/{database}/cmd")
    public BaseResult<JSONObject> execCMD(@PathVariable("clusterId") String clusterId,
                              @PathVariable("namespace") String namespace,
                              @PathVariable("middlewareName") String middlewareName,
                              @PathVariable("database") Integer database,
                              @RequestParam("cmd") String cmd) {
        return BaseResult.ok(redisDashboardService.execCMD(clusterId, namespace, middlewareName, database, cmd));
    }

}
