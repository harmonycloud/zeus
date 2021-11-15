package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.SendResult;
import com.harmonycloud.caas.common.model.middleware.AlertInfoDto;
import com.harmonycloud.zeus.bean.DingRobotInfo;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author yushuaikang
 * @date 2021/11/9 下午8:14
 */
public interface DingRobotService {

    SendResult send(AlertInfoDto alertInfoDto) throws IOException;

    SendResult sendWithAt(String message, ArrayList<String> atMobiles);

    SendResult sendWithAtAll(String message);

    void insert(DingRobotInfo dingRobotInfo);
}
