package com.harmonycloud.zeus.service.user.impl;

import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.LdapConfigDto;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.tool.encrypt.PasswordUtils;
import com.harmonycloud.zeus.bean.user.BeanUser;
import com.harmonycloud.zeus.service.user.AuthManager4Ldap;
import com.harmonycloud.zeus.service.user.UserService;
import com.harmonycloud.zeus.util.AssertUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.naming.Name;
import javax.naming.directory.Attributes;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.middleware.caas.common.constants.LdapConfigConstant.*;

/**
 * @author liyinlong
 * @description ldap方式实现认证接口
 * @since 2022/3/9 5:17 下午
 */
@Service
public class AuthManager4LdapImpl implements AuthManager4Ldap {

    private static Logger LOGGER = LoggerFactory.getLogger(AuthManager4LdapImpl.class);

    private String searchType = "person";
    private String objectClass = "cn";

    @Autowired
    private UserService userService;

    @Override
    public UserDto auth(String userName, String password, LdapConfigDto ldapConfigDto) throws Exception {
        AssertUtil.notBlank(userName, DictEnum.USERNAME);
        AssertUtil.notBlank(password, DictEnum.PASSWORD);
        if (StringUtils.isBlank(ldapConfigDto.getObjectClass())) {
            ldapConfigDto.setObjectClass(objectClass);
        }
        if (StringUtils.isBlank(ldapConfigDto.getSearchAttribute())) {
            ldapConfigDto.setSearchAttribute(searchType);
        }
        Map<String, String> userAttributes = this.getUserFromLdap(userName, password, ldapConfigDto);
        if (userAttributes == null) {
            throw new BusinessException(ErrorMessage.USERNAME_SHOULD_NOT_BE_NULL);
        }
        // 对ldap认证通过的用户,判断是否已经记录,如果没有，则记录用户,并返回该用户
        return saveUserInfo(userName, password, ldapConfigDto, userAttributes);
    }

    private Map<String, String> getUserFromLdap(String userName, String password, LdapConfigDto ldapConfigDto) {
        LdapTemplate template = LdapServiceImpl.getTemplate(ldapConfigDto);
        Map<String, String> userAttribute = getUserAttribute(userName, template, ldapConfigDto);
        boolean authenticated = template.authenticate(userAttribute.get("dn"),
                new EqualsFilter("objectClass", ldapConfigDto.getObjectClass()).encode(), password);
        if (authenticated) {
            return userAttribute;
        } else {
            throw new BusinessException(ErrorMessage.WRONG_PASSWORD);
        }
    }

    /**
     * ldap验证通过保存或更新用户信息
     *
     * @param userName
     * @param password
     * @param userAttributes
     * @throws Exception
     */
    private UserDto saveUserInfo(String userName, String password, LdapConfigDto ldapConfigDto, Map<String, String> userAttributes) throws Exception {
        BeanUser user = userService.get(userName);
        if (user == null) {
            user = new BeanUser();
            user.setUserName(userName);
            user.setPassword(PasswordUtils.md5(password));
            user.setEmail(userAttributes.get(LDAP_MAIL));
            user.setPhone(userAttributes.get(LDAP_MOBILE));
            user.setCreateTime(new Date());
            user.setAliasName(userAttributes.get(ldapConfigDto.getDisplayNameAttribute()) == null ? userName : userAttributes.get(ldapConfigDto.getDisplayNameAttribute()));
            userService.create(user);
            return userService.getUserDto(userName);
        }
        boolean userInfoChanged = false;
        if (StringUtils.isNotBlank(userAttributes.get(LDAP_MAIL)) && !userAttributes.get(LDAP_MAIL).equals(user.getEmail())) {
            user.setEmail(userAttributes.get(LDAP_MAIL));
            userInfoChanged = true;
        }
        if (StringUtils.isNotBlank(userAttributes.get(LDAP_MOBILE)) && !userAttributes.get(LDAP_MOBILE).equals(user.getPhone())) {
            user.setPhone(userAttributes.get(LDAP_MOBILE));
            userInfoChanged = true;
        }
        if (StringUtils.isNotBlank(userAttributes.get(LDAP_REAL_NAME)) && !userAttributes.get(LDAP_REAL_NAME).equals(user.getAliasName())) {
            user.setAliasName(userAttributes.get(LDAP_REAL_NAME));
            userInfoChanged = true;
        }
        if (!user.getPassword().equals(PasswordUtils.md5(password))) {
            user.setPassword(PasswordUtils.md5(password));
        }

        if (userInfoChanged) {
            userService.update(user);
        }
        return userService.getUserDto(userName);
    }

    private Map<String, String> getUserAttribute(String cn, LdapTemplate template, LdapConfigDto ldapConfigDto) {
        AndFilter andFilter = new AndFilter();
        andFilter.and(new EqualsFilter("objectClass", ldapConfigDto.getObjectClass()));
        andFilter.and(new EqualsFilter(ldapConfigDto.getSearchAttribute(), cn));

        List<Map<String, String>> results = template.search("", andFilter.encode(), new DnMapper());

        if (CollectionUtils.isEmpty(results)) {
            throw new BusinessException(ErrorMessage.USER_NOT_EXIT);
        }
        return results.get(0);
    }

    /**
     * 节点的 Dn映射
     */
    public static class DnMapper implements ContextMapper {
        @Override
        public Map<String, String> mapFromContext(Object ctx) {
            Map<String, String> result = new HashMap<>();
            DirContextAdapter context = (DirContextAdapter) ctx;
            Name name = context.getDn();
            result.put("dn", name.toString());
            try {
                Attributes attributes = context.getAttributes();
                if (attributes.get(LDAP_MAIL) != null) {
                    result.put(LDAP_MAIL, attributes.get(LDAP_MAIL).get().toString());
                }
                if (attributes.get(LDAP_MOBILE) != null) {
                    result.put(LDAP_MOBILE, attributes.get(LDAP_MOBILE).get().toString());
                }
                if (attributes.get(LDAP_REAL_NAME) != null) {
                    result.put(LDAP_REAL_NAME, attributes.get(LDAP_REAL_NAME).get().toString());
                }
            } catch (Exception e) {
                LOGGER.error("获取用户信息错误，", e);
            }
            return result;
        }
    }
}
