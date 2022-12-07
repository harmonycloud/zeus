package com.harmonycloud.zeus.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @author xutianhong
 * @Date 2022/12/5 2:18 下午
 */
@Slf4j
public class MiddlewareVersionUtil {

    public static Map<String, List<String>> convertVersion(String version) {
        Map<String, List<String>> map = new HashMap<>();
        try {
            String[] versions = version.split(",");
            for (int i = 0; i < versions.length; ++i) {
                String vx = versions[i].split("\\.")[0];
                if (!map.containsKey(vx)) {
                    List<String> list = new ArrayList<>();
                    list.add(versions[i]);
                    map.put(vx, list);
                } else {
                    map.get(vx).add(versions[i]);
                }
            }
            for (String key : map.keySet()) {
                map.get(key).sort(ChartVersionUtil::compare);
            }
        } catch (Exception e) {
            log.error("获取中间件类型失败，版本: {}", version);
            return map;
        }
        return map;
    }

}
