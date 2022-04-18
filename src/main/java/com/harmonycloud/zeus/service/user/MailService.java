package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.middleware.AlertInfoDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareAlertsDTO;
import com.harmonycloud.zeus.bean.MailInfo;
import com.harmonycloud.zeus.bean.MailToUser;
import com.harmonycloud.zeus.bean.user.BeanUser;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @author yushuaikang
 * @date 2021/11/8 下午3:28
 */
public interface MailService {

    /**
     * 使用QQ或者163邮箱发邮件
     * @param alertInfoDto
     * @param beanUser
     * @throws IOException
     * @throws MessagingException
     */
    void sendHtmlMail(AlertInfoDto alertInfoDto, BeanUser beanUser) throws IOException, MessagingException;

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
     * @param alertInfoDto
     * @param beanUser
     * @throws MessagingException
     * @throws IOException
     */
    void sendSinaMail(MailInfo mailInfo, AlertInfoDto alertInfoDto, BeanUser beanUser) throws MessagingException, IOException;
}
