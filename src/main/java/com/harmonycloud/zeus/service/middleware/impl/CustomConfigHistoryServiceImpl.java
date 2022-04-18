package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.model.middleware.CustomConfig;
import com.harmonycloud.caas.common.model.middleware.MiddlewareCustomConfig;
import com.harmonycloud.zeus.bean.BeanCustomConfigHistory;
import com.harmonycloud.zeus.dao.BeanCustomConfigHistoryMapper;
import com.harmonycloud.zeus.service.middleware.CustomConfigHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/3/4 3:42 下午
 */
@Service
@Slf4j
public class CustomConfigHistoryServiceImpl implements CustomConfigHistoryService {

    @Autowired
    private BeanCustomConfigHistoryMapper beanCustomConfigHistoryMapper;

    @Override
    public void insert(String middlewareName, Map<String, String> oldData,
        MiddlewareCustomConfig middlewareCustomConfig) {
        Date now = new Date();
        for (CustomConfig customConfig : middlewareCustomConfig.getCustomConfigList()) {
            BeanCustomConfigHistory beanCustomConfigHistory = new BeanCustomConfigHistory();
            beanCustomConfigHistory.setItem(customConfig.getName());
            beanCustomConfigHistory.setClusterId(middlewareCustomConfig.getClusterId());
            beanCustomConfigHistory.setNamespace(middlewareCustomConfig.getNamespace());
            beanCustomConfigHistory.setName(middlewareName);
            beanCustomConfigHistory.setLast(oldData.get(customConfig.getName()));
            beanCustomConfigHistory.setAfter(customConfig.getValue());
            beanCustomConfigHistory.setDate(now);
            beanCustomConfigHistory.setRestart(customConfig.getRestart());
            beanCustomConfigHistory.setStatus(false);
            beanCustomConfigHistoryMapper.insert(beanCustomConfigHistory);
        }
    }

    @Override
    public List<BeanCustomConfigHistory> get(String clusterId, String namespace, String name) {
        QueryWrapper<BeanCustomConfigHistory> wrapper = new QueryWrapper<BeanCustomConfigHistory>()
                .eq("cluster_id", clusterId).eq("namespace", namespace).eq("name", name);
        return beanCustomConfigHistoryMapper.selectList(wrapper);
    }

    @Override
    public void delete(String clusterId, String namespace, String name) {
        QueryWrapper<BeanCustomConfigHistory> wrapper = new QueryWrapper<BeanCustomConfigHistory>()
                .eq("cluster_id", clusterId).eq("namespace", namespace).eq("name", name);
        beanCustomConfigHistoryMapper.delete(wrapper);
    }

    @Override
    public void update(BeanCustomConfigHistory beanCustomConfigHistory) {
        QueryWrapper<BeanCustomConfigHistory> wrapper =
                new QueryWrapper<BeanCustomConfigHistory>().eq("id", beanCustomConfigHistory.getId());
        beanCustomConfigHistoryMapper.update(beanCustomConfigHistory, wrapper);
    }


}
