package com.harmonycloud.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.model.middleware.AlertInfoDto;
import com.harmonycloud.zeus.bean.MailInfo;
import com.harmonycloud.zeus.bean.MailToUser;
import com.harmonycloud.zeus.bean.user.BeanUser;
import com.harmonycloud.zeus.dao.MailMapper;
import com.harmonycloud.zeus.dao.MailToUserMapper;
import com.harmonycloud.zeus.service.user.DingRobotService;
import com.harmonycloud.zeus.service.user.MailService;
import com.harmonycloud.zeus.util.RobotClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author yushuaikang
 * @date 2021/11/8 下午3:25
 */
@Service
public class MailServiceImpl implements MailService {

    private static Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);

    @Autowired
    private MailMapper mailMapper;

    @Autowired
    private MailToUserMapper mailToUserMapper;

    @Autowired
    private DingRobotService dingRobotService;

    private static RobotClientUtil robot = new RobotClientUtil();


    /**
     * 邮件发送器
     *
     * @return 配置好的工具
     */
    private JavaMailSenderImpl createMailSender(MailInfo mailInfo) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailInfo.getMailServer());
        sender.setPort(mailInfo.getPort());
        sender.setUsername(mailInfo.getUserName());
        sender.setPassword(mailInfo.getPassword());
        sender.setDefaultEncoding("Utf-8");
        sender.setProtocol("smtp");
        Properties p = new Properties();
        p.setProperty("mail.smtp.timeout", "25000");
        p.setProperty("mail.smtp.auth", "true");
        p.setProperty("mail.smtp.starttls.enable","true");
        p.setProperty("mail.smtp.starttls.required","true");
        p.setProperty("mail.smtp.ssl.enable","true");
        p.setProperty("mail.smtp.socketFactory.port",String.valueOf(mailInfo.getPort()));
        p.setProperty("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
        sender.setJavaMailProperties(p);
        return sender;
    }

    /**
     * 发送邮件
     *
     * @param alertInfoDto 内容
     * @throws MessagingException 异常
     * @throws UnsupportedEncodingException 异常
     */
    @Override
    public void sendHtmlMail(AlertInfoDto alertInfoDto,MailToUser mailToUser) {
        QueryWrapper<MailInfo> wrapper = new QueryWrapper<>();
        MailInfo mailInfo = mailMapper.selectOne(wrapper);
        if (mailInfo == null || alertInfoDto == null) {
            return;
        }
        try {
            JavaMailSenderImpl mailSender = createMailSender(mailInfo);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // 设置utf-8或GBK编码，否则邮件会有乱码
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            String[] path = mailInfo.getMailPath().split("@");
            messageHelper.setFrom(mailInfo.getMailPath(), path[0]);
            messageHelper.setSubject("【中间件平台】"+alertInfoDto.getClusterId()+alertInfoDto.getDescription()+"告警");
            List<MailToUser> users = mailToUserMapper.selectList(new QueryWrapper<MailToUser>());
            messageHelper.setText(buildContent(alertInfoDto,mailToUser.getAliasName()), true);
            messageHelper.setTo(mailToUser.getEmail());
            mailSender.send(mimeMessage);
        }catch (Exception e) {
            logger.error("邮件发送失败:"+e.getMessage());
        }
    }

    @Override
    public void insertMail(MailInfo mailInfo) {
        QueryWrapper<MailInfo> wrapper = new QueryWrapper<>();
        List<MailInfo> list = mailMapper.selectList(wrapper);
        if (list.size() == 0) {
            mailMapper.insert(mailInfo);
        }else {
            mailMapper.update(mailInfo,wrapper);
        }
    }

    @Override
    public void insertUser(List<BeanUser> users, String ding) {
        QueryWrapper<MailToUser> mailToUserQueryWrapper = new QueryWrapper<>();
        List<MailToUser> mailUsers = mailToUserMapper.selectList(mailToUserQueryWrapper);
        List<Integer> list = mailUsers.stream().map(mail -> {
            Integer id = mail.getUserId();
            return id;
        }).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(users)) {
            List<MailToUser> mailToUsers = users.stream().map(user -> {
                MailToUser mailToUser = new MailToUser();
                if (list.contains(user.getId())) {
                    return mailToUser;
                }else {
                    BeanUtils.copyProperties(user,mailToUser);
                    mailToUser.setTime(new Date());
                    mailToUser.setUserId(user.getId());
                    return mailToUser;
                }
            }).collect(Collectors.toList());
            mailToUsers.stream().forEach(user ->{
                if (user.getUserId() != null) {
                    mailToUserMapper.insert(user);
                }
            });
        }
        if (StringUtils.isNotEmpty(ding)) {
            dingRobotService.enableDing();
        }

    }

    @Override
    public boolean checkEmail(String email) {
        return robot.checkEmailMethod(email);
    }

    @Override
    public MailInfo select() {
        return mailMapper.selectOne(new QueryWrapper<MailInfo>());
    }


    private static String buildContent(AlertInfoDto alertInfoDto, String username) throws IOException {
        //加载邮件html模板
        String fileName = "mail-alarm.html";
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(fileName);
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer buffer = new StringBuffer();
        String line = "";
        try {
            while ((line = fileReader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            logger.error("读取文件失败，fileName:{}", fileName, e);
        } finally {
            inputStream.close();
            fileReader.close();
        }

        String emailHeadColor = "";
        String emailTextColor = "";
        String level = "";
        if ("critical".equals(alertInfoDto.getLevel())) { //严重
            emailHeadColor = "red";
            emailTextColor = "<font color='red'>" + "严重" +"</font>";
            level = "严重";
        }
        if ("warning".equals(alertInfoDto.getLevel())) { //重要
            emailHeadColor = "#FFDB94 ";
            emailTextColor = "<font color='#FFDB94 '>" + "重要" +"</font>";
            level = "重要";
        }
        if ("info".equals(alertInfoDto.getLevel())) { //一般
            emailHeadColor = "#94DBFF";
            emailTextColor = "<font color='#94DBFF'>" + "一般" +"</font>";
            level = "一般";
        }

        String contentText = username + ", 以下是告警信息请查收!";
        //邮件表格header
        String header = "<td>告警id</td><td>告警等级</td><td>告警内容</td><td>告警对象</td><td>规则描述</td><td>实际监控</td><td>告警时间<td>";
        StringBuilder linesBuffer = new StringBuilder();
        String date = DateFormatUtils.format(alertInfoDto.getAlertTime(), "yyyy-MM-dd HH:mm:ss");
        linesBuffer.append("<tr><td>" + alertInfoDto.getRuleID() + "</td><td>" + emailTextColor + "</td><td>" + alertInfoDto.getContent() + "</td>" +
                "<td>" + alertInfoDto.getClusterId() + "</td><td>" + alertInfoDto.getDescription() + "</td><td>" + alertInfoDto.getMessage() + "</td><td>" + date + "</td></tr>");

        //填充html模板中的五个参数
        String htmlText = MessageFormat.format(buffer.toString(), emailHeadColor, level, contentText, "", header, linesBuffer.toString());

        //改变表格样式
        htmlText = htmlText.replaceAll("<td>", "<td style=\"padding:6px 10px; line-height: 150%;\">");
        htmlText = htmlText.replaceAll("<tr>", "<tr style=\"border-bottom: 1px solid #eee; color:#666;\">");
        return htmlText;
    }

    /**
     * 发送邮件
     *
     * @param mailMap 收件人与邮件内容集合
     * @throws MessagingException 异常
     */
    /*public static void sendHtmlMail(Map<String, String> mailMap) throws MessagingException{
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        // 设置utf-8编码
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        messageHelper.setFrom(EMAILFORM);
        Iterator<String> iterator = mailMap.keySet().iterator();
        while (iterator.hasNext()) {
            messageHelper.setTo(iterator.next());
            //messageHelper.setSubject(subject);
            messageHelper.setText(mailMap.get(iterator.next()), true);
            mailSender.send(mimeMessage);
        }
    }*/

}
