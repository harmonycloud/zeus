package com.harmonycloud.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.model.middleware.ImageRepositoryDTO;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.bean.BeanMiddlewareCluster;
import com.harmonycloud.zeus.dao.BeanMiddlewareClusterMapper;
import com.harmonycloud.zeus.integration.cluster.ClusterWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCluster;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.middleware.ImageRepositoryService;
import com.harmonycloud.zeus.util.K8sClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
    private ClusterWrapper clusterWrapper;
    @Autowired
    private ImageRepositoryService imageRepositoryService;

    @Override
    public void create(String clusterId, MiddlewareCluster middlewareCluster) {
        BeanMiddlewareCluster beanMiddlewareCluster = new BeanMiddlewareCluster();
        beanMiddlewareCluster.setClusterId(clusterId);
        middlewareCluster.getMetadata().setCreationTimestamp(convertDateToUTC());
        beanMiddlewareCluster.setMiddlewareCluster(JSONObject.toJSONString(middlewareCluster));
        middlewareClusterMapper.insert(beanMiddlewareCluster);
    }

    @Override
    public List<MiddlewareCluster> listClusters() {
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("cluster_id").isNotNull("middleware_cluster");
        List<BeanMiddlewareCluster> beanMiddlewareClusters = middlewareClusterMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanMiddlewareClusters)){
            beanMiddlewareClusters = initMiddlewareCluster();
        }
        List<MiddlewareCluster> middlewareClusters = new ArrayList<>();
        beanMiddlewareClusters.forEach(beanMiddlewareCluster -> {
            MiddlewareCluster middlewareCluster = JSONObject.parseObject(JSONObject.toJSONString(JSON.parse(beanMiddlewareCluster.getMiddlewareCluster())), MiddlewareCluster.class);
            middlewareClusters.add(middlewareCluster);
        });
        return middlewareClusters;
    }

    @Override
    public List<MiddlewareCluster> listClusters(String clusterId) {
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        List<BeanMiddlewareCluster> beanMiddlewareClusters = middlewareClusterMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanMiddlewareClusters)){
            beanMiddlewareClusters = initMiddlewareCluster();
        }
        List<MiddlewareCluster> middlewareClusters = new ArrayList<>();
        beanMiddlewareClusters.forEach(beanMiddlewareCluster -> {
            MiddlewareCluster middlewareCluster = JSONObject.parseObject(JSONObject.toJSONString(JSON.parse(beanMiddlewareCluster.getMiddlewareCluster())), MiddlewareCluster.class);
            middlewareClusters.add(middlewareCluster);
        });
        return middlewareClusters;
    }

    @Override
    public void update(String clusterId, MiddlewareCluster middlewareCluster) {
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId);
        BeanMiddlewareCluster beanMiddlewareCluster = new BeanMiddlewareCluster();
        beanMiddlewareCluster.setClusterId(clusterId);
        beanMiddlewareCluster.setMiddlewareCluster(JSONObject.toJSONString(middlewareCluster));
        middlewareClusterMapper.update(beanMiddlewareCluster, wrapper);
    }

    @Override
    public void delete(String clusterId) {
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId);
        middlewareClusterMapper.delete(wrapper);
    }

    @Override
    public List<BeanMiddlewareCluster> listClustersByClusterId(String clusterId) {
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        return middlewareClusterMapper.selectList(wrapper);
    }

    public String convertDateToUTC() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        now = DateUtils.addInteger(now, Calendar.HOUR_OF_DAY, -8);
        return sdf.format(now);
    }

    /**
     * 初始化middlewareCluster
     */
    public List<BeanMiddlewareCluster> initMiddlewareCluster() {
        List<MiddlewareCluster> middlewareClusterList = clusterWrapper.listClusters();
        if (CollectionUtils.isEmpty(middlewareClusterList)) {
            return new ArrayList<>();
        }
        for (MiddlewareCluster middlewareCluster : middlewareClusterList) {
            // 获取集群id
            String clusterId = K8sClient.getClusterId(middlewareCluster.getMetadata());
            // 创建集群在数据库中的记录
            this.create(clusterId, middlewareCluster);
            // 初始化集群默认的镜像仓库
            try {
                ImageRepositoryDTO imageRepositoryDTO =
                    imageRepositoryService.convertRegistry(middlewareCluster.getSpec().getInfo().getRegistry());
                imageRepositoryService.insert(clusterId, imageRepositoryDTO);
            } catch (Exception e) {
                log.error("集群{} 初始化镜像仓库失败", clusterId, e);
            }
        }
        return middlewareClusterMapper.selectList(
            new QueryWrapper<BeanMiddlewareCluster>().isNotNull("cluster_id").isNotNull("middleware_cluster"));
    }
}
