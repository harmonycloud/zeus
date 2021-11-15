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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

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
    public BaseResult creat(@RequestBody DingRobotInfo dingRobotInfo) {
        dingRobotService.insert(dingRobotInfo);
        return BaseResult.ok();
    }
}
