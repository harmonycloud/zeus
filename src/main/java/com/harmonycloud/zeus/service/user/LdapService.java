package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.LdapConfigDto;
import com.harmonycloud.zeus.bean.BeanLdapConfig;

/**
 * @author liyinlong
 * @since 2022/3/10 4:43 下午
 */
public interface LdapService {

    /**
     * 添加一条ldap配置
     * @param ldapConfig
     */
    void save(BeanLdapConfig ldapConfig);

    /**
     * 根据name查询配置记录
     * @param configName
     * @return
     */
    BeanLdapConfig findByConfigName(String configName);

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
    void update(BeanLdapConfig ldapConfig);

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
    boolean connectionCheck(LdapConfigDto ldapConfigDto);
}
