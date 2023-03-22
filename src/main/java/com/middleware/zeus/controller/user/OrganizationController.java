package com.middleware.zeus.controller.user;

import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.caas.common.model.user.OrganizationQuota;
import com.middleware.caas.common.model.user.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.user.OrganizationDto;
import com.middleware.zeus.service.user.OrganizationService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/6 4:22 下午
 */
@Slf4j
@Api(tags = {"系统管理","组织管理"}, value = "组织", description = "组织")
@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @ApiOperation(value = "创建组织", notes = "创建组织")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organizationDto", value = "组织对象", paramType = "query", dataTypeClass = OrganizationDto.class),
    })
    @PostMapping
    public BaseResult create(@RequestBody OrganizationDto organizationDto) {
        organizationService.add(organizationDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新组织信息", notes = "更新组织信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "organizationDto", value = "组织对象", paramType = "query", dataTypeClass = OrganizationDto.class),
    })
    @PutMapping("/{organId}")
    public BaseResult update(@PathVariable("organId") String organId,
                             @RequestBody OrganizationDto organizationDto) {
        organizationDto.setOrganId(organId);
        organizationService.update(organizationDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询组织列表", notes = "查询组织列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", value = "关键词", required = false, paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<List<OrganizationDto>> list(@RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(organizationService.list(keyword));
    }

    @ApiOperation(value = "删除组织", notes = "删除组织")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/{organId}")
    public BaseResult delete(@PathVariable("organId") String organId) {
        organizationService.delete(organId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取组织信息", notes = "获取组织信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{organId}")
    public BaseResult<OrganizationDto> get(@PathVariable("organId") String organId) {
        return BaseResult.ok(organizationService.get(organId));
    }

    @ApiOperation(value = "分配资源", notes = "分配资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "organizationQuota", value = "组织配额", paramType = "query", dataTypeClass = OrganizationQuota.class),
    })
    @PostMapping("/{organId}/quota")
    public BaseResult allocateQuota(@PathVariable("organId") String organId,
                                    @RequestBody OrganizationQuota organizationQuota) {
        organizationQuota.setOrganId(organId);
        organizationService.allocateQuota(organizationQuota);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询组织存储配额", notes = "查询组织存储配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", required = false, paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "detail", value = "是否包含项目配额分配情况", paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping("/{organId}/storage")
    public BaseResult<List<ResourceQuotaDo>> getStorageQuota(@PathVariable("organId") String organId,
                                                             @RequestParam(value = "clusterId", required = false) String clusterId,
                                                             @RequestParam(value = "detail", defaultValue = "false") Boolean detail) {
        return BaseResult.ok(organizationService.getStorageQuota(organId, clusterId, detail));
    }

    @ApiOperation(value = "移除组织存储配额", notes = "移除组织存储配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageId", value = "存储名id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{organId}/storage/{storageId}")
    public BaseResult removeStorageQuota(@PathVariable("organId") String organId,
                                         @PathVariable("storageId") String storageId,
                                         @RequestParam("clusterId") String clusterId) {
        organizationService.removeStorageQuota(organId, storageId, clusterId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询组织cpu memory配额", notes = "查询组织cpu memory配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "detail", value = "是否包含项目配额分配情况", paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping("/{organId}/cpuMemory")
    public BaseResult<List<ResourceQuotaDo>> getCpuAndMemoryQuota(@PathVariable("organId") String organId,
                                                                  @RequestParam(value = "detail", defaultValue = "false") Boolean detail) {
        return BaseResult.ok(organizationService.getCpuMemoryQuota(organId, detail));
    }

    @ApiOperation(value = "移除组织cpu memory配额", notes = "移除组织cpu memory配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{organId}/cpuMemory")
    public BaseResult removeCpuAndMemoryQuota(@PathVariable("organId") String organId,
                                              @RequestParam("clusterId") String clusterId) {
        organizationService.removeCpuMemoryQuota(organId, clusterId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询备份服务器", notes = "查询备份服务器")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", required = false, paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "detail", value = "查询备份服务器使用情况", paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping("/{organId}/backupServer")
    public BaseResult<List<BackupServerDTO>> listBackupServer(@PathVariable("organId") String organId,
                                                              @RequestParam(value = "clusterId", required = false) String clusterId,
                                                              @RequestParam(value = "detail", defaultValue = "false") Boolean detail) {
        return BaseResult.ok(organizationService.getBackupServer(organId, clusterId, detail));
    }

    @ApiOperation(value = "移除备份服务器", notes = "移除备份服务器")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupServerId", value = "备份服务器id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{organId}/backupServer/{backupServerId}")
    public BaseResult removeBackupServer(@PathVariable("organId") String organId,
                                         @PathVariable("backupServerId") Integer backupServerId,
                                         @RequestParam("clusterId") String clusterId) {
        organizationService.removeBackupServer(organId, backupServerId, clusterId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询备份服务器用户列表", notes = "查询备份服务器用户列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{organId}/user")
    public BaseResult<List<UserDto>> listOrganUser(@PathVariable("organId") String organId,
                                                   @RequestParam("allocatable") Boolean allocatable) {
        return BaseResult.ok(organizationService.listOrganUser(organId, allocatable));
    }

    @ApiOperation(value = "添加组织用户成员", notes = "添加组织用户成员")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "organizationDto", value = "组织对象", paramType = "query", dataTypeClass = OrganizationDto.class),
    })
    @PostMapping("/{organId}/user")
    public BaseResult addOrganUser(@PathVariable("organId") String organId,
                                   @RequestBody OrganizationDto organizationDto) {
        organizationDto.setOrganId(organId);
        organizationService.addOrganUser(organizationDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新组织用户成员角色", notes = "更新组织用户成员角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "roleId", required = false, value = "角色id", paramType = "query", dataTypeClass = Integer.class),
    })
    @PutMapping("/{organId}/user")
    public BaseResult updateOrganUser(@PathVariable("organId") String organId,
                                      @RequestParam("username") String username,
                                      @RequestParam(value = "roleId", required = false) Integer roleId) {
        organizationService.updateOrganUser(organId, username, roleId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新组织用户成员角色", notes = "更新组织用户成员角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{organId}/user")
    public BaseResult deleteOrganUser(@PathVariable("organId") String organId,
                                      @RequestParam("username") String username) {
        organizationService.deleteOrganUser(organId, username);
        return BaseResult.ok();
    }

}
