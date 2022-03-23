package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.bean.BeanCacheMiddleware;
import com.harmonycloud.zeus.dao.BeanCacheMiddlewareMapper;
import com.harmonycloud.zeus.service.middleware.CacheMiddlewareService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/12/13 2:56 下午
 */
@Service
public class CacheMiddlewareServiceImpl implements CacheMiddlewareService {
    
    @Autowired
    private BeanCacheMiddlewareMapper beanCacheMiddlewareMapper;
    
    @Override
    public List<BeanCacheMiddleware> list(String clusterId, String namespace, String type) {
        QueryWrapper<BeanCacheMiddleware> wrapper =
            new QueryWrapper<BeanCacheMiddleware>().eq("cluster_id", clusterId);
        if(StringUtils.isNotEmpty(namespace)){
            wrapper.eq("namespace", namespace);
        }
        if (StringUtils.isNotEmpty(type)){
            wrapper.eq("type", type);
        }
        List<BeanCacheMiddleware> beanCacheMiddlewareList = beanCacheMiddlewareMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanCacheMiddlewareList)) {
            return new ArrayList<>();
        }
        return beanCacheMiddlewareList;
    }

    @Override
    public BeanCacheMiddleware get(String clusterId, String namespace, String type, String name) {
        try {
            QueryWrapper<BeanCacheMiddleware> wrapper = new QueryWrapper<BeanCacheMiddleware>().eq("cluster_id", clusterId)
                    .eq("namespace", namespace).eq("type", type).eq("name", name);
            return beanCacheMiddlewareMapper.selectOne(wrapper);
        } catch (Exception e){
            throw new BusinessException(ErrorMessage.FIND_CACHE_MIDDLEWARE_FAILED);
        }
    }

    @Override
    public BeanCacheMiddleware get(Middleware middleware) {
        return this.get(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
    }

    @Override
    public void insert(BeanCacheMiddleware beanCacheMiddleware) {
        beanCacheMiddlewareMapper.insert(beanCacheMiddleware);
    }

    @Override
    public void delete(String clusterId, String namespace, String type, String name) {
        QueryWrapper<BeanCacheMiddleware> wrapper = new QueryWrapper<BeanCacheMiddleware>().eq("cluster_id", clusterId)
            .eq("namespace", namespace).eq("type", type).eq("name", name);
        beanCacheMiddlewareMapper.delete(wrapper);
    }

    @Override
    public void delete(Middleware middleware) {
        this.delete(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
    }
}
