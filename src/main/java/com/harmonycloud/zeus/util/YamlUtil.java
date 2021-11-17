package com.harmonycloud.zeus.util;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/11/16 10:03 上午
 */
@Slf4j
public class YamlUtil {

    /**
     * 读取yaml文件内容
     * @param filePath yaml文件路径
     * @return 字符串
     */
    public static String convertToString(String filePath) {
        String conf = null;
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(filePath));
            conf = IOUtils.toString(in, String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("文件读取失败", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                log.error("文件关闭失败", e);
            }
        }
        return conf;
    }

    /**
     * 获取admin.conf中的server地址
     * @param filePath 文件路径
     * @return
     */
    public static String getServerAddress(String filePath) {
        Yaml yaml = new Yaml();
        String server = null;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            Map map = yaml.load(inputStream);
            Object clusters = map.get("clusters");
            List<Map> clusterList = (List<Map>) clusters;
            Map cluster = (Map) clusterList.get(0).get("cluster");
            server = cluster.get("server").toString();
        } catch (FileNotFoundException e) {
            log.error("文件读取失败", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("文件关闭失败", e);
            }
        }
        return server;
    }


    public static void main(String[] args) throws IOException {
        String filePath = "/Users/liyinlong/Desktop/upload/admin-160.conf";
        convertToString(filePath);
    }


}
