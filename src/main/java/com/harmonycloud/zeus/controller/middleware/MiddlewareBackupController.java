package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.MiddlewareBackupDTO;
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
            @ApiImplicitParam(name = "limitRecord", value = "备份保留数量", paramType = "query", dataTypeClass = Integer.class),
    })
    @PostMapping
    public BaseResult create(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestParam("type") String type,
                             @RequestParam("middlewareName") String middlewareName,
                             @RequestParam(value = "cron", required = false) String cron,
                             @RequestParam(value = "limitRecord", required = false) Integer limitRecord,
                             @RequestParam(value = "pod", required = false) String pod) {
        return middlewareBackupService.createBackup(new MiddlewareBackupDTO(clusterId, namespace, middlewareName, type, cron, limitRecord, pod, null, null));
    }

    @ApiOperation(value = "更新备份规则", notes = "更新备份规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupScheduleName", value = "中间件定时备份规则名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "cron", value = "cron表达式", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "limitRecord", value = "备份保留数量", paramType = "query", dataTypeClass = Integer.class),
    })
    @PutMapping
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestParam("type") String type,
                             @RequestParam("backupScheduleName") String backupScheduleName,
                             @RequestParam("cron") String cron,
                             @RequestParam("limitRecord") Integer limitRecord,
                             @RequestParam(value = "pause", required = false) String pause) {
        return middlewareBackupService.updateBackupSchedule(new MiddlewareBackupDTO(clusterId, namespace, type, cron, limitRecord, pause, backupScheduleName));
    }

    @ApiOperation(value = "查询备份规则列表", notes = "查询备份规则列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "备份源名称关键词", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping()
    public BaseResult listSchedule(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @RequestParam("type") String type,
                                   @RequestParam("middlewareName") String middlewareName,
                                   @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(middlewareBackupService.listBackupSchedule(clusterId, namespace, type, middlewareName, keyword));
    }

    @ApiOperation(value = "删除备份规则", notes = "删除备份规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupScheduleName", value = "备份规则名称", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping()
    public BaseResult deleteSchedule(@PathVariable("clusterId") String clusterId,
                                     @PathVariable("namespace") String namespace,
                                     @RequestParam("type") String type,
                                     @RequestParam("backupScheduleName") String backupScheduleName) {
        return middlewareBackupService.deleteSchedule(clusterId, namespace, type, backupScheduleName);
    }

    @ApiOperation(value = "删除备份记录", notes = "删除备份记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份名称", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping("/record")
    public BaseResult delete(@PathVariable("clusterId") String clusterId,
                             @PathVariable("namespace") String namespace,
                             @RequestParam("type") String type,
                             @RequestParam("middlewareName") String middlewareName,
                             @RequestParam("backupName") String backupName,
                             @RequestParam(value = "backupFileName", required = false) String backupFileName) {
        return middlewareBackupService.deleteRecord(clusterId, namespace, middlewareName, type, backupName, backupFileName);
    }

    @ApiOperation(value = "查询备份记录列表", notes = "查询备份记录列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/record")
    public BaseResult listRecord(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @RequestParam("type") String type,
                                 @RequestParam("middlewareName") String middlewareName) {
        return BaseResult.ok(middlewareBackupService.listRecord(clusterId, namespace, middlewareName, type));
    }

    @ApiOperation(value = "创建备份恢复", notes = "创建备份恢复")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "服务类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "服务名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份记录名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupFileName", value = "备份文件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pods", value = "pod名称", paramType = "query", dataTypeClass = List.class)
    })
    @PostMapping("/restore")
    public BaseResult createRestore(@PathVariable("clusterId") String clusterId,
                                    @PathVariable("namespace") String namespace,
                                    @RequestParam("type") String type,
                                    @RequestParam("middlewareName") String middlewareName,
                                    @RequestParam("backupName") String backupName,
                                    @RequestParam(value = "backupFileName", required = false) String backupFileName,
                                    @RequestParam(value = "pod", required = false) String pod) {
        List<String> pods = new ArrayList<>();
        if (!StringUtils.isBlank(pod)) {
            pods.add(pod);
        }
        return middlewareBackupService.createRestore(clusterId, namespace, middlewareName, type, backupName, backupFileName, pods);
    }

}
