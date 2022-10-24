package com.harmonycloud.zeus.util;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

/**
 * @author liyinlong
 * @since 2022/10/24 3:07 下午
 */
@Slf4j
public class FileDownloadUtil {

    /**
     * 下载文件
     * @param request
     * @param response
     * @param fileRealName
     * @param dataBytes
     */
    public static void downloadFile(HttpServletRequest request, HttpServletResponse response, String fileRealName, byte[] dataBytes) {
        try (OutputStream toClient = new BufferedOutputStream(response.getOutputStream())) {
            response.reset();
            // 设置response的Header
            request.setCharacterEncoding("utf-8");
            response.setCharacterEncoding("utf-8");
            response.addHeader("Content-Length", "" + dataBytes.length);

            response.setContentType("application/x-download");
            String Agent = request.getHeader("User-Agent");
            if (null != Agent) {
                Agent = Agent.toLowerCase();
                if (Agent.contains("firefox")) {
                    response.addHeader("Content-Disposition", String.format("attachment;filename*=utf-8'zh_cn'%s", URLEncoder.encode(fileRealName, "utf-8")));
                } else {
                    response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileRealName, "utf-8"));
                }
            }
            toClient.write(dataBytes);
            toClient.flush();
        } catch (IOException e) {
            log.error("下载文件失败", e);
        }
    }

    public static void downloadFile(HttpServletRequest request, HttpServletResponse response, String fileRealName, String filePath) {
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            int available = inputStream.available();
            byte[] bytes = new byte[available];
            inputStream.read(bytes);
            downloadFile(request, response, fileRealName, bytes);
        } catch (IOException e) {
            log.error("下载文件失败", e);
        }
    }

}
