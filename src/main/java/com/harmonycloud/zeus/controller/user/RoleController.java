package com.harmonycloud.zeus.controller.user;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.user.RoleDto;
import com.harmonycloud.zeus.bean.user.BeanRole;
import com.harmonycloud.zeus.service.user.RoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/7/28 1:55 下午
 */
@Api(tags = {"系统管理","用户管理"}, value = "角色信息")
@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @ApiOperation(value = "新增角色", notes = "新增角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleDto", value = "角色", paramType = "query", dataTypeClass = String.class)
    })
    @PostMapping
    public BaseResult add(@RequestBody RoleDto roleDto) {
        roleService.add(roleDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取角色列表", notes = "获取角色列表")
    @GetMapping("/list")
    public BaseResult<List<RoleDto>> list(@RequestParam(value = "key", required = false) String key) {
        return BaseResult.ok(roleService.list(key));
    }

    @ApiOperation(value = "删除角色", notes = "删除角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleId", value = "角色id", paramType = "path", dataTypeClass = String.class)
    })
    @DeleteMapping("/{roleId}")
    public BaseResult delete(@PathVariable("roleId") Integer roleId) {
        roleService.delete(roleId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除角色", notes = "删除角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleDto", value = "角色", paramType = "query", dataTypeClass = String.class)
    })
    @PutMapping("/{roleId}")
    public BaseResult update(@PathVariable("roleId") Integer roleId,
                             @RequestBody RoleDto roleDto) {
        roleDto.setId(roleId);
        roleService.update(roleDto);
        return BaseResult.ok();
    }
}
