package com.harmonycloud.zeus.controller.user;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MiddlewareResourceInfo;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.user.ProjectDto;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.zeus.service.user.ProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/3/23 5:21 下午
 */
@Slf4j
@Api(tags = {"系统管理","项目管理"}, value = "项目", description = "项目")
@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @ApiOperation(value = "创建项目", notes = "创建项目")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectDto", value = "项目对象", paramType = "query", dataTypeClass = ProjectDto.class),
    })
    @PostMapping
    public BaseResult create(@RequestBody ProjectDto projectDto) {
        projectService.add(projectDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取项目列表", notes = "获取项目列表")
    @ApiImplicitParams({
    })
    @GetMapping
    public BaseResult<List<ProjectDto>> list() {
        return BaseResult.ok(projectService.list());
    }

    @ApiOperation(value = "删除项目", notes = "删除项目")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/{projectId}")
    public BaseResult delete(@PathVariable("projectId") String projectId) {
        projectService.delete(projectId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新项目", notes = "更新项目")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectDto", value = "项目对象", paramType = "query", dataTypeClass = ProjectDto.class),
    })
    @PutMapping("/{projectId}")
    public BaseResult update(@PathVariable("projectId") String projectId,
                             @RequestBody ProjectDto projectDto) {
        projectDto.setProjectId(projectId);
        projectService.update(projectDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取项目下分区", notes = "获取项目下分区")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{projectId}/namespace")
    public BaseResult<List<Namespace>> getNamespace(@PathVariable("projectId") String projectId) {
        return BaseResult.ok(projectService.getNamespace(projectId));
    }

    @ApiOperation(value = "项目绑定分区", notes = "项目绑定分区")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区对象", paramType = "query", dataTypeClass = Namespace.class),
    })
    @PostMapping("/{projectId}/namespace")
    public BaseResult bindNamespace(@PathVariable("projectId") String projectId,
                                    @RequestBody Namespace namespace) {
        namespace.setProjectId(projectId);
        projectService.bindNamespace(namespace);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取项目下成员", notes = "获取项目下成员")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{projectId}/user")
    public BaseResult<List<UserDto>> getUser(@PathVariable("projectId") String projectId) {
        return BaseResult.ok(projectService.getUser(projectId));
    }

    @ApiOperation(value = "项目绑定成员", notes = "项目绑定成员")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectDto", value = "项目对象", paramType = "query", dataTypeClass = ProjectDto.class),
    })
    @PostMapping("/{projectId}/user")
    public BaseResult bindUser(@PathVariable("projectId") String projectId,
                               @RequestBody ProjectDto projectDto) {
        projectDto.setProjectId(projectId);
        projectService.bindUser(projectDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "项目绑定成员", notes = "项目绑定成员")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "userDto", value = "用户对象", paramType = "query", dataTypeClass = UserDto.class),
    })
    @PutMapping("/{projectId}/user")
    public BaseResult updateUserRole(@PathVariable("projectId") String projectId,
                                     @RequestBody UserDto userDto) {
        projectService.updateUserRole(projectId, userDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "项目取消绑定成员", notes = "项目取消绑定成员")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping("/{projectId}/user")
    public BaseResult unBindUser(@PathVariable("projectId") String projectId,
                                 @RequestParam("username") String username) {
        projectService.unbindUser(projectId, username);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取项目下中间件资源", notes = "获取项目下中间件资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{projectId}/middleware")
    public BaseResult<Map<String, List<MiddlewareResourceInfo>>> getMiddlewareResource(@PathVariable("projectId") String projectId) throws Exception {
        return BaseResult.ok(projectService.middlewareResource(projectId));
    }


}
