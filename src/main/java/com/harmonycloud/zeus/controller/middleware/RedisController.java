package com.harmonycloud.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.RedisDbDTO;
import com.harmonycloud.zeus.annotation.Authority;
import com.harmonycloud.zeus.service.middleware.RedisService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = {"服务列表", "数据库管理"}, value = "redis中间件", description = "redis中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/redis")
public class RedisController {

    @Autowired
    private RedisService redisService;

    @ApiOperation(value = "添加kv", notes = "添加kv")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "服务名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "redisDbDTO", value = "redis参数", paramType = "query", dataTypeClass = RedisDbDTO.class)
    })
    @PostMapping
    @Authority(power = 3)
    public BaseResult createDb(@PathVariable("clusterId") String clusterId,
                               @PathVariable(value = "namespace") String namespace,
                               @PathVariable(value = "middlewareName") String middlewareName,
                               @RequestBody RedisDbDTO redisDbDTO) {
        redisService.create(clusterId,namespace,middlewareName,redisDbDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询kv集合", notes = "查询kv集合")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "服务名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "db", value = "数据库", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyWord", value = "关键词", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    @Authority(power = 3)
    public BaseResult ListDb(@PathVariable("clusterId") String clusterId,
                             @PathVariable(value = "namespace") String namespace,
                             @PathVariable(value = "middlewareName") String middlewareName,
                             @RequestParam(value = "db", required = false) String db,
                             @RequestParam(value = "keyWord") String keyWord) {
        return BaseResult.ok(redisService.listRedisDb(clusterId,namespace,middlewareName,db,keyWord));
    }

    @ApiOperation(value = "修改kv", notes = "修改kv")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "服务名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "redisDbDTO", value = "redis参数", paramType = "query", dataTypeClass = RedisDbDTO.class)
    })
    @PutMapping
    @Authority(power = 3)
    public BaseResult updateDb(@PathVariable("clusterId") String clusterId,
                               @PathVariable(value = "namespace") String namespace,
                               @PathVariable(value = "middlewareName") String middlewareName,
                               @RequestBody RedisDbDTO redisDbDTO) {
        redisService.update(clusterId,namespace,middlewareName,redisDbDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除kv", notes = "删除kv")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "服务名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "redisDbDTO", value = "redis参数", paramType = "query", dataTypeClass = RedisDbDTO.class)
    })
    @DeleteMapping
    @Authority(power = 3)
    public BaseResult deleteDb(@PathVariable("clusterId") String clusterId,
                               @PathVariable(value = "namespace") String namespace,
                               @PathVariable(value = "middlewareName") String middlewareName,
                               @RequestBody RedisDbDTO redisDbDTO) {
        redisService.delete(clusterId,namespace,middlewareName,redisDbDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取分片主节点", notes = "获取分片主节点")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "服务名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "slaveName", value = "从节点名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mode", value = "模式", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("burstMaster")
    @Authority(power = 3)
    public BaseResult getBurstMaster(@PathVariable("clusterId") String clusterId,
                                     @PathVariable(value = "namespace") String namespace,
                                     @PathVariable(value = "middlewareName") String middlewareName,
                                     @RequestParam(value = "slaveName") String slaveName,
                                     @RequestParam(value = "mode") String mode) {
        return BaseResult.ok(redisService.getBurstMaster(clusterId, namespace, middlewareName, slaveName, mode));
    }

    @ApiOperation(value = "获取主从对应关系列表", notes = "获取主从对应关系列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "服务名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mode", value = "模式", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("burstList")
    @Authority(power = 3)
    public BaseResult burstList(@PathVariable("clusterId") String clusterId,
                                @PathVariable(value = "namespace") String namespace,
                                @PathVariable(value = "middlewareName") String middlewareName,
                                @RequestParam(value = "mode") String mode){
        return BaseResult.ok(redisService.burstList(clusterId, namespace, middlewareName, mode));
    }
}
