package com.middleware.zeus.util;

import com.dtflys.forest.utils.StringUtils;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;

/**
 * @author liyinlong
 * @since 2023/2/9 12:04 下午
 */
@Slf4j
public class MinioUtils {

    public static int checkConnection(String protocol, String host, String port, String username, String password) {
        String portStr = StringUtils.isNotEmpty(port) ? ":" + port : "";
        String url = protocol + "://" + host + portStr;
        //构造方法 minio客户端
        MinioClient minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(username, password)
                .build();
        minioClient.setTimeout(3000, 1000, 1000);
        try {
            minioClient.listBuckets();
        } catch (ConnectException e) {
            log.error("连接{}失败", url);
            return 1;
        } catch (ErrorResponseException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            String errMsg = sw.toString();
            if (errMsg.contains("InvalidAccessKeyId")) {
                log.error("用户名错误");
            } else if (errMsg.contains("SignatureDoesNotMatch")) {
                log.error("密码错误");
            }
            return 2;
        } catch (Exception e) {
            log.error("未知错误");
            return 3;
        }
        return 0;
    }

}
