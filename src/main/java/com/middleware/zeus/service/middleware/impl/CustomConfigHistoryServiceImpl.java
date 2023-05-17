package com.middleware.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.model.CustomConfigHistoryDo;
import com.middleware.zeus.common.model.middleware.CustomConfig;
import com.middleware.zeus.common.model.middleware.Middleware;
import com.middleware.zeus.common.model.middleware.MiddlewareCustomConfig;
import com.middleware.zeus.bean.BeanCustomConfigHistory;
import com.middleware.zeus.dao.BeanCustomConfigHistoryMapper;
import com.middleware.zeus.service.middleware.CustomConfigHistoryService;
import com.middleware.zeus.service.middleware.MiddlewareCustomConfigService;
import com.middleware.zeus.service.middleware.MiddlewareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

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
            beanCustomConfigHistory.setAfter(customConfig.getValue());
            beanCustomConfigHistory.setDate(now);
            beanCustomConfigHistory.setRestart(customConfig.getRestart());
            beanCustomConfigHistory.setStatus(false);
            beanCustomConfigHistory.setRole(middlewareCustomConfig.getRole());
            // 当前值不存在，选择默认值
            if (oldData.containsKey(customConfig.getName())) {
                beanCustomConfigHistory.setLast(oldData.get(customConfig.getName()));
            } else {
                beanCustomConfigHistory.setLast(customConfig.getDefaultValue());
            }
            beanCustomConfigHistoryMapper.insert(beanCustomConfigHistory);
        }
    }

    @Override
    public List<BeanCustomConfigHistory> get(String clusterId, String namespace, String name, String role) {
        QueryWrapper<BeanCustomConfigHistory> wrapper = new QueryWrapper<BeanCustomConfigHistory>()
                .eq("cluster_id", clusterId).eq("namespace", namespace).eq("name", name);
        if (role != null) {
            wrapper.eq("role", role);
        }
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

    @Override
    public List<CustomConfigHistoryDo> listLatestConfig(Middleware middleware) {
        QueryWrapper<BeanCustomConfigHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", middleware.getClusterId()).eq("namespace", middleware.getNamespace())
            .eq("name", middleware.getName());
        HashMap<String, BeanCustomConfigHistory> historyMap = new HashMap<>();
        beanCustomConfigHistoryMapper.selectList(wrapper).forEach(cch -> {
            if (cch.getDate() == null) {
                return;
            }
            if (historyMap.containsKey(cch.getItem()) && historyMap.get(cch.getItem()).getDate() != null && historyMap.get(cch.getItem()).getDate().after(cch.getDate())) {
                return;
            }
            historyMap.put(cch.getItem(), cch);
        });
        ArrayList<CustomConfigHistoryDo> resultList = new ArrayList<>();
        for (BeanCustomConfigHistory history : historyMap.values()) {
            CustomConfigHistoryDo historyDo = new CustomConfigHistoryDo();
            BeanUtils.copyProperties(history, historyDo);
            resultList.add(historyDo);
        }
        return resultList;
    }


}
