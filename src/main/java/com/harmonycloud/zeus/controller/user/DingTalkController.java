package com.harmonycloud.zeus.controller.user;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.SendResult;
import com.harmonycloud.caas.common.model.middleware.AlertInfoDto;
import com.harmonycloud.zeus.bean.DingRobotInfo;
import com.harmonycloud.zeus.service.user.DingRobotService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @author yushuaikang
 * @date 2021/11/9 下午3:16
 */
@Slf4j
@Api(tags = {"钉钉告警","钉钉告警"}, value = "钉钉告警")
@RestController
@RequestMapping("/ding")
public class DingTalkController {

    @Autowired
    private DingRobotService dingRobotService;

    @ApiOperation(value = "钉钉告警", notes = "钉钉告警")
    @PostMapping("/send")
    public BaseResult send(@RequestBody AlertInfoDto alertInfoDto) throws IOException {
        SendResult result = dingRobotService.send(alertInfoDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "设置钉钉机器人", notes = "设置钉钉机器人")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "dingRobotInfo", value = "钉钉机器人信息", paramType = "query", dataTypeClass = DingRobotInfo.class),
    })
    @PostMapping
    public BaseResult creat(@RequestBody List<DingRobotInfo> dingRobotInfos) {
        dingRobotService.insert(dingRobotInfos);
        return BaseResult.ok();
    }

    @ApiOperation(value = "获取钉钉机器人", notes = "获取钉钉机器人")
    @GetMapping
    public BaseResult dings() {
        return BaseResult.ok(dingRobotService.getDings());
    }

    @ApiOperation(value = "钉钉连接测试", notes = "钉钉连接测试")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "dingRobotInfo", value = "钉钉机器人信息", paramType = "query", dataTypeClass = DingRobotInfo.class),
    })
    @PostMapping("/connect")
    public BaseResult connect(@RequestBody List<DingRobotInfo> dingRobotInfos) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        return BaseResult.ok(dingRobotService.dingConnect(dingRobotInfos));
    }

    @ApiOperation(value = "删除钉钉机器人", notes = "删除钉钉机器人")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "dingRobotInfo", value = "钉钉机器人信息", paramType = "query", dataTypeClass = DingRobotInfo.class),
    })
    @DeleteMapping
    public BaseResult delete(@RequestBody List<DingRobotInfo> dingRobotInfos) {
        dingRobotService.removeDing(dingRobotInfos);
        return BaseResult.ok();
    }
}
