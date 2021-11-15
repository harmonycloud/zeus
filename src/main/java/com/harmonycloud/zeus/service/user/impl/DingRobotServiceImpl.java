package com.harmonycloud.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.model.SendResult;
import com.harmonycloud.caas.common.model.TextMessage;
import com.harmonycloud.caas.common.model.middleware.AlertInfoDto;
import com.harmonycloud.zeus.bean.DingRobotInfo;
import com.harmonycloud.zeus.dao.DingRobotMapper;
import com.harmonycloud.zeus.service.user.DingRobotService;
import com.harmonycloud.zeus.util.RobotClientUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yushuaikang
 * @date 2021/11/9 下午8:14
 */
@Service
public class DingRobotServiceImpl implements DingRobotService {

    private static Logger logger = LoggerFactory.getLogger(DingRobotServiceImpl.class);

    private static final String NEWLINE = "\n";


    @Autowired
    private DingRobotMapper dingRobotMapper;

    private static RobotClientUtil robot = new RobotClientUtil();

    /**
     * 钉钉发送器
     * 发送普通文本消息
     * @param alertInfoDto
     * @return
     */
    @Override
    public SendResult send(AlertInfoDto alertInfoDto) throws IOException {
        SendResult sendResult = null;
        List<DingRobotInfo> dings = select();
        if (CollectionUtils.isEmpty(dings) || alertInfoDto == null) {
            return sendResult;
        }
        TextMessage textMessage = new TextMessage(buildContent(alertInfoDto));
        try {
            for (DingRobotInfo ding : dings) {
                if (StringUtils.isEmpty(ding.getSecretKey())) {
                    sendResult = robot.send(ding.getWebhook(), textMessage);
                }else {
                    sendResult = robot.send(secret(ding.getWebhook(),ding.getSecretKey()),textMessage);
                }
            }
        }catch (Exception e) {
            logger.error("钉钉发送失败:",sendResult);
            e.printStackTrace();
        }
        return sendResult;
    }

    /**
     * 发送文本消息 可以@部分人
     *
     * @param message
     * @param atMobiles 要@人的电话号码 ArrayList<String>
     * @return
     */
    @Override
    public SendResult sendWithAt(String message, ArrayList<String> atMobiles) {
        TextMessage textMessage = new TextMessage(message);
        SendResult sendResult = null;
        textMessage.setAtMobiles(atMobiles);
        List<DingRobotInfo> dings = select();
        try {
            for (DingRobotInfo ding : dings) {
                if (StringUtils.isEmpty(ding.getSecretKey())) {
                    sendResult = robot.send(ding.getWebhook(), textMessage);
                }else {
                    sendResult = robot.send(secret(ding.getWebhook(),ding.getSecretKey()),textMessage);
                }
                if (sendResult.getErrorCode() != 0) {
                    continue;
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return sendResult;
    }

    /**
     * 发送文本消息 并@所有人
     *
     * @param message
     * @return
     */
    @Override
    public SendResult sendWithAtAll(String message) {
        TextMessage textMessage = new TextMessage(message);
        SendResult sendResult = null;
        textMessage.setIsAtAll(false);
        List<DingRobotInfo> dings = select();
        try {
            for (DingRobotInfo ding : dings) {
                if (StringUtils.isEmpty(ding.getSecretKey())) {
                    sendResult = robot.send(ding.getWebhook(), textMessage);
                }else {
                    sendResult = robot.send(secret(ding.getWebhook(),ding.getSecretKey()),textMessage);
                }
                if (sendResult.getErrorCode() != 0) {
                    continue;
                }
            }
        } catch (Exception e) {
//            log.error("===> send robot message atAll error:", sendResult);
        }
        return sendResult;
    }

    /**
     * 签名计算
     */
    private String secret(String webhook, String secret) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        Long timestamp = System.currentTimeMillis();

        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
        String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)),"UTF-8");
        String webhookToken = webhook + "&timestamp=" + timestamp + "&sign=" + sign;
        return webhookToken;
    }

    private List<DingRobotInfo> select() {
        QueryWrapper<DingRobotInfo> wrapper = new QueryWrapper<>();
        List<DingRobotInfo> dings = dingRobotMapper.selectList(wrapper);
        return dings;
    }

    @Override
    public void insert(DingRobotInfo dingRobotInfo) {
        QueryWrapper<DingRobotInfo> wrapper = new QueryWrapper<>();
        List<DingRobotInfo> list = dingRobotMapper.selectList(wrapper);
        if (list.size() == 0) {
            dingRobotMapper.insert(dingRobotInfo);
        }else {
            dingRobotMapper.delete(wrapper);
            dingRobotMapper.insert(dingRobotInfo);
        }
    }

    /**
     * 定义一个消息模板
     * @param alertInfoDto
     * @return
     */
    private String buildContent(AlertInfoDto alertInfoDto) {
        String date = DateFormatUtils.format(alertInfoDto.getCreateTime(), "yyyy-MM-dd HH:mm:ss");
        StringBuffer sb = new StringBuffer();
        sb.append("告警ID: " + alertInfoDto.getRuleID()).append(NEWLINE);
        sb.append("告警等级: " + alertInfoDto.getLevel()).append(NEWLINE);
        sb.append("告警内容: " + alertInfoDto.getAlert()).append(NEWLINE);
        sb.append("告警对象: " + alertInfoDto.getAlertObject()).append(NEWLINE);
        sb.append("规则描述: " + alertInfoDto.getExpr()).append(NEWLINE);
        sb.append("实际监测: " + alertInfoDto.getExpr()).append(NEWLINE);
        sb.append("告警时间: " + date).append(NEWLINE);

        return sb.toString();
    }
}
