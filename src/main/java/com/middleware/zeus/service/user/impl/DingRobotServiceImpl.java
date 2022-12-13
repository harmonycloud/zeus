package com.middleware.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.constants.CommonConstant;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.DingRobotDTO;
import com.middleware.caas.common.model.SendResult;
import com.middleware.caas.common.model.TextMessage;
import com.middleware.caas.common.model.middleware.AlertInfoDto;
import com.middleware.zeus.bean.DingRobotInfo;
import com.middleware.zeus.dao.DingRobotMapper;
import com.middleware.zeus.service.user.DingRobotService;
import com.middleware.zeus.util.RobotClientUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    public SendResult send(AlertInfoDto alertInfoDto,DingRobotInfo dingRobotInfo) {
        SendResult sendResult = null;
        if (ObjectUtils.isEmpty(dingRobotInfo) || ObjectUtils.isEmpty(alertInfoDto) ) {
            return sendResult;
        }
        try {
            TextMessage textMessage = new TextMessage(buildContent(alertInfoDto));
            if ("0".equals(dingRobotInfo.getEnable())) {
                return sendResult;
            }
            if (StringUtils.isEmpty(dingRobotInfo.getSecretKey())) {
                sendResult = robot.send(dingRobotInfo.getWebhook(), textMessage);
            }else {
                sendResult = robot.send(secret(dingRobotInfo.getWebhook(),dingRobotInfo.getSecretKey()),textMessage);
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
    public String secret(String webhook, String secret) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
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
        return dingRobotMapper.selectList(wrapper);
    }

    @Override
    public void insert(List<DingRobotInfo> dingRobotInfos) {
        List<DingRobotDTO> dingRobotDTOS = this.dingConnect(dingRobotInfos);
        dingRobotDTOS.forEach(dingRobotDTO -> {
            if (!dingRobotDTO.isSuccess()) {
                throw new BusinessException(ErrorMessage.DING_SERVER_CONNECT_FAILED);
            }
        });
        QueryWrapper<DingRobotInfo> wrapper = new QueryWrapper<>();
        List<DingRobotInfo> list = dingRobotMapper.selectList(wrapper);
        if (list.size() == 0) {
            dingRobotInfos.forEach(dingRobotInfo -> {
                dingRobotInfo.setTime(new Date());
                dingRobotInfo.setEnable("1");
                dingRobotMapper.insert(dingRobotInfo);
            });
        }else {
            dingRobotMapper.delete(wrapper);
            dingRobotInfos.forEach(dingRobotInfo -> {
                dingRobotInfo.setTime(new Date());
                dingRobotInfo.setEnable("1");
                dingRobotMapper.insert(dingRobotInfo);
            });
        }
    }

    @Override
    public List<DingRobotInfo> getDings() {
        QueryWrapper<DingRobotInfo> wrapper = new QueryWrapper<>();
        return dingRobotMapper.selectList(wrapper);
    }

    @Override
    public List<DingRobotDTO> dingConnect(List<DingRobotInfo> dingRobotInfos) {
        List<String> collect = dingRobotInfos.stream().map(DingRobotInfo::getWebhook).distinct().collect(Collectors.toList());
        if (collect.size() < dingRobotInfos.size()) {
            throw new BusinessException(ErrorMessage.WEB_HOOK_REPETITION);
        }
        TextMessage textMessage = new TextMessage("告警连接测试,请忽略!");
        List<DingRobotDTO> dingRobotDTOS = new ArrayList<>();
        dingRobotInfos.forEach(dingRobotInfo -> {
            SendResult sendResult = null;
            DingRobotDTO dingRobotDTO = new DingRobotDTO();
            if (StringUtils.isEmpty(dingRobotInfo.getSecretKey())) {
                try {
                    sendResult = robot.send(dingRobotInfo.getWebhook(), textMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                try {
                    sendResult = robot.send(secret(dingRobotInfo.getWebhook(),dingRobotInfo.getSecretKey()),textMessage);
                } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
                    e.printStackTrace();
                }
            }
            BeanUtils.copyProperties(dingRobotInfo,dingRobotDTO);
            dingRobotDTO.setSuccess(sendResult.isSuccess());
            dingRobotDTOS.add(dingRobotDTO);
        });
        return dingRobotDTOS;
    }

    @Override
    public void removeDing(List<DingRobotInfo> dingRobotInfos) {
        dingRobotInfos.forEach(dingRobotInfo -> {
            dingRobotMapper.deleteById(dingRobotInfo.getId());
        });
    }

    @Override
    public void enableDing() {
        QueryWrapper<DingRobotInfo> wrapper = new QueryWrapper<>();
        List<DingRobotInfo> infos = dingRobotMapper.selectList(wrapper);
        for (DingRobotInfo info : infos) {
            DingRobotInfo dingRobotInfo = new DingRobotInfo();
            dingRobotInfo.setId(info.getId());
            dingRobotInfo.setWebhook(info.getWebhook());
            dingRobotInfo.setSecretKey(info.getSecretKey());
            dingRobotInfo.setTime(info.getTime());
            dingRobotInfo.setEnable("1");
            dingRobotMapper.updateById(dingRobotInfo);
        }
    }

    /**
     * 定义一个消息模板
     * @param alertInfoDto
     * @return
     */
    private String buildContent(AlertInfoDto alertInfoDto) {
        String level = "";
        switch (alertInfoDto.getLevel()) {
            case CommonConstant.INFO:
                level = "一般";
                break;
            case CommonConstant.WARNING:
                level = "重要";
                break;
            case CommonConstant.CRITICAL:
                level = "严重";
                break;
            default:

        }
        String time = DateFormatUtils.format(alertInfoDto.getAlertTime(), "yyyy-MM-dd HH:mm:ss");
        StringBuffer sb = new StringBuffer();
        sb.append("告警等级: ").append(level).append(NEWLINE);
        sb.append("告警内容: ").append(alertInfoDto.getContent()).append(NEWLINE);
        sb.append("告警对象: ").append(alertInfoDto.getMiddlewareName()).append(NEWLINE);
        sb.append("规则描述: ").append(alertInfoDto.getDescription()).append(NEWLINE);
        sb.append("实际监测: ").append(alertInfoDto.getMessage()).append(NEWLINE);
        sb.append("告警时间: ").append(time);

        return sb.toString();
    }
}
