package com.harmonycloud.zeus.service.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.dashboard.DatabaseDto;
import com.harmonycloud.caas.common.model.dashboard.MiddlewareUserAuthority;
import com.harmonycloud.caas.common.model.dashboard.MiddlewareUserDto;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/10/11 2:50 下午
 */
public interface PostgresqlDashboardService {

    /**
     * 获取database列表
     */
    List<DatabaseDto> listDatabases(String clusterId, String namespace, String middlewareName);

    /**
     * 获取user列表
     *
     */
    List<MiddlewareUserDto> listUser(String clusterId, String namespace, String middlewareName, String keyword);

    /**
     * 创建用户
     *
     */
    void addUser(String clusterId, String namespace, String middlewareName, MiddlewareUserDto middlewareUserDto);

    /**
     * 删除用户
     *
     */
    void dropUser(String clusterId, String namespace, String middlewareName, String username);

    /**
     * 获取用户权限
     *
     */
    List<MiddlewareUserAuthority> userAuthority(String clusterId, String namespace, String middlewareName, String username);

}
