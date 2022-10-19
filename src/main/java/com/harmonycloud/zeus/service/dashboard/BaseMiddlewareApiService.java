package com.harmonycloud.zeus.service.dashboard;

/**
 * @author xutianhong
 * @Date 2022/10/19 3:03 下午
 */

public interface BaseMiddlewareApiService {

    /**
     * 确定对应service
     *
     * @param type 中间件类型
     * @return boolean
     */
    boolean support(String type);

    /**
     * 登录
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param username 用户名
     * @param password 密码
     */
    String login(String clusterId, String namespace, String middlewareName, String username, String password);

}
