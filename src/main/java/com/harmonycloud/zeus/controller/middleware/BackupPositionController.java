package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.BackupPositionDTO;
import com.harmonycloud.caas.common.model.BackupServerDTO;
import com.harmonycloud.zeus.service.middleware.BackupPositionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author liyinlong
 * @since 2023/1/12 7:48 上午
 */
@Api(tags = {"备份服务", "备份位置"}, value = "备份位置")
@RestController
@RequestMapping("/backup/position")
public class BackupPositionController {

    @Autowired
    private BackupPositionService backupPositionService;

    @ApiOperation(value = "创建备份位置", notes = "创建备份位置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "backupPositionDTO", value = "备份位置", paramType = "query", dataTypeClass = BackupPositionDTO.class),
    })
    @PostMapping
    public BaseResult create(@RequestBody BackupPositionDTO backupPositionDTO){
        backupPositionService.create(backupPositionDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除备份位置", notes = "删除备份位置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "backupServerId", value = "备份位置id", paramType = "path", dataTypeClass = Integer.class),
    })
    @DeleteMapping("/{backupPositionId}")
    public BaseResult delete(@PathVariable("backupPositionId") Integer backupPositionId) {
        backupPositionService.delete(backupPositionId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询项目备份位置列表", notes = "查询项目备份位置列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/project/{projectId}")
    public BaseResult list(@PathVariable(value = "projectId", required = false) String projectId) {
        return BaseResult.ok();
    }

}
