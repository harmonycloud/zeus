package com.middleware.zeus.service.middleware.impl;

import com.middleware.zeus.service.middleware.PostgresqlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author lfy
 * @date 2024/11/20 09:53:39 上午
 */
@Slf4j
@Service
public class PostgresqlServiceImpl implements PostgresqlService {

    @Override
    public Map<String, List<String>> getChatSet(String version) {
        Map<String, List<String>> mp = new LinkedHashMap<>();
        mp.put("UTF8",Arrays.asList("zh_CN.UTF-8","en_US.UTF-8","zh_TW.UTF-8"));
        return mp;
    }
}
