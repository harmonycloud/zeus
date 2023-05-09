package com.middleware.zeus.service.user;

import com.middleware.caas.common.model.AlertRecordDo;
import com.middleware.caas.common.model.AlertUserDo;
import com.middleware.caas.common.model.middleware.AlertInfoDto;
import com.middleware.zeus.bean.MailInfo;
import com.middleware.zeus.bean.user.BeanUser;

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
     * @throws IOException
     * @throws MessagingException
     */
    void sendHtmlMail(AlertRecordDo alertRecordDo, List<AlertUserDo> alertUserDoList) throws IOException, MessagingException;

    /**
     * 添加邮箱
     * @param mailInfo
     */
    void insertMail(MailInfo mailInfo) throws IllegalAccessException;

    /**
     * 邮箱信息回显
     * @return
     */
    MailInfo select();

    /**
     * 邮箱连接测试
     * @param email
     * @param password
     * @return
     */
    boolean checkEmail(String email, String password);

    /**
     * 使用新浪邮箱发送邮件
     * @param mailInfo
     * @param alertRecordDo
     * @param alertUserDo
     * @throws MessagingException
     * @throws IOException
     */
    void sendSinaMail(MailInfo mailInfo, AlertRecordDo alertRecordDo, AlertUserDo alertUserDo) throws MessagingException, IOException;
}
