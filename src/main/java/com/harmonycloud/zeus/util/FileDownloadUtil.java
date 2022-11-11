package com.harmonycloud.zeus.util;

import com.harmonycloud.tool.file.FileUtil;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

/**
 * @author liyinlong
 * @since 2022/10/24 3:07 下午
 */
@Slf4j
public class FileDownloadUtil {

    public static void downloadFile(HttpServletResponse response, String fileDir, String fileName, String context)
        throws IOException {
        FileUtil.writeToLocal(fileDir, fileName, context);
        downloadFile(response, fileDir, fileName);
    }

    public static void downloadFile(HttpServletResponse response, String fileDir, String fileName) throws IOException {
        File file = new File(fileDir + File.separator + fileName);
        if (file.exists()) {
            response.setContentType("application/octet-stream");
            response.setHeader("content-type", "application/octet-stream");
            response.setHeader("Content-Length", String.valueOf(file.length()));
            response.setHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(fileName, "utf8"));
            byte[] buffer = new byte[2048];
            //输出流
            try (FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                OutputStream os = response.getOutputStream()) {
                int i = bis.read(buffer);
                while (i != -1) {
                    os.write(buffer);
                    i = bis.read(buffer);
                }
            } catch (Exception e) {
                log.error("文件{} 下载失败", fileName);
            }
        }
    }

    public static void downloadFile(HttpServletResponse response, String fileName, byte[] bytes) throws IOException {
        response.setContentType("application/octet-stream");
        response.setHeader("content-type", "application/octet-stream");
        response.setHeader("Content-Length", String.valueOf(bytes.length));
        response.setHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(fileName, "utf8"));
        //输出流
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
        } catch (Exception e) {
            log.error("文件{} 下载失败", fileName);
        }
    }

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
