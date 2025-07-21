package com.middleware.zeus.service.user.impl;

import cn.hutool.core.util.ObjectUtil;
import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.LdapConfigDto;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.zeus.util.encrypt.PasswordUtils;
import com.middleware.zeus.bean.user.BeanUser;
import com.middleware.zeus.service.user.AuthManager4Ldap;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.AssertUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.*;

import static com.middleware.zeus.common.constants.LdapConfigConstant.*;

/**
 * @author liyinlong
 * @description ldap方式实现认证接口
 * @since 2022/3/9 5:17 下午
 */
@Service
public class AuthManager4LdapImpl implements AuthManager4Ldap {

    private static Logger LOGGER = LoggerFactory.getLogger(AuthManager4LdapImpl.class);

    @Autowired
    private UserService userService;

    @Override
    public UserDto auth(String userName, String password, LdapConfigDto ldapConfigDto) throws Exception {
        AssertUtil.notBlank(userName, DictEnum.USERNAME);
        AssertUtil.notBlank(password, DictEnum.PASSWORD);
        if (StringUtils.isEmpty(ldapConfigDto.getUrl())){
            throw new BusinessException(ErrorMessage.LDAP_INCOMPLETE_PARAMETERS);
        }

        Map<String, String> userAttributes = this.getUserFromLdap(userName, ldapConfigDto);
        // 对ldap认证通过的用户,判断是否已经记录,如果没有，则记录用户,并返回该用户
        if (userAttributes.get("userPassword") != null && !userAttributes.get("userPassword").equals(password)) {
            throw new BusinessException(ErrorMessage.LOGIN_FAILED);
        }
        return saveUserInfo(userName, password, ldapConfigDto, userAttributes);
    }

    private Map<String, String> getUserFromLdap(String username, LdapConfigDto ldapConfigDto) {
        LdapTemplate template = LdapServiceImpl.getTemplate(ldapConfigDto);
        String filter = "(&" + "(" + ldapConfigDto.getObjectType() + ")" +
                "(" + ldapConfigDto.getAccountType() + "=" + username + ")" +
                (ObjectUtil.isNull(ldapConfigDto.getFilterCondition()) ? "" : ldapConfigDto.getFilterCondition()) + ")";
        List<Map<String, String>> infoList = template.search("", filter, (AttributesMapper<Map<String, String>>) attributes -> {
            Map<String, String> map = new HashMap<>();
            for (Enumeration<? extends Attribute> e = attributes.getAll(); e.hasMoreElements();) {
                Attribute attribute = e.nextElement();
                map.put(attribute.getID(), attribute.get().toString());
                if (attribute.getID().equals("userPassword") && attribute.get() instanceof byte[]) {
                    map.put(attribute.getID(), new String((byte[])attribute.get()));
                }
            }
            return map;
        });
        if (CollectionUtils.isEmpty(infoList)) {
            throw new BusinessException(ErrorMessage.LDAP_USER_NOT_EXIST);
        }
        return infoList.get(0);
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
        String phoneStr = ldapConfigDto.getPhone() == null ?  LDAP_MOBILE : ldapConfigDto.getPhone();
        String mailStr = ldapConfigDto.getMail() == null ? LDAP_MAIL : ldapConfigDto.getMail();
        if (user == null) {
            user = new BeanUser();
            user.setUserName(userName);
            user.setPassword(PasswordUtils.md5(password));
            user.setPhone(userAttributes.get(ldapConfigDto.getPhone()));
            user.setEmail(userAttributes.get(phoneStr));
            user.setCreateTime(new Date());
            user.setAliasName(userAttributes.get(ldapConfigDto.getDisplayName()));
            userService.create(user);
            return userService.getUserDto(userName, true);
        }
        boolean userInfoChanged = false;
        if (StringUtils.isNotBlank(userAttributes.get(mailStr)) && !userAttributes.get(mailStr).equals(user.getEmail())) {
            user.setEmail(userAttributes.get(mailStr));
            userInfoChanged = true;
        }
        if (StringUtils.isNotBlank(userAttributes.get(phoneStr)) && !userAttributes.get(phoneStr).equals(user.getPhone())) {
            user.setPhone(userAttributes.get(phoneStr));
            userInfoChanged = true;
        }
        if (StringUtils.isNotBlank(userAttributes.get(ldapConfigDto.getDisplayName())) && !userAttributes.get(ldapConfigDto.getDisplayName()).equals(user.getAliasName())) {
            user.setAliasName(userAttributes.get(ldapConfigDto.getDisplayName()));
            userInfoChanged = true;
        }
        if (!user.getPassword().equals(PasswordUtils.md5(password))) {
            user.setPassword(PasswordUtils.md5(password));
            userInfoChanged = true;
        }

        if (userInfoChanged) {
            userService.update(user);
        }
        return userService.getUserDto(userName, true);
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
