package com.harmonycloud.zeus.util;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.SendResult;
import com.harmonycloud.caas.common.model.TextMessage;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.IOException;

/**
 * @author yushuaikang
 * @date 2021/11/9 下午2:58
 */
public class RobotClientUtil {
    private static Logger logger = LoggerFactory.getLogger(RobotClientUtil.class);

    public static final String SENDER_EMAIL = "no-reply@domain.com";
    public static final String SENDER_EMAIL_SERVER = SENDER_EMAIL.split("@")[1];


    public SendResult send(String webhook, TextMessage message) throws IOException {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(webhook);
        httppost.addHeader("Content-Type", "application/json; charset=utf-8");
        StringEntity se = new StringEntity(message.toJsonString(), "utf-8");
        httppost.setEntity(se);
        SendResult sendResult = new SendResult();
        try {
            HttpResponse response = httpclient.execute(httppost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String result = EntityUtils.toString(response.getEntity());
                JSONObject obj = JSONObject.parseObject(result);
                Integer errcode = obj.getInteger("errcode");
                sendResult.setErrorCode(errcode);
                sendResult.setErrorMsg(obj.getString("errmsg"));
                sendResult.setIsSuccess(errcode.equals(0));
            }
        } catch (Exception e) {
            sendResult.setIsSuccess(false);
            return sendResult;
        }
        return sendResult;
    }



    public boolean checkEmailMethod(String email) {
        if (!email.matches("[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+")) {
            logger.error("Format error");
            return false;
        }

        String log = "";
        String host = "";
        String hostName = email.split("@")[1];
        Record[] result = null;
        SMTPClient client = new SMTPClient();

        try {
            // 查找MX记录
            Lookup lookup = new Lookup(hostName, Type.MX);
            lookup.run();
            if (lookup.getResult() != Lookup.SUCCESSFUL) {
                log += "找不到MX记录\n";
                return false;
            } else {
                result = lookup.getAnswers();
            }

            // 连接到邮箱服务器
            for (int i = 0; i < result.length; i++) {
                host = result[i].getAdditionalName().toString();
                client.connect(host);
                if (!SMTPReply.isPositiveCompletion(client.getReplyCode())) {
                    client.disconnect();
                    continue;
                } else {
                    log += "MX record about " + hostName + " exists.\n";
                    log += "Connection succeeded to " + host + "\n";
                    break;
                }
            }
            log += client.getReplyString();

            client.login(SENDER_EMAIL);
            log += "=" + client.getReplyString();

            client.setSender(SENDER_EMAIL_SERVER);
            log += "=" + client.getReplyString();

            client.addRecipient(email);
            log += "=" + client.getReplyString();

            if (250 == client.getReplyCode()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.error(log);
        }
        return false;
    }
}
