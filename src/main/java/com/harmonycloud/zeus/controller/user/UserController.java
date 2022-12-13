package com.harmonycloud.zeus.controller.user;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.pagehelper.PageInfo;
import com.middleware.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.zeus.bean.PersonalizedConfiguration;
import com.harmonycloud.zeus.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.user.UserDto;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.ASCEND;

/**
 * @author xutianhong
 * @Date 2021/7/22 11:52 上午
 */
@Slf4j
@Api(tags = {"系统管理","用户管理"}, value = "用户信息")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Value("${system.usercenter:zeus}")
    private String userCenter;

    @ApiOperation(value = "获取用户信息", notes = "获取用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userName", value = "账户", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    public BaseResult<UserDto> get(@RequestParam(value = "userName", required = false) String userName,
                                   @RequestParam(value = "projectId", required = false) String projectId) {
        return BaseResult.ok(userService.getUserDto(userName, projectId));
    }

    @ApiOperation(value = "获取用户列表", notes = "获取用户列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", value = "过滤字", required = false, paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "当前页", required = false, paramType = "query", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "size", value = "每页记录数", required = false, paramType = "query", dataTypeClass = Long.class),
    })
    @GetMapping("/list")
    public BaseResult<PageInfo<UserDto>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "current", required = false) Integer current,
                                              @RequestParam(value = "size", required = false) Integer size,
                                              @RequestParam(value = "order", required = false) String order) {
        List<UserDto> userDtoList = userService.list(keyword);
        // 排序
        sort(userDtoList, order);
        if (current != null && size != null){
            return BaseResult.ok(convertPage(userDtoList, current, size));
        }
        return BaseResult.ok(userDtoList);
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

    @ApiOperation(value = "重置密码", notes = "重置密码")
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
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "query", dataTypeClass = String.class),
    })
    public BaseResult<List<ResourceMenuDto>> menu(@RequestParam(value = "projectId", required = false) String projectId) throws Exception {
        log.info("获取菜单列表：{}", projectId);
        return BaseResult.ok(userService.menu(projectId));
    }

    @ApiOperation(value = "获取服务列表", notes = "获取服务列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/menu/middlewares")
    public BaseResult<List<ResourceMenuDto>> listMiddlewareMenu(@RequestParam("clusterId") String clusterId,
                                                                @RequestParam("projectId") String projectId) {
        return BaseResult.ok(userService.listMiddlewareMenu(clusterId, projectId));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "personalizedConfiguration", value = "个性化配置信息", paramType = "query", dataTypeClass = PersonalizedConfiguration.class),
            @ApiImplicitParam(name = "status", value = "恢复初始化设置",paramType = "path", dataTypeClass = String.class),
    })
    @ApiOperation(value = "添加个性化配置", notes = "添加个性化配置")
    @PostMapping("/personalized")
    public BaseResult Personalized(@RequestBody PersonalizedConfiguration personalizedConfiguration,
                                   @RequestParam String status) throws Exception {
        userService.insertPersonalConfig(personalizedConfiguration,status);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取个性化配置", notes = "获取个性化配置")
    @GetMapping("/getPersonalConfig")
    public BaseResult getPersonalConfig() throws IOException {
        return BaseResult.ok(userService.getPersonalConfig());
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", value = "图片",paramType = "form",dataTypeClass = File.class),
    })
    @ApiOperation(value = "上传图片", notes = "上传图片")
    @ResponseBody
    @PostMapping("/uploadFile")
    public BaseResult UploadFile(@RequestPart("file") MultipartFile file) throws IOException {
        return BaseResult.ok(userService.uploadFile(file));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "alertRuleId", value = "规则ID", paramType = "query", dataTypeClass = String.class),
    })
    @ApiOperation(value = "获取登录用户列表及通知人列表", notes = "获取登录用户列表及通知人列表")
    @GetMapping("/users")
    public BaseResult getUsers(@RequestParam(value = "alertRuleId", required = false) String alertRuleId) {
        return BaseResult.ok(userService.getUserList(alertRuleId));
    }

    @ApiOperation(value = "切换项目", notes = "切换项目")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "项目id", paramType = "query", dataTypeClass = String.class),})
    @GetMapping("/switchProject")
    public BaseResult<String> switchProject(@RequestParam(value = "projectId") String projectId,
                                            HttpServletResponse response) {
        userService.switchProject(projectId, response);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询是否接入观云台", notes = "查询是否接入观云台")
    @GetMapping("/useOpenUserCenter")
    public BaseResult<Boolean> userCenter() {
        return BaseResult.ok(userCenter.contains("skyview2"));
    }

    /**
     * 用户列表排序
     */
    public void sort(List<UserDto> userDtoList, String order) {
        if (StringUtils.isNotEmpty(order)) {
            userDtoList.sort((o1, o2) -> o1.getCreateTime() == null && o2.getCreateTime() == null ? 0
                : o1.getCreateTime() == null ? 1
                    : o2.getCreateTime() == null ? -1
                        : ASCEND.equals(order) ? o1.getCreateTime().compareTo(o2.getCreateTime())
                            : o2.getCreateTime().compareTo(o1.getCreateTime()));
        }
    }

    /**
     * 用户列表分页
     */
    public PageInfo<UserDto> convertPage(List<UserDto> userDtoList, Integer current, Integer size){
        List<UserDto> res = new ArrayList<>();
        for (int i = (current - 1) * size ; i < userDtoList.size() && i < current * size; ++i) {
            res.add(userDtoList.get(i));
        }
        PageInfo<UserDto> userDtoPageInfo = new PageInfo<>(res);
        userDtoPageInfo.setPageNum(current);
        userDtoPageInfo.setTotal(userDtoList.size());
        userDtoPageInfo.setSize(res.size());
        userDtoPageInfo.setPageSize(size);
        userDtoPageInfo.setPrePage(current - 1);
        userDtoPageInfo.setNextPage(userDtoPageInfo.getList().size() == size ?  current + 1 : 0);
        double endPageNum = Math.ceil((double) userDtoList.size() / size);
        int[] pageNum = new int[(int)endPageNum];
        for (int i = 1; i <= endPageNum; ++i){
            pageNum[i - 1] = i;
        }
        userDtoPageInfo.setNavigatepageNums(pageNum);

        return userDtoPageInfo;
    }

}
