package com.harmonycloud.zeus.service.k8s.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.bean.BeanK8sDefaultCluster;
import com.harmonycloud.zeus.dao.BeanK8sDefaultClusterMapper;
import com.harmonycloud.zeus.service.k8s.K8sDefaultClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/8/3 3:56 下午
 */
@Slf4j
@Service
@Deprecated
public class K8SDefaultClusterServiceImpl implements K8sDefaultClusterService {
    
    @Autowired
    private BeanK8sDefaultClusterMapper beanK8sDefaultClusterMapper;

    @Override
    public BeanK8sDefaultCluster get() {
        QueryWrapper<BeanK8sDefaultCluster> wrapper = new QueryWrapper<>();
        List<BeanK8sDefaultCluster> defaultClusterList = beanK8sDefaultClusterMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(defaultClusterList)) {
            return null;
        }
        return defaultClusterList.get(defaultClusterList.size() - 1);
    }

    @Override
    public void create(MiddlewareClusterDTO cluster) {
        try {
            BeanK8sDefaultCluster defaultCluster = new BeanK8sDefaultCluster();
            defaultCluster.setClusterId(cluster.getId());
            defaultCluster.setUrl("https://" + cluster.getHost() + ":" + cluster.getPort());
            defaultCluster.setToken(cluster.getAccessToken());
            beanK8sDefaultClusterMapper.insert(defaultCluster);
        } catch (Exception e) {
            log.error("集群{}, 默认集群写入失败", cluster.getId());
        }
    }

    @Override
    public void delete(String clusterId) {
        QueryWrapper<BeanK8sDefaultCluster> wrapper =
            new QueryWrapper<BeanK8sDefaultCluster>().eq("cluster_id", clusterId);
        beanK8sDefaultClusterMapper.delete(wrapper);
    }
}
