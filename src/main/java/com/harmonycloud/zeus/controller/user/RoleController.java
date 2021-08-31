package com.harmonycloud.zeus.controller.user;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.zeus.bean.user.BeanRole;
import com.harmonycloud.zeus.service.user.RoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @ApiOperation(value = "获取角色列表", notes = "获取角色列表")
    @GetMapping("/list")
    public BaseResult<List<BeanRole>> list() {
        return BaseResult.ok(roleService.list(false));
    }

}
