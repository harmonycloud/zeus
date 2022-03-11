package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.LdapConfigDto;
import com.harmonycloud.caas.common.model.user.UserDto;

/**
 * @author liyinlong
 * @description 认证方式接口, 如果需要扩展其他认证方式，需要实现该接口,并且配置在applicationContext.xml中配置认证方式
 * @since 2022/3/9 5:30 下午
 */
public interface AuthManager4Ldap {
    UserDto auth(String userName, String password, LdapConfigDto ldapConfigDto) throws Exception;
}
