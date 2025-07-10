package com.middleware.zeus.service.user.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.middleware.zeus.common.base.CurrentLanguage;
import com.middleware.zeus.common.enums.DateType;
import com.middleware.zeus.common.model.user.OrganizationDto;
import com.middleware.zeus.common.model.user.ProjectNamespaceDo;
import com.middleware.zeus.service.user.OrganizationService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.bean.MailInfo;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.AlertRecordDo;
import com.middleware.zeus.common.model.AlertUserDo;
import com.middleware.zeus.dao.MailMapper;
import com.middleware.zeus.service.user.MailService;

import lombok.extern.slf4j.Slf4j;

import static com.middleware.zeus.common.constants.AlertConstant.*;

/**
 * @author yushuaikang
 * @date 2021/11/8 下午3:25
 */
@Service
@Slf4j
public class MailServiceImpl implements MailService {

    @Value("${system.mail.ssl.enable:false}")
    private Boolean mailSSL;

    @Autowired
    private MailMapper mailMapper;

    @Autowired
    private ProjectService projectService;
    @Autowired
    private OrganizationService organizationService;

    @Override
    public void sendHtmlMail(AlertRecordDo alertRecordDo, List<AlertUserDo> alertUserDoList) {
        // 获取邮箱服务器信息
        QueryWrapper<MailInfo> wrapper = new QueryWrapper<>();
        List<MailInfo> mailInfoList = mailMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(mailInfoList)){
            return;
        }
        MailInfo mailInfo = mailInfoList.get(0);

        //1 初始化连接
        JavaMailSenderImpl mailSender = createMailSender(mailInfo);

        for (AlertUserDo alertUserDo : alertUserDoList){

            //2 创建消息
            MimeMessage message = mailSender.createMimeMessage();
            try {
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                // 2.1 发件人
                helper.setFrom(mailInfo.getUserName());
                // 2.2 收件人
                helper.setTo(alertUserDo.getEmail());
                // 2.3 主题（标题）
                //helper.setSubject("【中间件平台】" + alertRecordDo.getClusterId() + alertRecordDo.getTargetAliasName() + "告警");
                //String target = alertRecordDo.getAlertType().equals(SERVICE) ? "中间件" : (alertRecordDo.getAlertType().equals(SYSTEM) ? "系统" : "集群");
                //helper.setSubject(String.format("【%s】Zeus中间件%s告警-%s", alertRecordDo.getLevel(), target, alertRecordDo.getAlertName()));
                helper.setSubject(buildSubject(alertRecordDo.getAlertType(), alertRecordDo.getLevel(), alertRecordDo.getAlertName(), alertRecordDo.getTargetName()));
                // 2.4 正文
                helper.setText(buildContent(alertRecordDo), true);

                mailSender.send(message);
                log.info("邮件发送成功，接收者: {}", alertUserDo.getUsername());
            } catch (Exception e) {
                log.error("发送邮件时发生异常: ", e);
                throw new MessagingException("邮件发送失败");
            }
        }
    }

    @Override
    public void insertMail(MailInfo mailInfo) {
        paramsCheck(mailInfo);
        // 校验邮箱服务器
        checkEmail(mailInfo);
        QueryWrapper<MailInfo> wrapper = new QueryWrapper<>();
        List<MailInfo> list = mailMapper.selectList(wrapper);

        if (list.isEmpty()) {
            mailMapper.insert(mailInfo);
        }else {
            mailMapper.update(mailInfo,wrapper);
        }
    }

    @Override
    public void checkEmail(MailInfo mailInfo) {
        if (StringUtils.isNotEmpty(mailInfo.getUserName()) && !isValidEmail(mailInfo.getUserName())){
            throw new BusinessException(ErrorMessage.MAIL_ADDRESS_INVALID);
        }
        JavaMailSenderImpl mailSender = createMailSender(mailInfo);
        try {
            mailSender.testConnection();
        } catch (Exception e) {
            throw new BusinessException(ErrorMessage.SMTP_SERVER_CONNECT_FAILED);
        }
    }

    public static boolean isValidEmail(String email) {
        if ((email != null) && (!email.isEmpty())) {
            return Pattern.matches("^(\\w+([-.][A-Za-z0-9]+)*){3,18}@\\w+([-.][A-Za-z0-9]+)*\\.\\w+([-.][A-Za-z0-9]+)*$", email);
        }
        return false;
    }

    @Override
    public MailInfo select() {
        return mailMapper.selectOne(new QueryWrapper<MailInfo>());
    }
    

    private void paramsCheck(MailInfo mailInfo) {
        if (StringUtils.isAnyBlank(mailInfo.getMailServer(), mailInfo.getPassword(), mailInfo.getUserName(),
                String.valueOf(mailInfo.getPort()))) {
            throw new BusinessException(ErrorMessage.MAIL_INCOMPLETE_PARAMETERS);
        }
    }

    /**
     * 邮件发送器
     *
     * @return 配置好的工具
     */
    private JavaMailSenderImpl createMailSender(MailInfo mailInfo) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailInfo.getMailServer());
        mailSender.setPort(mailInfo.getPort());
        mailSender.setUsername(mailInfo.getUserName());
        mailSender.setPassword(mailInfo.getPassword());
        mailSender.setDefaultEncoding("UTF-8");
        mailSender.setProtocol(mailSSL ? "smtps" : "smtp");

        Properties props = mailSender.getJavaMailProperties();
        initProps(props, mailInfo);

        return mailSender;
    }

    public void initProps(Properties props, MailInfo mailInfo){
        if (mailSSL){
            props.put("mail.smtps.ssl.enable", "true");
            props.put("mail.smtps.host", mailInfo.getMailServer());
            props.put("mail.smtps.port", mailInfo.getPort());
            props.put("mail.smtps.starttls.enable", "true");
            // 配置信任所有证书
            props.put("mail.smtps.ssl.trust", "*");
            props.put("mail.smtps.connectiontimeout", "7000");
        } else {
            props.put("mail.smtp.host", mailInfo.getMailServer());
            props.put("mail.smtp.port", mailInfo.getPort());
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.ssl.trust", "*");
            props.put("mail.smtp.connectiontimeout", "7000");
        }

        // 创建一个信任所有证书的信任管理器
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        // 启用信任所有证书的信任管理器
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            // 创建一个 SSL socket 工厂，禁用证书验证
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            // 设置 Java Mail API 的 SSL socket 工厂
            props.put("mail.smtp.ssl.socketFactory", socketFactory);
        } catch (Exception e){
            log.error("启用信任所有证书的信任管理器失败", e);
        }
    }

    private String buildSubject(String alertType, String level, String alertName, String middlewareName) {
        String language = CurrentLanguage.getLanguage();
        String subject;
        if ("en-US".equals(language)) {
            String target =
                alertType.equals(SERVICE) ? "Middleware" : (alertType.equals(SYSTEM) ? "System" : "Cluster");
            subject =
                String.format("【%s】Zeus %s Alarm - %s - %s Name: %s", level, target, alertName, target, middlewareName);
        } else {
            level = translateLevel(level);
            String target = alertType.equals(SERVICE) ? "中间件" : (alertType.equals(SYSTEM) ? "系统" : "集群");
            subject = String.format("【%s】Zeus%s告警-%s-%s名称: %s", level, target, alertName,
                target.equals("中间件") ? "服务" : target, middlewareName);
        }
        return subject;
    }


    private String buildContent(AlertRecordDo alertRecordDo) throws IOException {
        String fileName = "mail/mail-alarm.html";
        InputStream inputStream = MailServiceImpl.class.getClassLoader().getResourceAsStream(fileName);
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = fileReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            log.error("读取文件失败，fileName:{}", fileName, e);
        } finally {
            inputStream.close();
            fileReader.close();
        }

        String language = CurrentLanguage.getLanguage();

        String emailHeadColor = "";
        String level = translateLevel(alertRecordDo.getLevel());
        if ("critical".equals(alertRecordDo.getLevel())) {
            emailHeadColor = "red";
        }
        if ("warning".equals(alertRecordDo.getLevel())) {
            emailHeadColor = "#FFDB94 ";
        }
        if ("info".equals(alertRecordDo.getLevel())) {
            emailHeadColor = "#94DBFF";
        }
        

        String contentText;
        switch (language) {
            case "zh-CN":
                contentText = buildZhContent(alertRecordDo);
                break;
            case "en-US":
                contentText = buildEnContent(alertRecordDo);
                break;
            default:
                contentText = buildZhContent(alertRecordDo);
                break;
        }

        String htmlText = MessageFormat.format(sb.toString(), emailHeadColor, level, contentText, "", "", "", "", "");

        // 改变表格样式
        htmlText = htmlText.replaceAll("<td>", "<td style=\"padding:6px 10px; line-height: 150%;\">");
        htmlText = htmlText.replaceAll("<tr>", "<tr style=\"border-bottom: 1px solid #eee; color:#666;\">");
        
        return htmlText;
    }
    
    private String buildZhContent(AlertRecordDo alertRecordDo) {
//        String target = alertRecordDo.getAlertType().equals(SERVICE) ? "中间件"
//            : (alertRecordDo.getAlertType().equals(SYSTEM) ? "系统" : "集群");
        String level = translateLevel(alertRecordDo.getLevel());
        String contentText =  "<p>告警级别：" + level + "</p>" + "<p>告警时间："
            + DateUtil.DateToString(alertRecordDo.getAlertTime(), DateType.YYYY_MM_DD_HH_MM_SS_EN) + "</p>" + "<p>异常指标："
            + alertRecordDo.getAlertName() + "</p>" + "<p>告警描述：</p><p>" + alertRecordDo.getMessage() + "</p><br>"
            + "<p>影响范围：</p>";
        StringBuilder affectArea = new StringBuilder();
        if (alertRecordDo.getAlertType().equals(SERVICE)) {
            affectArea.append("<p>·服务名称：[").append(alertRecordDo.getTargetName()).append("]</p>")
                .append("<p>·集群/命名空间：[").append(alertRecordDo.getClusterId()).append("][")
                .append(alertRecordDo.getNamespace()).append("]</p>");
            // 获取组织项目名称
            List<ProjectNamespaceDo> projectNamespaceDoList =
                projectService.listNamespace(alertRecordDo.getClusterId());
            projectNamespaceDoList = projectNamespaceDoList.stream()
                .filter(projectNamespaceDo -> projectNamespaceDo.getNamespace().equals(alertRecordDo.getNamespace()))
                .collect(Collectors.toList());
            if (!projectNamespaceDoList.isEmpty()) {
                ProjectNamespaceDo projectNamespaceDo = projectNamespaceDoList.get(0);
                OrganizationDto organizationDto = organizationService.get(projectNamespaceDo.getOrganId());

                affectArea.append("<p>·组织/项目：[").append(organizationDto.getName()).append("][")
                    .append(projectNamespaceDo.getProjectName()).append("]</p>");
            }
        } else if (alertRecordDo.getAlertType().equals(CLUSTER) || alertRecordDo.getAlertType().equals(SYSTEM)) {
            affectArea.append("<p>·集群名称：[").append(alertRecordDo.getClusterId()).append("]</p>").append("<p>·组件名称：[")
                .append(alertRecordDo.getTargetAliasName()).append("]</p>");
        }

        contentText = contentText + affectArea + "<br><p>"
            + DateUtil.DateToString(new Date(), DateType.YYYY_MM_DD_HH_MM_SS_EN) + "</p>";

        return contentText;
    }

    private String buildEnContent(AlertRecordDo alertRecordDo) {
        String contentText =
             "<p>Alert Level：" + alertRecordDo.getLevel() + "</p>"
                + "<p>Alert Time：" + DateUtil.DateToString(alertRecordDo.getAlertTime(), DateType.YYYY_MM_DD_HH_MM_SS_EN)
                + "</p>" + "<p>Abnormal Indicator：" + alertRecordDo.getAlertName() + "</p>" + "<p>Alert Description：</p><p>"
                + alertRecordDo.getMessage() + "</p><br>" + "<p>Scope of Impact：</p>";
        StringBuilder affectArea = new StringBuilder();
        if (alertRecordDo.getAlertType().equals(SERVICE)) {
            affectArea.append("<p>·MiddlewareName：[").append(alertRecordDo.getTargetName()).append("]</p>")
                .append("<p>·Cluster/Namespace：[").append(alertRecordDo.getClusterId()).append("][")
                .append(alertRecordDo.getNamespace()).append("]</p>");
            // 获取组织项目名称
            List<ProjectNamespaceDo> projectNamespaceDoList =
                projectService.listNamespace(alertRecordDo.getClusterId());
            projectNamespaceDoList = projectNamespaceDoList.stream()
                .filter(projectNamespaceDo -> projectNamespaceDo.getNamespace().equals(alertRecordDo.getNamespace()))
                .collect(Collectors.toList());
            if (!projectNamespaceDoList.isEmpty()) {
                ProjectNamespaceDo projectNamespaceDo = projectNamespaceDoList.get(0);
                OrganizationDto organizationDto = organizationService.get(projectNamespaceDo.getOrganId());

                affectArea.append("<p>·Organization/Project：[").append(organizationDto.getName()).append("][")
                    .append(projectNamespaceDo.getProjectName()).append("]</p>");
            }
        } else if (alertRecordDo.getAlertType().equals(CLUSTER) || alertRecordDo.getAlertType().equals(SYSTEM)) {
            affectArea.append("<p>·Cluster：[").append(alertRecordDo.getClusterId()).append("]</p>").append("<p>·Components：[")
                .append(alertRecordDo.getTargetAliasName()).append("]</p>");
        }

        contentText = contentText + affectArea + "<br>"
            + DateUtil.DateToString(new Date(), DateType.YYYY_MM_DD_HH_MM_SS_EN) + "</p>";
        return contentText;
    }

    private String translateLevel(String level) {
        String language = CurrentLanguage.getLanguage();
        if (language.equals("zh-CN")) {
            switch (level) {
                case "critical":
                    return "重要";
                case "major":
                    return "次要";
                case "info":
                    return "一般>";
            }
        }
        return level;
    }
}
