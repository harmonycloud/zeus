package com.middleware.zeus.service.user;

import com.middleware.caas.common.model.DingRobotDTO;
import com.middleware.caas.common.model.SendResult;
import com.middleware.caas.common.model.middleware.AlertInfoDto;
import com.middleware.zeus.bean.DingRobotInfo;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yushuaikang
 * @date 2021/11/9 下午8:14
 */
public interface DingRobotService {

    SendResult send(AlertInfoDto alertInfoDto,DingRobotInfo dingRobotInfo);

    SendResult sendWithAt(String message, ArrayList<String> atMobiles);

    SendResult sendWithAtAll(String message);

    void insert(List<DingRobotInfo> dingRobotInfos);

    List<DingRobotInfo> getDings();

    List<DingRobotDTO> dingConnect(List<DingRobotInfo> dingRobotInfos) throws IOException, NoSuchAlgorithmException, InvalidKeyException;

    void removeDing(List<DingRobotInfo> dingRobotInfos);

    void enableDing();
}
