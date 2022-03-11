package com.harmonycloud.zeus.controller.system;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.LdapConfigDto;
import com.harmonycloud.zeus.service.user.LdapService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author liyinlong
 * @since 2022/3/10 7:44 下午
 */
@Api(tags = {"平台管理", "开放中心"}, value = "平台管理")
@RestController
@RequestMapping("/ldap")
public class LdapController {

    @Autowired
    private LdapService ldapService;

    @ApiOperation(value = "启用ldap", notes = "启用ldap")
    @PostMapping("/enable")
    public BaseResult enable(@RequestBody LdapConfigDto ldapConfigDto) {
        ldapService.save(ldapConfigDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "连接测试", notes = "连接测试")
    @PostMapping("/connectionCheck")
    public BaseResult connectionCheck(@RequestBody LdapConfigDto ldapConfigDto) {
        return BaseResult.ok(ldapService.connectionCheck(ldapConfigDto));
    }

    @ApiOperation(value = "查询Ldap配置信息", notes = "查询Ldap配置信息")
    @GetMapping("/detail")
    public BaseResult detail() {
        return BaseResult.ok(ldapService.queryLdapDetail());
    }

    @ApiOperation(value = "禁用ldap", notes = "禁用ldap")
    @PutMapping("/disable")
    public BaseResult disable(){
        ldapService.disable();
        return BaseResult.ok();
    }


}
