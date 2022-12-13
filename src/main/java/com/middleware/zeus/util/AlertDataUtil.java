package com.middleware.zeus.util;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 告警统计
 * @author liyinlong
 * @since 2021/9/27 3:57 下午
 */
public class AlertDataUtil {

    /**
     * 对未查询到到小时补零
     * @param list
     * @param hourList
     */
    public static List<Map<String, Object>> checkAndFillZero(List<Map<String, Object>> list, List<String> hourList) {
        List<Map<String, Object>> resList = new ArrayList<>();
        Map<String, String> map = new HashMap<>();

        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(item -> {
                String alerttime = item.get("alerttime").toString();
                String num = item.get("num").toString();
                map.put(DateUtil.addHourAndGetNormal(alerttime, 8), num);
            });
        }

        hourList.forEach(item -> {
            Map<String, Object> single = new HashMap();
            if (map.containsKey(item)) {
                single.put(DateUtil.addHourAndGetNormal(item, 0), map.get(item));
                single.put("num", map.get(item));
                single.put("alerttime", DateUtil.addHourAndGetNormal(item, 0));
            } else {
                single.put(DateUtil.addHourAndGetNormal(item, 0), "0");
                single.put("num", 0);
                single.put("alerttime", DateUtil.addHourAndGetNormal(item, 0));
            }
            resList.add(single);
        });
        return resList;
    }

    /**
     * 统计异常数量
     * @param list
     * @return
     */
    public static int countAlertNum(List<Map<String, Object>> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0;
        }
        AtomicInteger sum = new AtomicInteger(0);
        list.forEach(item -> {
            sum.getAndAdd(Integer.parseInt(item.get("num").toString()));
        });
        return sum.intValue();
    }

}
