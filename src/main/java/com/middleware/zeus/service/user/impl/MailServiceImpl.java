package com.middleware.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.middleware.AlertInfoDto;
import com.middleware.zeus.bean.MailInfo;
import com.middleware.zeus.bean.user.BeanUser;
import com.middleware.zeus.dao.MailMapper;
import com.middleware.zeus.service.user.MailService;
import com.middleware.zeus.util.RobotClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author yushuaikang
 * @date 2021/11/8 下午3:25
 */
@Service
public class MailServiceImpl implements MailService {

    private static Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);

    @Autowired
    private MailMapper mailMapper;

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
     * @throws IOException 异常
     * @throws MessagingException 异常
     */
    @Override
    public void sendHtmlMail(AlertInfoDto alertInfoDto,BeanUser beanUser) throws IOException, MessagingException {
        QueryWrapper<MailInfo> wrapper = new QueryWrapper<>();
        MailInfo mailInfo = mailMapper.selectOne(wrapper);
        if (ObjectUtils.isEmpty(mailInfo) || ObjectUtils.isEmpty(alertInfoDto)) {
            return;
        }
        if ("qq.com".equals(mailInfo.getUserName().split("@")[1]) || "163.com".equals(mailInfo.getUserName().split("@")[1])) {
            JavaMailSenderImpl mailSender = createMailSender(mailInfo);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // 设置utf-8或GBK编码，否则邮件会有乱码
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            String[] path = mailInfo.getUserName().split("@");
            messageHelper.setFrom(mailInfo.getUserName(), path[0]);
            messageHelper.setSubject("【中间件平台】"+alertInfoDto.getClusterId()+alertInfoDto.getDescription()+"告警");
            messageHelper.setText(buildContent(alertInfoDto,beanUser.getAliasName()), true);
            messageHelper.setTo(beanUser.getEmail());
            mailSender.send(mimeMessage);
            return;
        }
        sendSinaMail(mailInfo,alertInfoDto,beanUser);
    }

    @Override
    public void insertMail(MailInfo mailInfo) throws IllegalAccessException {
        paramsCheck(mailInfo);
        boolean flag = this.checkEmail(mailInfo.getUserName(),mailInfo.getPassword());
        if (!flag) {
            throw new BusinessException(ErrorMessage.MAIL_SERVER_CONNECT_FAILED);
        }
        QueryWrapper<MailInfo> wrapper = new QueryWrapper<>();
        List<MailInfo> list = mailMapper.selectList(wrapper);
        objectToTrim(mailInfo);
        if (list.size() == 0) {
            mailMapper.insert(mailInfo);
        }else {
            mailMapper.update(mailInfo,wrapper);
        }
    }

    @Override
    public boolean checkEmail(String email, String password) {
        boolean result = isValidEmail(email);
        if (!result) {
            return result;
        }
        if ("163.com".equals(email.split("@")[1])) {
            try {
                RobotClientUtil robotClientUtil = new RobotClientUtil(email,password);
                robotClientUtil.checkPassword();
                new Thread(robotClientUtil).start();
                return robotClientUtil.getSuccess();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return robot.checkEmailMethod(email);
    }

    public static boolean isValidEmail(String email) {
        if ((email != null) && (!email.isEmpty())) {
            return Pattern.matches("^(\\w+([-.][A-Za-z0-9]+)*){3,18}@\\w+([-.][A-Za-z0-9]+)*\\.\\w+([-.][A-Za-z0-9]+)*$", email);
        }
        return false;
    }

    @Override
    public void sendSinaMail(MailInfo mailInfo,  AlertInfoDto alertInfoDto, BeanUser beanUser) throws MessagingException, IOException {
        Properties props = new Properties();
        props.setProperty("mail.host", mailInfo.getMailServer());
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        Authenticator authenticator = new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailInfo.getUserName(), mailInfo.getPassword());
            }
        };
        //1 获得连接
        Session session = Session.getDefaultInstance(props, authenticator);
        //2 创建消息
        Message message = new MimeMessage(session);
        // 2.1 发件人
        message.setFrom(new InternetAddress(mailInfo.getUserName()));
        // 2.2 收件人
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(beanUser.getEmail()));
        // 2.3 主题（标题）
        message.setSubject("【中间件平台】"+alertInfoDto.getClusterId()+alertInfoDto.getDescription()+"告警");
        // 2.4 正文
        //设置编码，防止发送的内容中文乱码。
        message.setContent(buildContent(alertInfoDto,beanUser.getAliasName()), "text/html;charset=UTF-8");
        //3发送消息
        Transport.send(message);
    }

    @Override
    public MailInfo select() {
        return mailMapper.selectOne(new QueryWrapper<MailInfo>());
    }


    private static String buildContent(AlertInfoDto alertInfoDto, String username) throws IOException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        //加载邮件html模板
        String fileName = "mail/mail-alarm.html";
        InputStream inputStream = MailServiceImpl.class.getClassLoader().getResourceAsStream(fileName);
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
        String header = "<td>告警等级</td><td>告警内容</td><td>告警对象</td><td>规则描述</td><td>实际监控</td><td>告警时间<td>";
        StringBuilder linesBuffer = new StringBuilder();
        String date = DateFormatUtils.format(alertInfoDto.getAlertTime(), "yyyy-MM-dd HH:mm:ss");
        linesBuffer.append("<tr><td>" + emailTextColor + "</td><td>" + alertInfoDto.getContent() + "</td>" +
                "<td>" + alertInfoDto.getMiddlewareName() + "</td><td>" + alertInfoDto.getDescription() + "</td><td>" + alertInfoDto.getMessage() + "</td><td>" + date + "</td></tr>");

        String href = "";
        String ip = "";
        if (StringUtils.isNotEmpty(alertInfoDto.getIp())) {
            href =  "<a href=\"" + "http://" + alertInfoDto.getIp() + "\">";
            ip = alertInfoDto.getIp() + "</a>";
        }
        //填充html模板中的五个参数
        String htmlText = MessageFormat.format(buffer.toString(), emailHeadColor, level, contentText, "", header, linesBuffer.toString(),href,ip);

        //改变表格样式
        htmlText = htmlText.replaceAll("<td>", "<td style=\"padding:6px 10px; line-height: 150%;\">");
        htmlText = htmlText.replaceAll("<tr>", "<tr style=\"border-bottom: 1px solid #eee; color:#666;\">");
        return htmlText;
    }

    private void paramsCheck(MailInfo mailInfo) {
        if (StringUtils.isAnyBlank(mailInfo.getMailServer(), mailInfo.getPassword(), mailInfo.getUserName(),
            String.valueOf(mailInfo.getPort()))) {
            throw new BusinessException(ErrorMessage.MAIL_INCOMPLETE_PARAMETERS);
        }
    }

    /**
     * 去除两端空格
     * @param obj
     * @throws IllegalAccessException
     */
    public static void objectToTrim(Object obj) throws IllegalAccessException {
        Map<String,String> map = new HashMap<>();
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field field:declaredFields){
            String type = field.getType().getCanonicalName();
            if (StringUtils.equals("java.lang.String", type)){
                field.setAccessible(true);
                Object object = field.get(obj);
                if (object != null) {
                    String trim = object.toString().replace(" ","");
                    map.put(field.getName(),trim);
                }
            }
        }
        for (Field field:declaredFields){
            if (map.get(field.getName())!=null){
                String s = map.get(field.getName());
                field.setAccessible(true);
                field.set(obj,s);
            }
        }

    }
}
