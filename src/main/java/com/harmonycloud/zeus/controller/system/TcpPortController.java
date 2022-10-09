package com.harmonycloud.zeus.controller.system;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.LdapConfigDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * @author xutianhong
 * @Date 2022/9/26 11:34 上午
 */
@Api(tags = {"系统管理", "端口范围"}, value = "端口范围")
@RestController
@RequestMapping("/port")
public class TcpPortController {

    @Value("${server.ingressTcp.start: 30000}")
    private Integer ingressTcpStart;
    @Value("${server.ingressTcp.end: 65535}")
    private Integer ingressTcpEnd;
    @Value("${server.nodePort.start: 30000}")
    private Integer nodePortStart;
    @Value("${server.nodePort.end: 32767}")
    private Integer nodePortEnd;


    @ApiOperation(value = "启用ldap", notes = "启用ldap")
    @GetMapping("/ingressTcp")
    public BaseResult<String> ingressTcp() {
        String port = String.format("%s-%s", ingressTcpStart, ingressTcpEnd);
        return BaseResult.ok(port);
    }

    @ApiOperation(value = "启用ldap", notes = "启用ldap")
    @GetMapping("/nodePort")
    public BaseResult<String> nodePort() {
        String port = String.format("%s-%s", nodePortStart, nodePortEnd);
        return BaseResult.ok(port);
    }

}
