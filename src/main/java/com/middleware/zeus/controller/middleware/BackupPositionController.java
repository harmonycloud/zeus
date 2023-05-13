package com.middleware.zeus.controller.middleware;

import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.BackupPositionDTO;
import com.middleware.zeus.service.middleware.BackupPositionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/12 7:48 上午
 */
@Api(tags = {"备份服务", "备份位置"}, value = "备份位置")
@RestController
@RequestMapping("/organizations/{organId}/project/{projectId}/position")
public class BackupPositionController {

    @Autowired
    private BackupPositionService backupPositionService;

    @ApiOperation(value = "创建备份位置", notes = "创建备份位置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupServerId", value = "备份服务器id", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "backupPositionDTO", value = "备份位置", paramType = "query", dataTypeClass = BackupPositionDTO.class),
    })
    @PostMapping
    public BaseResult create(@PathVariable("organId") String organId,
                             @PathVariable("projectId") String projectId,
                             @RequestBody BackupPositionDTO backupPositionDTO) {
        backupPositionDTO.setOrganId(organId);
        backupPositionDTO.setProjectId(projectId);
        backupPositionService.create(backupPositionDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除备份位置", notes = "删除备份位置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupServerId", value = "备份位服务器id", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "backupPositionId", value = "备份位置id", paramType = "path", dataTypeClass = Integer.class),
    })
    @DeleteMapping("/{backupPositionId}")
    public BaseResult delete(@PathVariable("backupPositionId") Integer backupPositionId) {
        backupPositionService.delete(backupPositionId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新备份位置", notes = "更新备份位置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupServerId", value = "备份服务器id", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "backupPositionDTO", value = "备份位置", paramType = "query", dataTypeClass = BackupPositionDTO.class),
    })
    @PutMapping
    public BaseResult update(@PathVariable("organId") String organId,
                             @PathVariable("projectId") String projectId,
                             @RequestBody BackupPositionDTO backupPositionDTO) {
        backupPositionDTO.setOrganId(organId);
        backupPositionDTO.setProjectId(projectId);
        backupPositionService.update(backupPositionDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询项目备份位置列表", notes = "查询项目备份位置列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupServerId", value = "备份服务器id", paramType = "path", dataTypeClass = Integer.class),
    })
    @GetMapping
    public BaseResult<List<BackupPositionDTO>> list(@PathVariable("organId") String organId,
                                                    @PathVariable("projectId") String projectId) {
        return BaseResult.ok(backupPositionService.list(organId, projectId, null));
    }

    @ApiOperation(value = "查询集群分区可用备份位置列表", notes = "查询集群分区可用备份位置列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/usable")
    public BaseResult<List<BackupPositionDTO>> listByNamespace(@PathVariable("organId") String organId,
                                                               @PathVariable("projectId") String projectId,
                                                               @RequestParam("clusterId") String clusterId,
                                                               @RequestParam("namespace") String namespace) {
        return BaseResult.ok(backupPositionService.usable(organId, projectId, clusterId, namespace));
    }

}
