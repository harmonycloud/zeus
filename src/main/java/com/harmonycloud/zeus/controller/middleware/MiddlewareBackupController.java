package com.harmonycloud.zeus.controller.middleware;

import java.util.List;

import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareBackupServiceImpl;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackup;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = {"容灾备份", "数据安全"}, value = "中间件备份", description = "中间件备份")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/backups")
public class MiddlewareBackupController {

    @Autowired
    private MiddlewareBackupServiceImpl middlewareBackupService;

    @ApiOperation(value = "创建中间件备份", notes = "创建中间件备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "cron", value = "cron表达式", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "limitRecord", value = "备份保留数量", paramType = "query", dataTypeClass = Integer.class),
    })
    @PostMapping
    public BaseResult create(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String middlewareName,
                             @RequestParam("type") String type,
                             @RequestParam(value = "cron", required = false) String cron,
                             @RequestParam(value = "limitRecord", required = false) Integer limitRecord) {
        return middlewareBackupService.create(clusterId, namespace, middlewareName, type, cron, limitRecord);
    }

    @ApiOperation(value = "更新中间件备份配置", notes = "更新中间件备份配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "cron", value = "cron表达式", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "limitRecord", value = "备份保留数量", paramType = "query", dataTypeClass = Integer.class),
    })
    @PutMapping
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String middlewareName,
                             @RequestParam("type") String type,
                             @RequestParam(value = "cron", required = false) String cron,
                             @RequestParam(value = "limitRecord", required = false) Integer limitRecord,
                             @RequestParam(value = "pause", required = false) String pause) {
        return middlewareBackupService.update(clusterId, namespace, middlewareName, type, cron, limitRecord, pause);
    }

    @ApiOperation(value = "删除中间件备份", notes = "删除中间件备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份名称", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @PathVariable("middlewareName") String middlewareName,
                             @RequestParam(value = "backupName") String backupName,
                             @RequestParam(value = "type") String type,
                             @RequestParam(value = "backupFileName", required = false) String backupFileName) {
        return middlewareBackupService.delete(clusterId, namespace, middlewareName, type, backupName, backupFileName);
    }

    @ApiOperation(value = "查询中间件备份数据列表", notes = "查询中间件备份数据列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping(value = "list")
    public BaseResult list(@PathVariable("clusterId") String clusterId,
                           @PathVariable("namespace") String namespace,
                           @RequestParam("type") String type,
                           @PathVariable("middlewareName") String middlewareName) {
        return BaseResult.ok(middlewareBackupService.list(clusterId, namespace, middlewareName, type));
    }

    @ApiOperation(value = "查询备份设置", notes = "查询备份设置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult get(@PathVariable("clusterId") String clusterId,
                          @PathVariable("namespace") String namespace,
                          @PathVariable("middlewareName") String middlewareName,
                          @RequestParam("type") String type) {
        return middlewareBackupService.get(clusterId, namespace, middlewareName, type);
    }

    @ApiOperation(value = "创建备份恢复", notes = "创建备份恢复")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份记录名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "restoreName", value = "备份服务名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "aliasName", value = "服务别名", paramType = "query", dataTypeClass = String.class)
    })
    @PostMapping("restore")
    public BaseResult createRestore(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @PathVariable("middlewareName") String middlewareName,
                                    @RequestParam("type") String type,
                                    @RequestParam("backupName") String backupName,
                                    @RequestParam("backupFileName") String backupFileName,
                                    @RequestParam("restoreName") String restoreName,
                                    @RequestParam(value = "aliasName", required = false) String aliasName) {
        return middlewareBackupService.createRestore(clusterId, namespace, middlewareName, type, restoreName, backupName, backupFileName, aliasName);
    }

}
