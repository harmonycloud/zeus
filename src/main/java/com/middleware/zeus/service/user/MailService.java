package com.middleware.zeus.service.user;

import com.middleware.zeus.common.model.AlertRecordDo;
import com.middleware.zeus.common.model.AlertUserDo;
import com.middleware.zeus.bean.MailInfo;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

/**
 * @author yushuaikang
 * @date 2021/11/8 下午3:28
 */
public interface MailService {

    /**
     * 使用QQ或者163邮箱发邮件
     * @param alertRecordDo 告警记录
     * @param alertUserDoList 告警用户列表
     */
    void sendHtmlMail(AlertRecordDo alertRecordDo, List<AlertUserDo> alertUserDoList);

    /**
     * 添加邮箱
     * @param mailInfo
     */
    void insertMail(MailInfo mailInfo);

    /**
     * 邮箱信息回显
     * @return
     */
    MailInfo select();

    /**
     * 邮箱连接测试
     * @param mailInfo
     * @return
     */
    void checkEmail(MailInfo mailInfo);
}
