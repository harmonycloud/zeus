package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.middleware.AlertInfoDto;
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
     * @param mailToUser
     * @throws IOException
     * @throws MessagingException
     */
    void sendHtmlMail(AlertInfoDto alertInfoDto, MailToUser mailToUser) throws IOException, MessagingException;

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
     * 选择被通知人
     * @param users
     * @param ding
     */
    void insertUser(List<BeanUser> users, String ding);

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
     * @param mailToUser
     * @throws MessagingException
     * @throws IOException
     */
    void sendSinaMail(MailInfo mailInfo, AlertInfoDto alertInfoDto, MailToUser mailToUser) throws MessagingException, IOException;
}
