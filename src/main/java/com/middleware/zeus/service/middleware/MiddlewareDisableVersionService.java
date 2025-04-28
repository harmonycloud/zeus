package com.middleware.zeus.service.middleware;

import com.middleware.zeus.bean.BeanMiddlewareDisableVersion;
import com.middleware.zeus.common.model.middleware.MiddlewareDisableVersionDo;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2025/4/28 上午10:17
 */
public interface MiddlewareDisableVersionService {


    /**
     * 获取禁用版本
     */
    List<MiddlewareDisableVersionDo> get(String clusterId, String chartName, String chartVersion);

    /**
     * 获取禁用版本
     */
    void add(MiddlewareDisableVersionDo middlewareDisableVersionDo);

    /**
     * 清理禁用版本
     */
    void clear(String clusterId, String chartName, String chartVersion);

}
