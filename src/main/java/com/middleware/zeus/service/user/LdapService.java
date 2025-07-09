package com.middleware.zeus.service.user;

import com.middleware.zeus.common.model.LdapConfigDto;
import com.middleware.zeus.bean.BeanSystemConfig;

/**
 * @author liyinlong
 * @since 2022/3/10 4:43 下午
 */
public interface LdapService {

    /**
     * 开启/关闭ldap
     * @param enable 是否启用
     */
    void enable(Boolean enable);

    /**
     * 查看ldap开关
     */
    Boolean enableInfo();

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
