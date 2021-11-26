package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.middleware.AlertInfoDto;
import com.harmonycloud.zeus.bean.MailInfo;
import com.harmonycloud.zeus.bean.user.BeanUser;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @author yushuaikang
 * @date 2021/11/8 下午3:28
 */
public interface MailService {

    void sendHtmlMail(AlertInfoDto alertInfoDto) throws MessagingException, UnsupportedEncodingException;

    void insertMail(MailInfo mailInfo);

    MailInfo select();

    void insertUser(List<BeanUser> users, String ding);

    boolean checkEmail(String email);
}
