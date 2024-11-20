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
    public Map<String, List<String>> getChatSet() {
        Map<String, List<String>> mp = new HashMap<>();
        mp.put("big5",Arrays.asList("zh_HK.BIG5","zh_TW.BIG5"));
        mp.put("EUC_CN",Arrays.asList("zh_CN.EUC_CN"));
        mp.put("EUC_JP",Arrays.asList("ja_JP.EUC-JP"));
        mp.put("EUC_JIS_2004",Arrays.asList("ja_JP.EUC-JIS-2004"));
        mp.put("EUC_KR",Arrays.asList("ko_KR.EUC-KR"));
        mp.put("EUC_TW",Arrays.asList("zh_TW.EUC-TW"));
        mp.put("GB18030",Arrays.asList("zh_CN.GB18030","zh_TW.GB18030","ja_JP.GB18030","ko_KR.GB18030"));
        mp.put("GBK",Arrays.asList("zh_CN.GBK","zh_TW.GBK"));
        mp.put("UTF8",Arrays.asList("zh_CN.UTF-8","en_US.UTF-8","ja_JP.UTF-8","ko_KR.UTF-8","de_DE.UTF-8","fr_FR.UTF-8","es_ES.UTF-8",
                "ru_RU.UTF-8","pt_PT.UTF-8","it_IT.UTF-8","ar_SA.UTF-8"));
        return mp;
    }
}
