package com.middleware.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.MiddlewareBackupDTO;
import com.middleware.caas.common.model.MiddlewareIncBackupDto;
import com.middleware.caas.common.util.ThreadPoolExecutorFactory;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.service.middleware.impl.MiddlewareBackupServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author liyinlong
 * @date 2021/03/23
 */
@Api(tags = {"容灾备份", "数据安全"}, value = "中间件备份", description = "中间件备份")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/backup")
public class MiddlewareBackupController {

    @Autowired
    private MiddlewareBackupServiceImpl middlewareBackupService;

    @ApiOperation(value = "创建全量备份", notes = "创建备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareBackupDTO", value = "中间件备份对象", paramType = "query", dataTypeClass = MiddlewareBackupDTO.class),
    })
    @PostMapping
    public BaseResult create(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestBody MiddlewareBackupDTO middlewareBackupDTO) {
        middlewareBackupDTO.setClusterId(clusterId).setNamespace(namespace);
        middlewareBackupService.createBackup(middlewareBackupDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "创建增量备份", notes = "创建增量备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "time", value = "间隔时间", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("{backupName}/inc")
    public BaseResult createInc(@PathVariable("clusterId") String clusterId,
                                @PathVariable("namespace") String namespace,
                                @PathVariable("backupName") String backupName,
                                @RequestParam("time") String time) {
        middlewareBackupService.createIncBackup(clusterId, namespace, backupName, time);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改备份", notes = "修改备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareBackupDTO", value = "中间件备份对象", paramType = "query", dataTypeClass = MiddlewareBackupDTO.class),
    })
    @PutMapping
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestBody MiddlewareBackupDTO middlewareBackupDTO) {
        middlewareBackupDTO.setClusterId(clusterId).setNamespace(namespace);
        middlewareBackupService.updateBackupSchedule(middlewareBackupDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询备份任务列表", notes = "查询备份任务列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    @Authority(power = 1)
    public BaseResult listRecord(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @RequestParam(value = "type", required = false) String type,
                                 @RequestParam(value = "middlewareName", required = false) String middlewareName,
                                 @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(middlewareBackupService.backupTaskList(clusterId, namespace, middlewareName, type, keyword));
    }

    @ApiOperation(value = "查询增量备份信息", notes = "查询增量备份信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{backupName}/inc")
    @Authority(power = 1)
    public BaseResult<MiddlewareIncBackupDto> getIncBackupInfo(@PathVariable("clusterId") String clusterId,
                                                               @PathVariable("namespace") String namespace,
                                                               @PathVariable("backupName") String backupName) {
        return BaseResult.ok(middlewareBackupService.getIncBackupInfo(clusterId, namespace, backupName));
    }

    @ApiOperation(value = "删除备份任务", notes = "删除备份任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份规则名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupId", value = "备份任务ID", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "schedule", value = "schedule", paramType = "query", dataTypeClass = Boolean.class)
    })
    @DeleteMapping
    @Authority(power = 1)
    public BaseResult deleteSchedule(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @RequestParam("type") String type,
                                     @RequestParam("backupName") String backupName,
                                     @RequestParam(value = "backupId", required = false) String backupId,
                                     @RequestParam("schedule") Boolean schedule){
        middlewareBackupService.deleteBackUpTask(clusterId, namespace, type, backupName, backupId, schedule);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除备份记录", notes = "删除备份记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupId", value = "备份任务ID", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/record")
    @Authority(power = 1)
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestParam("type") String type,
                             @RequestParam("backupName") String backupName,
                             @RequestParam(value = "backupId", required = false) String backupId) {
        middlewareBackupService.deleteBackUpRecord(clusterId, namespace, type, backupName, backupId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询备份任务对应的备份记录", notes = "查询备份任务对应的备份记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份名称", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/record")
    @Authority(power = 1)
    public BaseResult listTaskRecord(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @RequestParam(value = "type", required = false) String type,
                                    @RequestParam(value = "backupName", required = false) String backupName) {
        return BaseResult.ok(middlewareBackupService.backupRecords(clusterId, namespace, backupName, type));
    }


    @ApiOperation(value = "创建备份恢复", notes = "创建备份恢复")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "服务类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "服务名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份记录名称", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("/restore")
    @Authority(power = 1)
    public BaseResult createRestore(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @RequestParam("type") String type,
                                    @RequestParam("middlewareName") String middlewareName,
                                    @RequestParam(value = "backupName", required = false) String backupName,
                                    @RequestParam(value = "restoreTime", required = false) String restoreTime) {
        ThreadPoolExecutorFactory.executor.execute(() -> middlewareBackupService.createRestore(clusterId, namespace,
            middlewareName, type, backupName, restoreTime));
        return BaseResult.ok();
    }

}
