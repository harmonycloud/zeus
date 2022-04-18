package com.harmonycloud.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.bean.BeanMiddlewareCluster;
import com.harmonycloud.zeus.dao.BeanMiddlewareClusterMapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCluster;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.util.K8sClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author yushuaikang
 * @date 2022/3/25 上午11:24
 */
@Slf4j
@Service
public class MiddlewareClusterServiceImpl implements MiddlewareClusterService {

    @Autowired
    private BeanMiddlewareClusterMapper middlewareClusterMapper;
    @Autowired
    private K8sClient k8sClient;

    @Override
    public void create(String name, MiddlewareCluster middlewareCluster) {
        BeanMiddlewareCluster beanMiddlewareCluster = new BeanMiddlewareCluster();
        beanMiddlewareCluster.setClusterId(name);
        middlewareCluster.getMetadata().setCreationTimestamp(convertDateToUTC());
        beanMiddlewareCluster.setMiddlewareCluster(JSONObject.toJSONString(middlewareCluster));
        middlewareClusterMapper.insert(beanMiddlewareCluster);
    }

    @Override
    public List<MiddlewareCluster> listClusters() {
        k8sClient.getDefaultClient();
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("clusterId").isNotNull("middleware_cluster");
        List<BeanMiddlewareCluster> beanMiddlewareClusters = middlewareClusterMapper.selectList(wrapper);
        List<MiddlewareCluster> middlewareClusters = new ArrayList<>();
        beanMiddlewareClusters.stream().forEach(beanMiddlewareCluster -> {
            MiddlewareCluster middlewareCluster = JSONObject.parseObject(JSONObject.toJSONString(JSON.parse(beanMiddlewareCluster.getMiddlewareCluster())), MiddlewareCluster.class);
            middlewareClusters.add(middlewareCluster);
        });
        return middlewareClusters;
    }

    @Override
    public void update(String name, MiddlewareCluster middlewareCluster) {
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        wrapper.eq("clusterId",name);
        BeanMiddlewareCluster beanMiddlewareCluster = new BeanMiddlewareCluster();
        beanMiddlewareCluster.setClusterId(name);
        beanMiddlewareCluster.setMiddlewareCluster(JSONObject.toJSONString(middlewareCluster));
        middlewareClusterMapper.update(beanMiddlewareCluster,wrapper);
    }

    @Override
    public void delete(String name) {
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        wrapper.eq("clusterId",name);
        middlewareClusterMapper.delete(wrapper);
    }

    @Override
    public List<BeanMiddlewareCluster> listClustersByClusterId(String clusterId) {
        k8sClient.getDefaultClient();
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        wrapper.eq("clusterId",clusterId);
        return middlewareClusterMapper.selectList(wrapper);
    }

    public String convertDateToUTC() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        now = DateUtils.addInteger(now,Calendar.HOUR_OF_DAY,-8);
        return sdf.format(now);
    }
}
