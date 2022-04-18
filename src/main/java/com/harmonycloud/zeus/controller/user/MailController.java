package com.harmonycloud.zeus.controller.user;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.zeus.bean.MailInfo;
import com.harmonycloud.zeus.service.user.MailService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author yushuaikang
 * @date 2021/11/8 下午3:24
 */
@Slf4j
@Api(tags = {"邮件发送","邮件发送"}, value = "邮件发送")
@RestController
@RequestMapping(value = {"/mail/{ding}","/mail"})
public class MailController {

    @Autowired
    private MailService mailService;

    @ApiOperation(value = "设置邮箱", notes = "设置邮箱")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mailInfo", value = "邮箱信息", paramType = "query", dataTypeClass = MailInfo.class),
    })
    @PostMapping
    public BaseResult creat(@RequestBody MailInfo mailInfo) throws IllegalAccessException {
        mailService.insertMail(mailInfo);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取邮箱信息", notes = "获取邮箱信息")
    @GetMapping("/getMailInfo")
    public BaseResult get() {
        return BaseResult.ok(mailService.select());
    }

    @ApiOperation(value = "邮箱连接测试", notes = "邮箱连接测试")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "邮箱", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", value = "密码", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/connect")
    public BaseResult connect(@RequestParam String email,@RequestParam String password) {
        return BaseResult.ok(mailService.checkEmail(email, password));
    }
}
