package com.middleware.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.bean.BeanMiddlewareDisableVersion;
import com.middleware.zeus.common.model.middleware.MiddlewareDisableVersionDo;
import com.middleware.zeus.common.model.middleware.MiddlewareDisableVersionDto;
import com.middleware.zeus.dao.BeanMiddlewareDisableVersionMapper;
import com.middleware.zeus.service.middleware.MiddlewareDisableVersionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2025/4/28 上午10:17
 */
@Slf4j
@Service
public class MiddlewareDisableVersionServiceImpl implements MiddlewareDisableVersionService {

    @Autowired
    private BeanMiddlewareDisableVersionMapper beanMiddlewareDisableVersionMapper;


    @Override
    public List<MiddlewareDisableVersionDo> get(String clusterId, String chartName, String chartVersion) {
        QueryWrapper<BeanMiddlewareDisableVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("cluster_id", clusterId);
        queryWrapper.eq("chart_name", chartName);
        if (StringUtils.isNotEmpty(chartVersion)){
            queryWrapper.eq("chart_version", chartVersion);
        }

        return beanMiddlewareDisableVersionMapper.selectList(queryWrapper).stream().map(MiddlewareDisableVersionDo::new)
            .collect(Collectors.toList());
    }

    @Override
    public void add(MiddlewareDisableVersionDo middlewareDisableVersionDo) {
        QueryWrapper<BeanMiddlewareDisableVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chart_name", middlewareDisableVersionDo.getChartName());
        queryWrapper.eq("chart_version", middlewareDisableVersionDo.getChartVersion());
        queryWrapper.eq("cluster_id", middlewareDisableVersionDo.getClusterId());
        queryWrapper.eq("disable_version", middlewareDisableVersionDo.getVersion());

        BeanMiddlewareDisableVersion beanMiddlewareDisableVersion = new BeanMiddlewareDisableVersion();
        beanMiddlewareDisableVersion.setChartName(middlewareDisableVersionDo.getChartName());
        beanMiddlewareDisableVersion.setChartVersion(middlewareDisableVersionDo.getChartVersion());
        beanMiddlewareDisableVersion.setClusterId(middlewareDisableVersionDo.getClusterId());
        beanMiddlewareDisableVersion.setDisableVersion(middlewareDisableVersionDo.getVersion());

        if (beanMiddlewareDisableVersionMapper.selectCount(queryWrapper) > 0) {
            log.warn("禁用版本已存在");
            return;
        }
        beanMiddlewareDisableVersionMapper.insert(beanMiddlewareDisableVersion);

    }

    @Override
    public void clear(String clusterId, String chartName, String chartVersion) {
        QueryWrapper<BeanMiddlewareDisableVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chart_name", chartName);
        queryWrapper.eq("chart_version", chartVersion);
        queryWrapper.eq("cluster_id", clusterId);

        beanMiddlewareDisableVersionMapper.delete(queryWrapper);
    }
}
