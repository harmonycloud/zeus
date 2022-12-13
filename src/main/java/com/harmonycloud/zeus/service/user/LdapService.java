package com.harmonycloud.zeus.service.user;

import com.middleware.caas.common.model.LdapConfigDto;
import com.harmonycloud.zeus.bean.BeanSystemConfig;

/**
 * @author liyinlong
 * @since 2022/3/10 4:43 下午
 */
public interface LdapService {

    /**
     * 添加一条ldap配置
     * @param ldapConfig
     */
    void save(BeanSystemConfig ldapConfig);

    /**
     * 根据name查询配置记录
     * @param configName
     * @return
     */
    BeanSystemConfig findByConfigName(String configName);

    /**
     * 保存单条配置
     * @param config_key
     * @param config_value
     */
    void saveSingle(String config_key, String config_value);

    /**
     * 保存ldap配置
     * @param ldapConfigDto
     */
    void save(LdapConfigDto ldapConfigDto);

    /**
     * 修改ldap配置
     * @param ldapConfig
     */
    void update(BeanSystemConfig ldapConfig);

    /**
     * 关闭ldap
     */
    void disable();

    /**
     * 查询ldap详细配置
     * @return
     */
    LdapConfigDto queryLdapDetail();

    /**
     * 连接测试
     * @param ldapConfigDto
     * @return
     */
    void connectionCheck(LdapConfigDto ldapConfigDto);
}
