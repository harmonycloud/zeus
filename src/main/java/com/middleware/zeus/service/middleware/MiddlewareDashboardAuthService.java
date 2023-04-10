package com.middleware.zeus.service.middleware;

import com.alibaba.fastjson.JSONObject;

/**
 * @author xutianhong
 * @Date 2022/10/11 10:53 上午
 */
public interface MiddlewareDashboardAuthService {

    /**
     * 登录
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间名称
     * @param username 用户名
     * @param password 秘密
     * @param type 中间件类型
     * @return JSONObject
     **/
    JSONObject login(String clusterId, String namespace, String middlewareName, String username, String password, String type);

    /**
     * 登录
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param username
     * @param password
     * @param type
     * @param encryptedPassword 是否解密密码
     * @return
     */
    JSONObject login(String clusterId, String namespace, String middlewareName, String username, String password, String type, Boolean encryptedPassword);

    /**
     * 登出
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间名称
     * @param type 中间件类型
     * @return JSONObject
     **/
    void logout(String clusterId, String namespace, String middlewareName, String type);

    /**
     * 手动获取并添加MWToken到header中
     */
    void addMWToken(String clusterId, String namespace, String middlewareName, String type);

}
