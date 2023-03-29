package com.middleware.zeus.controller.user;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.BackupPositionDTO;
import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.MiddlewareInfoDTO;
import com.middleware.caas.common.model.middleware.Namespace;
import com.middleware.caas.common.model.middleware.ProjectMiddlewareResourceInfo;
import com.middleware.caas.common.model.user.OrganizationQuota;
import com.middleware.caas.common.model.user.ProjectDto;
import com.middleware.caas.common.model.user.ProjectQuota;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.zeus.service.middleware.MiddlewareInfoService;
import com.middleware.zeus.service.user.ProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * @author xutianhong
 * @Date 2022/3/23 5:21 下午
 */
@Slf4j
@Api(tags = {"系统管理","项目管理"}, value = "项目", description = "项目")
@RestController
@RequestMapping("/organizations/{organId}/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;

    @ApiOperation(value = "创建项目", notes = "创建项目")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectDto", value = "项目对象", paramType = "query", dataTypeClass = ProjectDto.class),
    })
    @PostMapping
    public BaseResult create(@PathVariable("organId") String organId,
                             @RequestBody ProjectDto projectDto) {
        projectDto.setOrganId(organId);
        projectService.add(projectDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取项目列表", notes = "获取项目列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "key", value = "关键词检索", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<List<ProjectDto>> list(@PathVariable("organId") String organId,
                                             @RequestParam(value = "key", required = false) String key) {
        return BaseResult.ok(projectService.list(organId, key));
    }

    @ApiOperation(value = "删除项目", notes = "删除项目")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/{projectId}")
    public BaseResult delete(@PathVariable("organId") String organId,
                             @PathVariable("projectId") String projectId) {
        projectService.delete(organId, projectId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新项目", notes = "更新项目")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectDto", value = "项目对象", paramType = "query", dataTypeClass = ProjectDto.class),
    })
    @PutMapping("/{projectId}")
    public BaseResult update(@PathVariable("organId") String organId,
                             @PathVariable("projectId") String projectId,
                             @RequestBody ProjectDto projectDto) {
        projectDto.setOrganId(organId);
        projectDto.setProjectId(projectId);
        projectService.update(projectDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取项目下分区", notes = "获取项目下分区")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "withQuota", value = "配额", paramType = "query", required = false, defaultValue = "false", dataTypeClass = Boolean.class),
    })
    @GetMapping("/{projectId}/namespace")
    public BaseResult<List<Namespace>> getNamespace(@PathVariable("organId") String organId,
                                                    @PathVariable("projectId") String projectId,
                                                    @RequestParam(value = "clusterId", required = false) String clusterId,
                                                    @RequestParam(value = "withQuota", required = false, defaultValue = "false") Boolean withQuota,
                                                    @RequestParam(value = "withMiddleware", required = false, defaultValue = "false") Boolean withMiddleware) {
        return BaseResult.ok(projectService.getNamespace(organId, projectId, clusterId, withQuota, withMiddleware));
    }

    @ApiOperation(value = "获取项目下可分配分区", notes = "获取项目下可分配分区")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
    })
    @Deprecated
    @GetMapping("/namespace/allocatable")
    public BaseResult<List<MiddlewareClusterDTO>> getAllocatableNamespace(@PathVariable("organId") String organId) {
        return BaseResult.ok(projectService.getAllocatableNamespace());
    }

    @ApiOperation(value = "项目绑定分区", notes = "项目绑定分区")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区对象", paramType = "query", dataTypeClass = Namespace.class),
    })
    @PostMapping("/{projectId}/namespace")
    public BaseResult addNamespace(@PathVariable("organId") String organId,
                                   @PathVariable("projectId") String projectId,
                                   @RequestBody Namespace namespace) {
        namespace.setProjectId(projectId);
        namespace.setOrganId(organId);
        projectService.addNamespace(namespace);
        return BaseResult.ok();
    }

    @ApiOperation(value = "项目解绑分区", notes = "项目解绑分区")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区名称", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{projectId}/namespace")
    public BaseResult unBindNamespace(@PathVariable("organId") String organId,
                                      @PathVariable("projectId") String projectId,
                                      @RequestParam("clusterId") String clusterId,
                                      @RequestParam("namespace") String namespace) {
        projectService.unBindNamespace(organId, projectId, clusterId, namespace, true);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取项目下成员", notes = "获取项目下成员")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "allocatable", value = "可分配的", paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping("/{projectId}/user")
    public BaseResult<List<UserDto>> getUser(@PathVariable("organId") String organId,
                                             @PathVariable("projectId") String projectId,
                                             @RequestParam("allocatable") Boolean allocatable) {
        return BaseResult.ok(projectService.getUser(organId, projectId, allocatable));
    }

    @ApiOperation(value = "项目绑定成员", notes = "项目绑定成员")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectDto", value = "项目对象", paramType = "query", dataTypeClass = ProjectDto.class),
    })
    @PostMapping("/{projectId}/user")
    public BaseResult bindUser(@PathVariable("organId") String organId,
                               @PathVariable("projectId") String projectId,
                               @RequestBody ProjectDto projectDto) {
        projectDto.setOrganId(organId);
        projectDto.setProjectId(projectId);
        projectService.bindUser(projectDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新项目下成员角色", notes = "更新项目下成员角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "userDto", value = "用户对象", paramType = "query", dataTypeClass = UserDto.class),
    })
    @PutMapping("/{projectId}/user")
    public BaseResult updateUserRole(@PathVariable("organId") String organId,
                                     @PathVariable("projectId") String projectId,
                                     @RequestBody UserDto userDto) {
        projectService.updateUserRole(organId, projectId, userDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "项目取消绑定成员", notes = "项目取消绑定成员")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{projectId}/user")
    public BaseResult unBindUser(@PathVariable("organId") String organId,
                                 @PathVariable("projectId") String projectId,
                                 @RequestParam("username") String username) {
        projectService.unbindUser(organId, projectId, username);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取项目下中间件资源", notes = "获取项目下中间件资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{projectId}/middleware")
    public BaseResult<List<ProjectMiddlewareResourceInfo>> getMiddlewareResource(@PathVariable("organId") String organId,
                                                                                 @PathVariable("projectId") String projectId) throws Exception {
        return BaseResult.ok(projectService.middlewareResource(organId, projectId));
    }

    @ApiOperation(value = "获取项目下服务数量", notes = "获取项目下服务数量")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/middleware/count")
    public BaseResult<List<ProjectDto>> getMiddlewareCount(@PathVariable("organId") String organId) {
        return BaseResult.ok(projectService.getMiddlewareCount(organId, null));
    }

    @ApiOperation(value = "获取项目关联的集群", notes = "获取项目关联的集群")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{projectId}/clusters")
    public BaseResult<Set<String>> getRelationCluster(@PathVariable("organId") String organId,
                                                      @PathVariable("projectId") String projectId) {
        return BaseResult.ok(projectService.getRelationClusterIds(organId, projectId));
    }

    @ApiOperation(value = "查询用户在指定项目下拥有运维权限的operator", notes = "查询用户在指定项目下拥有运维权限的operator")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/operator")
    public BaseResult<List<MiddlewareInfoDTO>> getMiddlewareOperator(@PathVariable("organId") String organId,
                                                                     @RequestParam("clusterId") String clusterId) {
        // todo
        return BaseResult.ok(middlewareInfoService.listUsersOperator(clusterId));
    }

    @ApiOperation(value = "分配资源", notes = "分配资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectQuota", value = "项目配额", paramType = "query", dataTypeClass = ProjectQuota.class),
    })
    @PostMapping("/{projectId}/quota")
    public BaseResult allocateQuota(@PathVariable("organId") String organId,
                                    @PathVariable("projectId") String projectId,
                                    @RequestBody ProjectQuota projectQuota) {
        projectQuota.setOrganId(organId);
        projectQuota.setProjectId(projectId);
        projectService.allocateQuota(projectQuota);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询项目存储配额", notes = "查询项目存储配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", required = false, paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "detail", value = "是否包含项目配额分配情况", paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping("/{projectId}/storage")
    public BaseResult<List<ResourceQuotaDo>> getStorageQuota(@PathVariable("organId") String organId,
                                                             @PathVariable("projectId") String projectId,
                                                             @RequestParam(value = "clusterId", required = false) String clusterId,
                                                             @RequestParam("detail") Boolean detail) {
        return BaseResult.ok(projectService.getStorageQuota(organId, projectId, clusterId, detail));
    }

    @ApiOperation(value = "移除项目存储配额", notes = "移除项目存储配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "storageId", value = "存储名id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{projectId}/storage/{storageId}")
    public BaseResult removeStorageQuota(@PathVariable("organId") String organId,
                                         @PathVariable("projectId") String projectId,
                                         @PathVariable("storageId") String storageId,
                                         @RequestParam("clusterId") String clusterId) {
        projectService.removeStorageQuota(organId, projectId, storageId, clusterId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询项目cpu memory配额", notes = "查询项目cpu memory配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "detail", value = "是否包含项目配额分配情况", paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping("/{projectId}/cpuMemory")
    public BaseResult<List<ResourceQuotaDo>> getCpuAndMemoryQuota(@PathVariable("organId") String organId,
                                                                  @PathVariable("projectId") String projectId,
                                                                  @RequestParam("detail") Boolean detail) {
        return BaseResult.ok(projectService.getCpuMemoryQuota(organId, projectId, detail));
    }

    @ApiOperation(value = "移除项目cpu memory配额", notes = "移除项目cpu memory配额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{projectId}/cpuMemory")
    public BaseResult removeCpuAndMemoryQuota(@PathVariable("organId") String organId,
                                              @PathVariable("projectId") String projectId,
                                              @RequestParam("clusterId") String clusterId) {
        projectService.removeCpuMemoryQuota(organId, projectId, clusterId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询备份服务器", notes = "查询备份服务器")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", required = false, paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "detail", value = "查询备份服务器使用情况", paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping("/{projectId}/backupServer")
    public BaseResult<List<BackupServerDTO>> listBackupServer(@PathVariable("organId") String organId,
                                                              @PathVariable("projectId") String projectId,
                                                              @RequestParam(value = "clusterId", required = false) String clusterId,
                                                              @RequestParam(value = "detail", defaultValue = "false") Boolean detail,
                                                              @RequestParam(value = "position", required = false, defaultValue = "false") Boolean position) {
        return BaseResult.ok(projectService.getBackupServer(organId, projectId, clusterId, detail, position));
    }

    @ApiOperation(value = "移除备份服务器", notes = "移除备份服务器")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organId", value = "组织id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupServerId", value = "备份服务器id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{projectId}/backupServer/{backupServerId}")
    public BaseResult removeBackupServer(@PathVariable("organId") String organId,
                                         @PathVariable("projectId") String projectId,
                                         @PathVariable("backupServerId") Integer backupServerId,
                                         @RequestParam("clusterId") String clusterId) {
        projectService.removeBackupServer(organId, projectId, backupServerId, clusterId);
        return BaseResult.ok();
    }
}
