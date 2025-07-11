package com.middleware.zeus.service.system;

import com.github.pagehelper.PageInfo;
import com.middleware.zeus.bean.BeanAlertRecord;
import com.middleware.zeus.common.model.*;
import com.middleware.zeus.common.model.middleware.MiddlewareAlertsDTO;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/5/6 11:03 上午
 */
public interface AlertService {

    /**
     * 查询告警记录索引
     *
     * @param alertType 告警对象类型
     * @return List<AlertRecordIndex>
     */
    List<AlertRecordIndex> alertRecordIndex(String alertType);

    /**
     * 查询告警记录索引
     *
     * @param alertType 告警对象类型
     * @return List<AlertRecordIndex>
     */
    List<AlertRecordIndex> alertRecordFilter(String alertType, String clusterId);



    /**
     * 查询告警记录
     *
     * @param alertRecordQueryDto 告警记录查询
     * @return List<BeanAlertRecord>
     */
    List<BeanAlertRecord> searchAlertRecord(AlertRecordQueryDto alertRecordQueryDto);

    PageInfo<AlertDTO> pageAlertRecord(List<BeanAlertRecord> alertRecordList);

    /**
     * 新建/接入告警对象
     *
     * @param alertTargetDto 告警对象
     */
    void alertTarget(AlertTargetDto alertTargetDto);

    /**
     * 查询告警对象列表
     *
     * @param clusterId 集群id
     * @return List<AlertTargetDto>
     */
    List<AlertTargetDto> alertTargetList(String clusterId);

    /**
     * 查询告警规则
     *
     * @param clusterId 集群id
     * @param targetName 告警对象名称
     * @param namespace 分区
     * @param prometheusRuleName 告警规则文件名称
     * @return List<MiddlewareAlertsDTO>
     */
    List<MiddlewareAlertsDTO> alertRule(String targetName, String clusterId, String namespace, String prometheusRuleName);

    /**
     * 查询告警用户
     *
     * @param clusterId 集群id
     * @param allocatable 可分配的
     * @param roleId  角色id
     * @return List<AlertUserDTO>
     */
    List<AlertUserDto> alertUser(String clusterId, Boolean allocatable, Integer roleId);

    /**
     * 添加告警用户
     *
     * @param alertUserListDto 告警用户列表
     */
    void addAlertUser(AlertUserListDto alertUserListDto);

    /**
     * 移除告警用户
     *
     * @param username 用户名
     * @param clusterId 集群id
     */
    void removeAlertUser(String username, String clusterId);

    /**
     * 周期更新系统组件告警中的标签
     */
    void refreshPrometheusRulesLabels();

}
