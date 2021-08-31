package com.harmonycloud.zeus.controller.user;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.zeus.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.user.UserDto;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @Date 2021/7/22 11:52 上午
 */
@Api(tags = {"系统管理","用户管理"}, value = "用户信息")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @ApiOperation(value = "获取用户信息", notes = "获取用户信息")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "userName", value = "账户", paramType = "query", dataTypeClass = String.class),})
    @GetMapping
    public BaseResult<UserDto> get(@RequestParam(value = "userName", required = false) String userName) throws Exception {
        return BaseResult.ok(userService.get(userName));
    }

    @ApiOperation(value = "获取用户列表", notes = "获取用户列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", value = "过滤字", required = false, paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/list")
    public BaseResult<List<UserDto>> list(@RequestParam(value = "keyword", required = false) String keyword) throws Exception {
        return BaseResult.ok(userService.list(keyword));
    }

    @ApiOperation(value = "创建用户", notes = "创建用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userDto", value = "用户信息", paramType = "query", dataTypeClass = UserDto.class),
    })
    @PostMapping
    public BaseResult create(@RequestBody UserDto userDto) throws Exception {
        userService.create(userDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改用户信息", notes = "修改用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userDto", value = "用户信息", paramType = "query", dataTypeClass = UserDto.class),
            @ApiImplicitParam(name = "userName", value = "用户名", paramType = "path", dataTypeClass = UserDto.class),
    })
    @PutMapping("/{userName}")
    public BaseResult update(@RequestBody UserDto userDto,
                             @PathVariable("userName") String userName) throws Exception {
        userDto.setUserName(userName);
        userService.update(userDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除用户", notes = "删除用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userName", value = "账户", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/{userName}")
    public BaseResult<Boolean> delete(@PathVariable("userName") String userName) {
        return BaseResult.ok(userService.delete(userName));
    }

    @ApiOperation(value = "重制密码", notes = "重制密码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userName", value = "账户", paramType = "path", dataTypeClass = String.class),
    })
    @PostMapping("/{userName}/password/reset")
    public BaseResult<Boolean> reset(@PathVariable("userName") String userName) {
        return BaseResult.ok(userService.reset(userName));
    }

    @ApiOperation(value = "修改密码", notes = "修改密码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userName", value = "账户", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", value = "原密码", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "newPassword", value = "新密码", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "reNewPassword", value = "二次新密码", paramType = "query", dataTypeClass = String.class),
    })
    @PutMapping("/{userName}/password")
    public BaseResult changePassword(@PathVariable("userName") String userName,
                                     @RequestParam("password") String password,
                                     @RequestParam("newPassword") String newPassword,
                                     @RequestParam("reNewPassword") String reNewPassword) throws Exception {
        userService.changePassword(userName, password, newPassword, reNewPassword);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取菜单列表", notes = "获取菜单列表")
    @GetMapping("/menu")
    public BaseResult<List<ResourceMenuDto>> menu(HttpServletRequest request) throws Exception {
        return BaseResult.ok(userService.menu(request));
    }
}
