package com.middleware.zeus.service.system;

import com.middleware.caas.common.model.DisasterRecoveryDto;
import com.middleware.caas.common.model.DisasterRecoveryInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @auther wangpenglei
 * @date 2023/3/22 10:33
 */
public interface PlatformService {

    /**
     * 查询平台访问信息
     *
     * @return DisasterRecoveryDto
     */
    DisasterRecoveryDto queryAccessInfo();

    /**
     * 切换主备平台
     */
    void switchPlatform(Boolean isMaster);

    /**
     * 切换主备平台
     */
    void saveAddr(DisasterRecoveryInfo info);

    /**
     * 查询同步器状态
     *
     * @return DisasterRecoveryDto
     */
    DisasterRecoveryDto getMysqlReplicateStatus();

    /**
     * 获取zeus-mysql唯一标识
     *
     * @return String
     */
    String getMiddlewareUid();

    /**
     * 获取本地平台连接地址
     *
     * @return String
     */
    DisasterRecoveryInfo getLocalPlatformAddress();

    /**
     * 获取远程平台连接地址
     *
     * @return String
     */
    DisasterRecoveryInfo getRelationPlatformAddress();
}
