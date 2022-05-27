package com.harmonycloud.zeus.util;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

    public static JSONObject loadAsJson(String filePath){
        Yaml yaml = new Yaml();
        JSONObject jsonObject = yaml.loadAs(convertToString(filePath), JSONObject.class);
        return jsonObject;
    }



    /**
     * 合并JSON对象，用source覆盖target，返回覆盖后的JSON对象，
     *
     * @param source JSONObject
     * @param target JSONObject
     * @return JSONObject
     */
    public static JSONObject jsonMerge(JSONObject source, JSONObject target) {
        // 覆盖目标JSON为空，直接返回覆盖源
        if (target == null) {
            return source;
        }

        for (String key : source.keySet()) {
            Object value = source.get(key);
            if (!target.containsKey(key)) {
                target.put(key, value);
            } else {
                if (value instanceof JSONObject) {
                    JSONObject valueJson = (JSONObject) value;
                    JSONObject targetValue = jsonMerge(valueJson, target.getJSONObject(key));
                    target.put(key, targetValue);
                } else if (value instanceof JSONArray) {
                    JSONArray valueArray = (JSONArray) value;
                    for (int i = 0; i < valueArray.size(); i++) {
                        JSONObject obj = (JSONObject) valueArray.get(i);
                        JSONObject targetValue = jsonMerge(obj, (JSONObject) target.getJSONArray(key).get(i));
                        target.getJSONArray(key).set(i, targetValue);
                    }
                } else {
                    target.put(key, value);
                }
            }
        }
        return target;
    }

    /**
     * 将yaml文件加载为标准json对象（加载后的对象类型是JSONObject不是LinkedHashMap）
     * @param filePath 文件路径
     * @return 标注json对象
     */
    public static JSONObject loadAsNormalJsonObject(String filePath) {
        Yaml yaml = new Yaml();
        Map jsonObject = yaml.loadAs(convertToString(filePath), Map.class);
        String jsonStr = JSONUtil.toJsonStr(jsonObject);
        JSONObject normalJsonObj = JSONObject.parseObject(jsonStr);
        return normalJsonObj;
    }

    public static JSONObject convertYamlAsNormalJsonObject(String yamlStr) {
        Yaml yaml = new Yaml();
        Map jsonObject = yaml.loadAs(yamlStr, Map.class);
        String jsonStr = JSONUtil.toJsonStr(jsonObject);
        JSONObject normalJsonObj = JSONObject.parseObject(jsonStr);
        return normalJsonObj;
    }

    /**
     * 将values的json对象字段类型转为标准字段类型,空的tolerations应为[]，而底座chart包中是{}
     *
     * @param upgradeValues
     */
    public static void convertToStandardJsonObject(JSONObject upgradeValues) {
        Object tolerations = upgradeValues.get("tolerations");
        if (tolerations instanceof JSONObject) {
            JSONArray tolerationsArray = new JSONArray();
            tolerationsArray.add(tolerations);
            upgradeValues.put("tolerations", tolerationsArray);
        }
    }

}
