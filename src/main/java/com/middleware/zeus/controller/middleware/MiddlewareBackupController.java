package com.middleware.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.MiddlewareBackupDTO;
import com.middleware.caas.common.model.MiddlewareIncBackupDto;
import com.middleware.caas.common.model.MiddlewareTaskDTO;
import com.middleware.caas.common.model.middleware.MiddlewareBackupRecord;
import com.middleware.caas.common.model.middleware.MiddlewareBackupRecordGroup;
import com.middleware.caas.common.model.middleware.MiddlewareBackupRestore;
import com.middleware.caas.common.model.middleware.MiddlewareRestoreDto;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.service.middleware.MiddlewareBackupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author liyinlong
 * @date 2021/03/23
 */
@Api(tags = {"容灾备份", "数据安全"}, value = "中间件备份", description = "中间件备份")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/backup")
public class MiddlewareBackupController {

    @Qualifier("middlewareBackupServiceImpl")
    @Autowired
    private MiddlewareBackupService middlewareBackupService;

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
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    @Authority(power = 1)
    public BaseResult<List<MiddlewareBackupRecordGroup>> listRecord(@PathVariable("clusterId") String clusterId,
                                                                    @PathVariable("namespace") String namespace,
                                                                    @RequestParam(value = "organId", required = false) String organId,
                                                                    @RequestParam(value = "projectId", required = false) String projectId,
                                                                    @RequestParam(value = "type", required = false) String type,
                                                                    @RequestParam(value = "middlewareName", required = false) String middlewareName,
                                                                    @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(middlewareBackupService.backupTaskGroupList(clusterId, namespace, middlewareName, organId, projectId, type, keyword));
    }

    @ApiOperation(value = "查询备份任务详情", notes = "查询备份任务详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份任务名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupId", value = "备份任务id", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/detail")
    public BaseResult<List<MiddlewareBackupRecord>> get(@PathVariable("clusterId") String clusterId,
                                                  @PathVariable("namespace") String namespace,
                                                  @RequestParam("backupName") String backupName,
                                                  @RequestParam("backupId") String backupId) {
        return BaseResult.ok(middlewareBackupService.getBackup(clusterId, namespace, backupName, backupId));
    }

    @ApiOperation(value = "检查中间件是否已创建周期备份", notes = "检查中间件是否已创建周期备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/checkSchedule")
    public BaseResult checkSchedule(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @RequestParam("type") String type,
                                    @RequestParam("middlewareName") String middlewareName) {
        return BaseResult.ok(middlewareBackupService.checkSchedule(clusterId, namespace, type, middlewareName));
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
            @ApiImplicitParam(name = "taskDTO", value = "备份任务信息", paramType = "query", dataTypeClass = Boolean.class)
    })
    @DeleteMapping
    @Authority(power = 1)
    public BaseResult deleteSchedule(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @RequestBody MiddlewareTaskDTO taskDTO){
        taskDTO.setClusterId(clusterId).setNamespace(namespace);
        middlewareBackupService.deleteBackUpTask(taskDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除备份记录", notes = "删除备份记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupId", value = "备份任务ID", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "forceDelete", value = "是否强制删除", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/record")
    @Authority(power = 1)
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestParam("type") String type,
                             @RequestParam("backupName") String backupName,
                             @RequestParam(value = "backupId", required = false) String backupId,
                             @RequestParam(value = "forceDelete", required = false, defaultValue = "false") Boolean forceDelete) {
        middlewareBackupService.deleteBackUpRecord(clusterId, namespace, type, backupName, backupId, forceDelete);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询备份任务对应的全量记录", notes = "查询备份任务对应的全量记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupId", value = "备份任务id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupMode", value = "备份类型(single:单次，period:周期)", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/record")
    @Authority(power = 1)
    public BaseResult<List<MiddlewareBackupRecord>> listTaskRecord(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @RequestParam("middlewareName") String middlewareName,
                                     @RequestParam("type") String type,
                                     @RequestParam("backupId") String backupId,
                                     @RequestParam("backupMode") String backupMode) {
        return BaseResult.ok(middlewareBackupService.backupRecords(clusterId, namespace, middlewareName, type, backupId, backupMode));
    }

    @ApiOperation(value = "查询备份任务对应的增量记录", notes = "查询备份任务对应的增量记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupId", value = "备份任务id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupMode", value = "备份类型(single:单次，period:周期)", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/incrRecord")
    @Authority(power = 1)
    public BaseResult<List<MiddlewareBackupRecord>> listIncrRecord(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @RequestParam("middlewareName") String middlewareName,
                                     @RequestParam("type") String type,
                                     @RequestParam("backupId") String backupId,
                                     @RequestParam("backupMode") String backupMode) {
        return BaseResult.ok(middlewareBackupService.backupIncrRecords(clusterId, namespace, middlewareName, type, backupId, backupMode));
    }

    @ApiOperation(value = "查询备份任务对应的克隆记录", notes = "查询备份任务对应的克隆记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupId", value = "备份任务id", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/restore")
    @Authority(power = 1)
    public BaseResult<List<MiddlewareBackupRestore>> listRestoreRecord(@PathVariable("clusterId") String clusterId,
                                                                       @PathVariable("namespace") String namespace,
                                                                       @RequestParam("backupId") String backupId) {
        return BaseResult.ok(middlewareBackupService.backupRestores(clusterId, namespace, backupId));
    }

    @ApiOperation(value = "创建备份恢复", notes = "创建备份恢复")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "服务类型", paramType = "query", dataTypeClass = MiddlewareRestoreDto.class),
    })
    @PostMapping("/restore")
    @Authority(power = 2)
    public BaseResult createRestore(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @RequestBody MiddlewareRestoreDto restoreDto) {
        restoreDto.setClusterId(clusterId);
        restoreDto.setNamespace(namespace);
        middlewareBackupService.createRestore(restoreDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除克隆记录", notes = "删除克隆记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "restoreName", value = "克隆记录名称", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/restore")
    @Authority(power = 1)
    public BaseResult<List<MiddlewareBackupRestore>> deleteRestoreRecord(@PathVariable("clusterId") String clusterId,
                                                                       @PathVariable("namespace") String namespace,
                                                                       @RequestParam("restoreName") String restoreName) {
        middlewareBackupService.deleteRestoreRecord(clusterId, namespace, restoreName);
        return BaseResult.ok();
    }

}
