package com.middleware.zeus.service.system;

import com.middleware.zeus.bean.BeanSystemConfig;

/**
 * @author xutianhong
 * @Date 2022/11/1 3:00 下午
 */
public interface SystemConfigService {

    /**
     * 添加配置
     * @param name 配置名称
     * @param value 配置值
     *
     */
    void addConfig(String name, String value);

    /**
     * 更新配置
     * @param name 配置名称
     * @param value 配置值
     *
     */
    void updateConfig(String name, String value);

    /**
     * 获取配置
     * @param name 配置名称
     *
     * @return BeanSystemConfig
     */
    BeanSystemConfig getConfig(String name);

    /**
     * 获取配置
     * @param name 配置名称
     *
     * @return BeanSystemConfig
     */
    BeanSystemConfig getConfigForUpdate(String name);

}
