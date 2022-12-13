package com.middleware.zeus.service.system;

import com.middleware.caas.common.model.LicenseInfoDto;

/**
 * @author xutianhong
 * @Date 2022/10/27 11:43 上午
 */
public interface LicenseService {

    /**
     * 解析license
     *
     * @param license
     */
    void license(String license) throws Exception;

    /**
     * 查询license使用信息
     *
     */
    LicenseInfoDto info();

    /**
     * 平台可用额度校验
     *
     * @return Boolean
     */
    Boolean check(String clusterId);

    /**
     * 计算cpu使用量
     */
    void refreshMiddlewareResource() throws Exception;

    /**
     * 计算cpu使用量
     *
     * @param type 集群类型
     * @param cpu cpu
     */
    void addMiddlewareResource(String type, Double cpu);



}
