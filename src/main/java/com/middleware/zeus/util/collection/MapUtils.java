package com.middleware.zeus.util.collection;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
public class MapUtils {

    private static final Logger logger = LoggerFactory.getLogger(MapUtils.class);

    public static Map<String, Object> objectToMap(Object obj) {
        Map<String, Object> map = new HashMap<>();
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object value;
            try {
                value = field.get(obj);
            } catch (IllegalAccessException e) {
                logger.error("{}对象转map异常", obj.getClass().getName());
                throw new RuntimeException("object " + obj.getClass().getName() + " convert to map exception.");
            }
            map.put(fieldName, value);
        }
        return map;
    }

    /**
     * Map转String
     * @param map
     * @return
     */
    public static String hashMapToString(Map<String, Object> map) {
        JSONObject json = new JSONObject(map);
        return json.toString();
    }

    /**
     * String转Map
     * @param str
     * @return
     */
    public static Map<String, Object> stringToHashMap(String str) {
        JSONObject json = JSONObject.parseObject(str);
        HashMap<String, Object> map = new HashMap<>();
        for (String key : json.keySet()) {
            // 根据key获取对应的value并存储到map中
            Object value = json.get(key);
            map.put(key, value);
        }
        return map;
    }
    
}
