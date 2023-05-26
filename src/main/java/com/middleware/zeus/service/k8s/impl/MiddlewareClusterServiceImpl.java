package com.middleware.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.bean.BeanMiddlewareCluster;
import com.middleware.zeus.dao.BeanMiddlewareClusterMapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCluster;
import com.middleware.zeus.integration.cluster.bean.MiddlewareClusterInfo;
import com.middleware.zeus.service.k8s.MiddlewareClusterService;
import com.middleware.zeus.service.middleware.ImageRepositoryService;
import com.middleware.zeus.util.K8sClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.NameConstant.CREATE_TIME;
import static com.middleware.zeus.common.constants.NameConstant.NAME;

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
    public List<MiddlewareClusterDTO> listClusterDtos() {
        List<MiddlewareCluster> middlewareClusters = listClusters();
        return middlewareClusters.stream().map(c -> {
            MiddlewareClusterInfo info = c.getSpec().getInfo();
            MiddlewareClusterDTO cluster = new MiddlewareClusterDTO();
            BeanUtils.copyProperties(info, cluster);
            cluster.setId(K8sClient.getClusterId(c.getMetadata())).setHost(info.getAddress())
                    .setName(c.getMetadata().getName()).setDcId(c.getMetadata().getNamespace())
                    .setAnnotations(c.getMetadata().getAnnotations());
            if (!CollectionUtils.isEmpty(c.getMetadata().getAnnotations())) {
                cluster.setNickname(c.getMetadata().getAnnotations().get(NAME));
            }
            JSONObject attributes = new JSONObject();
            if (c.getMetadata().getCreationTimestamp() != null) {
                attributes.put(CREATE_TIME, c.getMetadata().getCreationTimestamp().contains("T")
                        ? DateUtils.parseUTCDate(c.getMetadata().getCreationTimestamp()) : c.getMetadata().getCreationTimestamp());
                cluster.setAttributes(attributes);
            }
            if (StringUtils.isEmpty(info.getAccessToken())) {
                cluster.setAccessToken(info.getAccessToken());
            }
            return SerializationUtils.clone(cluster);
        }).collect(Collectors.toList());
    }

    @Override
    public List<MiddlewareCluster> listClusters() {
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("cluster_id").isNotNull("middleware_cluster");
        List<BeanMiddlewareCluster> beanMiddlewareClusters = middlewareClusterMapper.selectList(wrapper);
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        return sdf.format(now);
    }
}
