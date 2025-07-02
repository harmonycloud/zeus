package com.middleware.zeus.service.user.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.bean.BeanSystemConfig;
import com.middleware.zeus.common.constants.LdapConfigConstant;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.LdapConfigDto;
import com.middleware.zeus.dao.BeanSystemConfigMapper;
import com.middleware.zeus.service.user.AuthManager4Ldap;
import com.middleware.zeus.service.user.LdapService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liyinlong
 * @since 2022/3/10 4:48 下午
 */
@Slf4j
@Service
public class LdapServiceImpl implements LdapService {

    @Autowired
    private BeanSystemConfigMapper ldapConfigMapper;
    @Autowired
    private AuthManager4Ldap authManager4Ldap;

    @Override
    public void save(BeanSystemConfig ldapConfig) {
        ldapConfigMapper.insert(ldapConfig);
    }

    @Override
    public BeanSystemConfig findByConfigName(String configName) {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_name", configName);
        return ldapConfigMapper.selectOne(wrapper);
    }

    @Override
    public void saveSingle(String config_name, String config_value) {
        BeanSystemConfig ldapConfig = new BeanSystemConfig();
        ldapConfig.setConfigName(config_name);
        ldapConfig.setConfigValue(config_value);
        BeanSystemConfig config = findByConfigName(config_name);
        if(config == null){
            ldapConfigMapper.insert(ldapConfig);
            return;
        }
        config.setConfigValue(config_value);
        ldapConfigMapper.updateById(config);
    }

    @Override
    public void save(LdapConfigDto ldapConfigDto) {
        this.connectionCheck(ldapConfigDto);
        saveSingle(LdapConfigConstant.URL, ldapConfigDto.getUrl());
        saveSingle(LdapConfigConstant.BASE, ldapConfigDto.getBase());
        saveSingle(LdapConfigConstant.USERNAME, ldapConfigDto.getUsername());
        saveSingle(LdapConfigConstant.PASSWORD, ldapConfigDto.getPassword());
        saveSingle(LdapConfigConstant.OBJECT_TYPE, ldapConfigDto.getObjectType());
        saveSingle(LdapConfigConstant.ACCOUNT_TYPE, ldapConfigDto.getAccountType());
        saveSingle(LdapConfigConstant.DISPLAY_NAME, ldapConfigDto.getDisplayName());
        saveSingle(LdapConfigConstant.MAIL, ldapConfigDto.getMail());
        saveSingle(LdapConfigConstant.PHONE, ldapConfigDto.getPhone());
        saveSingle(LdapConfigConstant.FILTER_CONDITION, ldapConfigDto.getFilterCondition());
    }

    @Override
    public void update(BeanSystemConfig ldapConfig) {
        ldapConfigMapper.updateById(ldapConfig);
    }

    @Override
    public LdapConfigDto queryLdapDetail() {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<>();
        List<BeanSystemConfig> ldapConfigs = ldapConfigMapper.selectList(wrapper);
        LdapConfigDto ldapConfigDto = new LdapConfigDto();
        ldapConfigs.forEach(config -> {
            switch (config.getConfigName()) {
                case LdapConfigConstant.URL:
                    ldapConfigDto.setUrl(config.getConfigValue());
                    break;
                case LdapConfigConstant.BASE:
                    ldapConfigDto.setBase(config.getConfigValue());
                    break;
                case LdapConfigConstant.USERNAME:
                    ldapConfigDto.setUsername(config.getConfigValue());
                    break;
                case LdapConfigConstant.PASSWORD:
                    ldapConfigDto.setPassword(config.getConfigValue());
                    break;
                case LdapConfigConstant.OBJECT_TYPE:
                    ldapConfigDto.setObjectType(config.getConfigValue());
                    break;
                case LdapConfigConstant.ACCOUNT_TYPE:
                    ldapConfigDto.setAccountType(config.getConfigValue());
                    break;
                case LdapConfigConstant.DISPLAY_NAME:
                    ldapConfigDto.setDisplayName(config.getConfigValue());
                    break;
                case LdapConfigConstant.MAIL:
                    ldapConfigDto.setMail(config.getConfigValue());
                    break;
                case LdapConfigConstant.PHONE:
                    ldapConfigDto.setPhone(config.getConfigValue());
                    break;
                case LdapConfigConstant.FILTER_CONDITION:
                    ldapConfigDto.setFilterCondition(config.getConfigValue());
                    break;
            }
        });
        return ldapConfigDto;
    }

    @Override
    public void connectionCheck(LdapConfigDto ldapConfigDto) {
        paramCheck(ldapConfigDto);
        try {
            LdapContextSource contextSource = getLdapContextSource(ldapConfigDto);
            contextSource.afterPropertiesSet();
            LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
            ldapTemplate.setIgnorePartialResultException(true);
            contextSource.getReadOnlyContext();
        } catch (Exception e) {
            log.error("ldap连接失败", e);
            throw new BusinessException(ErrorMessage.LDAP_SERVER_CONNECT_FAILED);
        }
    }

    private static @NotNull LdapContextSource getLdapContextSource(LdapConfigDto ldapConfigDto) {
        LdapContextSource contextSource = new LdapContextSource();
        Map<String, Object> config = new HashMap<>();
        contextSource.setUrl(ldapConfigDto.getUrl());
        contextSource.setBase(ldapConfigDto.getBase());
        contextSource.setUserDn(ldapConfigDto.getUsername());
        contextSource.setPassword(ldapConfigDto.getPassword());

        //  解决乱码
        config.put("java.naming.ldap.attributes.binary", "objectGUID");
        //关闭ldap pooling
        contextSource.setPooled(false);
        contextSource.setBaseEnvironmentProperties(config);
        return contextSource;
    }

    public static LdapTemplate getTemplate(LdapConfigDto ldapConfigDto) {
        LdapContextSource contextSource = getLdapContextSource(ldapConfigDto);
        contextSource.afterPropertiesSet();
        return new LdapTemplate(contextSource);
    }

    public void paramCheck(LdapConfigDto ldapConfigDto) {
        if (StringUtils.isAnyBlank(ldapConfigDto.getUrl(), ldapConfigDto.getBase(), ldapConfigDto.getUsername(),
            ldapConfigDto.getPassword(), ldapConfigDto.getObjectType(), ldapConfigDto.getAccountType(),
            ldapConfigDto.getDisplayName())) {
            throw new BusinessException(ErrorMessage.LDAP_INCOMPLETE_PARAMETERS);
        }
    }

}
