package com.middleware.zeus.controller.middleware;

import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.BackupServerDTO;
import com.middleware.zeus.common.model.BackupServerQueryDto;
import com.middleware.zeus.service.middleware.BackupServerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/9 2:25 下午
 */
@Api(tags = {"备份服务", "备份服务器"}, value = "备份服务器")
@RestController
@RequestMapping("/backup/server")
public class BackupServerController {

    @Autowired
    private BackupServerService backupServerService;

    @ApiOperation(value = "查询备份服务器列表", notes = "查询备份服务器列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "backupServerQueryDto", value = "查询信息", paramType = "query", dataTypeClass = BackupServerQueryDto.class),
    })
    @PostMapping("/list")
    public BaseResult<List<BackupServerDTO>> list(@RequestBody BackupServerQueryDto backupServerQueryDto) {
        return BaseResult.ok(backupServerService.list(backupServerQueryDto.getClusterIds(), backupServerQueryDto.getKeyword(), backupServerQueryDto.getWithDetail()));
    }

    @ApiOperation(value = "创建备份服务器", notes = "创建备份服务器")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "backupServerDTO", value = "备份服务器", paramType = "query", dataTypeClass = BackupServerDTO.class),
    })
    @PostMapping
    public BaseResult create(@RequestBody BackupServerDTO backupServerDTO) {
        backupServerService.create(backupServerDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新备份服务器", notes = "更新备份服务器")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "backupServerDTO", value = "备份服务器", paramType = "query", dataTypeClass = BackupServerDTO.class),
    })
    @PutMapping
    public BaseResult update(@RequestBody BackupServerDTO backupServerDTO) {
        backupServerService.update(backupServerDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "分配备份服务器给集群", notes = "分配备份服务器给集群")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "backupServerId", value = "备份服务器Id", paramType = "path", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "clusterId", value = "集群Id", paramType = "path", dataTypeClass = String.class),
    })
    @PutMapping("/{backupServerId}/clusters/{clusterId}/allocate")
    public BaseResult allocate(@PathVariable("backupServerId") Integer backupServerId,
                               @PathVariable("clusterId") String clusterId) {
        backupServerService.allocate(backupServerId, clusterId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除备份服务器", notes = "删除备份服务器")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "backupServerId", value = "备份服务器id", paramType = "path", dataTypeClass = Integer.class),
    })
    @DeleteMapping("/{backupServerId}")
    public BaseResult delete(@PathVariable("backupServerId") Integer backupServerId) {
        backupServerService.delete(backupServerId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询备份服务器数量信息", notes = "查询备份服务器数量信息")
    @GetMapping("/count")
    public BaseResult getServerCountInfo() {
        return BaseResult.ok(backupServerService.getBackupServerCountInfo());
    }

}
