package com.harmonycloud.zeus.controller.user;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.BaseResult;
import com.harmonycloud.zeus.service.user.AuthService;
import com.middleware.tool.encrypt.RSAUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Api(tags = {"系统管理","用户管理"}, value = "授权认证")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @ApiOperation(value = "登录", notes = "登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userName", value = "用户名", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", value = "密码", paramType = "query", dataTypeClass = String.class)
    })
    @PostMapping("/login")
    public BaseResult<JSONObject> login(@RequestParam("userName") String userName,
                                        @RequestParam("password") String password,
                                        HttpServletResponse response) throws Exception {
        return BaseResult.ok(authService.login(userName, password, response));
    }

    @ApiOperation(value = "登出", notes = "登出")
    @PostMapping("/logout")
    public BaseResult<String> logout(HttpServletRequest request,
                                     HttpServletResponse response) {
        return BaseResult.ok(authService.logout(request, response));
    }

    @ApiOperation(value = "rsa公钥", notes = "rsa公钥")
    @GetMapping("/rsa/public")
    public BaseResult<String> rsa() {
        return BaseResult.ok(RSAUtils.PUBLIC_KEY);
    }

}
