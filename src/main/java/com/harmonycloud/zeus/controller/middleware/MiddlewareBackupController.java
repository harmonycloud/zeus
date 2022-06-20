package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.MiddlewareBackupDTO;
import com.harmonycloud.zeus.annotation.Authority;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareBackupServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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

    @ApiOperation(value = "创建备份", notes = "创建备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "cron", value = "cron表达式", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "retentionTime", value = "保留时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "taskName", value = "任务名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "addressName", value = "备份地址", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping
    public BaseResult create(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestParam("type") String type,
                             @RequestParam("middlewareName") String middlewareName,
                             @RequestParam(value = "cron", required = false) String cron,
                             @RequestParam(value = "retentionTime", required = false) String retentionTime,
                             @RequestParam(value = "taskName") String taskName,
                             @RequestParam(value = "addressName") String addressName,
                             @RequestParam(value = "pod", required = false) String pod) {
        middlewareBackupService.createBackup(new MiddlewareBackupDTO(clusterId, namespace, middlewareName, type, cron, retentionTime, taskName, addressName, pod, null, null));
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除备份任务", notes = "删除备份任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份规则名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupFileName", value = "备份文件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "addressName", value = "备份地址名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "cron", value = "cron", paramType = "query", dataTypeClass = String.class)

    })
    @DeleteMapping("/task")
    @Authority(power = 1)
    public BaseResult deleteSchedule(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @RequestParam("type") String type,
                                     @RequestParam("backupName") String backupName,
                                     @RequestParam("backupFileName") String backupFileName,
                                     @RequestParam("addressName") String addressName,
                                     @RequestParam("cron") String cron) {
        middlewareBackupService.deleteBackUpTask(clusterId, namespace, type, backupName, backupFileName, addressName, cron);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除备份记录", notes = "删除备份记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份规则名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupFileName", value = "备份文件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "addressName", value = "备份地址名称", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/record")
    @Authority(power = 1)
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestParam("type") String type,
                             @RequestParam("backupName") String backupName,
                             @RequestParam("backupFileName") String backupFileName,
                             @RequestParam("addressName") String addressName) {
        middlewareBackupService.deleteBackUpRecord(clusterId, namespace, type, backupName, backupFileName, addressName);
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
    @GetMapping("/task")
    @Authority(power = 1)
    public BaseResult listRecord(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @RequestParam(value = "type", required = false) String type,
                                 @RequestParam(value = "middlewareName", required = false) String middlewareName,
                                 @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(middlewareBackupService.backupTaskList(clusterId, namespace, middlewareName, type, keyword));
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
            @ApiImplicitParam(name = "backupFileName", value = "备份文件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "addressName", value = "备份地址名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pods", value = "pod名称", paramType = "query", dataTypeClass = List.class)
    })
    @PostMapping("/restore")
    @Authority(power = 1)
    public BaseResult createRestore(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @RequestParam("type") String type,
                                    @RequestParam("middlewareName") String middlewareName,
                                    @RequestParam("backupName") String backupName,
                                    @RequestParam(value = "backupFileName", required = false) String backupFileName,
                                    @RequestParam(value = "addressName", required = false) String addressName,
                                    @RequestParam(value = "pod", required = false) String pod) {
        List<String> pods = new ArrayList<>();
        if (!StringUtils.isBlank(pod)) {
            pods.add(pod);
        }
        middlewareBackupService.createRestore(clusterId, namespace, middlewareName, type, backupName, backupFileName, pods, addressName);
        return BaseResult.ok();
    }

}
