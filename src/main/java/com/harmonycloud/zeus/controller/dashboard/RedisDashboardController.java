package com.harmonycloud.zeus.controller.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.dashboard.DatabaseDto;
import com.harmonycloud.caas.common.model.dashboard.redis.KeyValueDto;
import com.harmonycloud.zeus.bean.BeanSqlExecuteRecord;
import com.harmonycloud.zeus.service.dashboard.RedisDashboardService;
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

    @ApiOperation(value = "查询指定库全部key", notes = "查询指定库全部key")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "keyword", value = "key关键词", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/databases/{database}/keys")
    public BaseResult<List<KeyValueDto>> listChartset(@PathVariable("clusterId") String clusterId,
                                                 @PathVariable("namespace") String namespace,
                                                 @PathVariable("middlewareName") String middlewareName,
                                                 @PathVariable("database") Integer database,
                                                 @RequestParam(value = "keyword", defaultValue = "") String keyword) {
        return BaseResult.ok(redisDashboardService.getAllKeys(clusterId,  namespace, middlewareName, database, keyword));
    }

    @ApiOperation(value = "获取全部库", notes = "获取全部库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/databases")
    public BaseResult<List<DatabaseDto>> listChartset(@PathVariable("clusterId") String clusterId,
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

    @ApiOperation(value = "修改key信息", notes = "修改key信息,修改名称或超时时间(s)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "key", value = "key", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyValueDto", value = "redis参数", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/databases/{database}/keys/{key}")
    public BaseResult updateKey(@PathVariable("clusterId") String clusterId,
                                @PathVariable("namespace") String namespace,
                                @PathVariable("middlewareName") String middlewareName,
                                @PathVariable("database") Integer database,
                                @PathVariable("key") String key,
                                @RequestBody KeyValueDto keyValueDto) {
        redisDashboardService.updateKey(clusterId, namespace, middlewareName, database, key, keyValueDto);
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

    @ApiOperation(value = "查询cmd历史记录", notes = "查询cmd历史记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "database", value = "数据库名称", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "start", value = "开始时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "end", value = "结束时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageNum", value = "页码", paramType = "query", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "size", value = "每页数量", paramType = "query", dataTypeClass = Integer.class),
    })
    @GetMapping("/databases/{database}/cmd/history")
    public BaseResult<List<BeanSqlExecuteRecord>> listExecuteSql(@PathVariable("clusterId") String clusterId,
                                                                 @PathVariable("namespace") String namespace,
                                                                 @PathVariable("middlewareName") String middlewareName,
                                                                 @PathVariable("database") Integer database,
                                                                 @RequestParam(value = "keyword", required = false) String keyword,
                                                                 @RequestParam(value = "start", required = false) String start,
                                                                 @RequestParam(value = "end", required = false) String end,
                                                                 @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                                 @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        return BaseResult.ok(redisDashboardService.listExecuteSql(clusterId, namespace, middlewareName, database, keyword, start, end, pageNum, size));
    }

}