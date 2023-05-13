package com.middleware.zeus.service.system;

import com.middleware.zeus.common.model.AlertUserDo;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/5/8 11:01 上午
 */
public interface AlertUserService {

    /**
     * 查询告警用户
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param name 名称
     * @param alertType 告警对象类型
     * @return List<AlertUserDo>
     */
    List<AlertUserDo> list(String clusterId, String namespace, String name, String alertType);

    /**
     * 查询告警用户
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param name 名称
     * @param alertType 告警对象类型
     * @return List<AlertUserDo>
     */
    List<AlertUserDo> listWithUserInfo(String clusterId, String namespace, String name, String alertType);

    /**
     * 添加告警用户
     *
     * @param alertUserDo 告警用户
     * @return
     */
    void add(AlertUserDo alertUserDo);

    /**
     * 移除告警用户
     *
     * @param username 用户名
     * @param clusterId 集群id
     * @param alertType 告警类型
     */
    void delete(String username, String clusterId, String namespace, String name, String alertType);

}
