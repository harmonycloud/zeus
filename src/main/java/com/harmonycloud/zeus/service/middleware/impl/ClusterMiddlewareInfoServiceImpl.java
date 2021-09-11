package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.dao.BeanClusterMiddlewareInfoMapper;
import com.harmonycloud.zeus.service.middleware.ClusterMiddlewareInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/9/3 4:10 下午
 */
@Service
@Slf4j
public class ClusterMiddlewareInfoServiceImpl implements ClusterMiddlewareInfoService {

    @Autowired
    private BeanClusterMiddlewareInfoMapper beanClusterMiddlewareInfoMapper;
    
    @Override
    public List<BeanClusterMiddlewareInfo> list(String clusterId) {
        QueryWrapper<BeanClusterMiddlewareInfo> wrapper =
            new QueryWrapper<BeanClusterMiddlewareInfo>().eq("cluster_id", clusterId);
        return beanClusterMiddlewareInfoMapper.selectList(wrapper);
    }

    @Override
    public BeanClusterMiddlewareInfo get(String clusterId, String type) {
        QueryWrapper<BeanClusterMiddlewareInfo> wrapper =
            new QueryWrapper<BeanClusterMiddlewareInfo>().eq("cluster_id", clusterId).eq("chart_name", type);
        return beanClusterMiddlewareInfoMapper.selectOne(wrapper);
    }

    @Override
    public void insert(BeanClusterMiddlewareInfo beanClusterMiddlewareInfo) {
        beanClusterMiddlewareInfoMapper.insert(beanClusterMiddlewareInfo);
    }

    @Override
    public void update(BeanClusterMiddlewareInfo beanClusterMiddlewareInfo) {
        QueryWrapper<BeanClusterMiddlewareInfo> wrapper =
            new QueryWrapper<BeanClusterMiddlewareInfo>().eq("chart_name", beanClusterMiddlewareInfo.getChartName())
                .eq("chart_version", beanClusterMiddlewareInfo.getChartVersion())
                .eq("cluster_id", beanClusterMiddlewareInfo.getClusterId());
        beanClusterMiddlewareInfoMapper.update(beanClusterMiddlewareInfo, wrapper);
    }

    @Override
    public void delete(String clusterId, String chartName, String chartVersion) {
        QueryWrapper<BeanClusterMiddlewareInfo> wrapper =
            new QueryWrapper<BeanClusterMiddlewareInfo>().eq("chart_name", chartName).eq("chart_version", chartVersion);
        beanClusterMiddlewareInfoMapper.delete(wrapper);
    }
}
