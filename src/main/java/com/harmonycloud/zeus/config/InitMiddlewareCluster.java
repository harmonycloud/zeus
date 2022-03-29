package com.harmonycloud.zeus.config;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.bean.BeanMiddlewareCluster;
import com.harmonycloud.zeus.dao.BeanMiddlewareClusterMapper;
import com.harmonycloud.zeus.integration.cluster.ClusterWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCluster;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author yushuaikang
 * @date 2022/3/28 下午5:15
 */
@Slf4j
@Component
public class InitMiddlewareCluster {

    @Autowired
    private BeanMiddlewareClusterMapper temMiddlewareClusterMapper;
    @Autowired
    private MiddlewareClusterService temMiddlewareClusterService;
    @Autowired
    private ClusterWrapper temClusterWrapper;

    private BeanMiddlewareClusterMapper middlewareClusterMapper;
    private MiddlewareClusterService middlewareClusterService;
    private ClusterWrapper clusterWrapper;

    @PostConstruct
    public void init() throws Exception {
        if (middlewareClusterMapper == null) {
            middlewareClusterMapper = temMiddlewareClusterMapper;
        }
        if (middlewareClusterService == null) {
            middlewareClusterService = temMiddlewareClusterService;
        }
        if (clusterWrapper == null) {
            clusterWrapper = temClusterWrapper;
        }
        initMiddlewareCluster();
    }

    public void initMiddlewareCluster() {
        List<MiddlewareCluster> clusterList = clusterWrapper.listClusters();
        if (!clusterList.isEmpty()) {
            clusterList.forEach(middlewareCluster -> {
                BeanMiddlewareCluster beanMiddlewareCluster = new BeanMiddlewareCluster();
                String name = middlewareCluster.getMetadata().getName();
                List<BeanMiddlewareCluster> beanMiddlewareClusters = middlewareClusterService.listClustersByClusterId(name);
                if (beanMiddlewareClusters.isEmpty()) {
                    beanMiddlewareCluster.setClusterId(middlewareCluster.getMetadata().getName());
                    beanMiddlewareCluster.setMiddlewareCluster(JSONObject.toJSONString(middlewareCluster));
                    middlewareClusterMapper.insert(beanMiddlewareCluster);
                }
            });
        }
    }
}
