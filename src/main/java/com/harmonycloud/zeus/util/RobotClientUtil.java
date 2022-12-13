package com.harmonycloud.zeus.util;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.model.SendResult;
import com.middleware.caas.common.model.TextMessage;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.xmlbeans.impl.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author yushuaikang
 * @date 2021/11/9 下午2:58
 */
public class RobotClientUtil implements Runnable {
    private String userName;
    private String password;
    private String success = "false";

    public RobotClientUtil() {

    }

    public RobotClientUtil(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public static final String SENDER_EMAIL = "no-reply@domain.com";
    public static final String SENDER_EMAIL_SERVER = "domain.com";

    private static Logger logger = LoggerFactory.getLogger(RobotClientUtil.class);

    private final static String host = "contacts.163.com";
    private final static int port = 443;
    private final static int TIMEOUT = 3;
    private final static int repeat = 3;
    private final static String path = "/.well-known/carddav";
    private final static SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
    private final static String xmlData = "<?xml version=\"1.0\"?> <d:propfind xmlns:d=\"DAV:\" xmlns:cs=\"http://calendarserver.org/ns/\"> <d:prop> <d:displayname /> <d:getetag /> </d:prop> </d:propfind>";
    private final CountDownLatch runCount = new CountDownLatch(3);


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
            logger.error("钉钉连接失败:", e);
            sendResult.setIsSuccess(false);
            return sendResult;
        }
        return sendResult;
    }



    public boolean checkEmailMethod(String email) {
        if (!email.matches("[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+")) {
            return false;
        }
        String host = "";
        String hostName = email.split("@")[1];
        Record[] result = null;
        SMTPClient client = new SMTPClient();
        try {
            // 查找MX记录
            Lookup lookup = new Lookup(hostName, Type.MX);
            lookup.run();
            if (lookup.getResult() != Lookup.SUCCESSFUL) {
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
                    break;
                }
            }
            //以下2项自己填写快速的，有效的邮箱
            client.login(SENDER_EMAIL_SERVER);
            client.setSender(SENDER_EMAIL);
            client.addRecipient(email);
            if (250 == client.getReplyCode()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
            }
        }
        return false;
    }

    public String checkPassword() throws IOException {

        Socket socket = ssf.createSocket(host, port);
        socket.setSoTimeout(TIMEOUT * 1000);
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
        out.println("PROPFIND " + path + " HTTP/1.1");
        out.println("Host: " + host);
        String userCredentials = userName + ":" + password;
        String basicAuth = "Basic " + new String(Base64.encode(userCredentials.getBytes()));
        String authorization = "Authorization: " + basicAuth;
        out.println(authorization.trim());
        out.println("Content-Length: " + xmlData.length());
        out.println("Content-Type: text/xml");
        out.println("Depth: 1");
        out.println();
        out.println(xmlData);
        out.flush();

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        List<String> counts = new ArrayList<>(1);
        if (counts.size() == 0) {
            try {
                counts.add(in.readLine());
            } catch (SocketTimeoutException st) {
                return "timeout";
            }
        }
        in.close();
        //        out.close();
        //        socket.close();
        if (counts.get(0).contains("207")) {
            return "true";
        } else {
            return "false";
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < repeat; i++) {
            try {
                String result = checkPassword();
                if (result.equals("true")) {
                    success = "true";
                    for(int j =0;j<=runCount.getCount()+1;j++){
                        runCount.countDown();
                    }
                    break;
                } else if (result.equals("false")) {
                    success = "false";
                    for(int j =0;j<=runCount.getCount()+1;j++){
                        runCount.countDown();
                    }
                    break;
                } else {
                    success = "timeout";
                    runCount.countDown();
                    System.out.println("repeating ...");
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean getSuccess() {
        try {
            runCount.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if ("true".equals(success)) {
            return true;
        }
        return false;
    }
}
