package com.harmonycloud.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.constants.LdapConfigConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.LdapConfigDto;
import com.harmonycloud.zeus.bean.BeanSystemConfig;
import com.harmonycloud.zeus.dao.BeanSystemConfigMapper;
import com.harmonycloud.zeus.service.user.LdapService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2022/3/10 4:48 下午
 */
@Slf4j
@Service
public class LdapServiceImpl implements LdapService {

    @Autowired
    BeanSystemConfigMapper ldapConfigMapper;

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
        saveSingle(LdapConfigConstant.IS_ON, String.valueOf(ldapConfigDto.getIsOn()));
        saveSingle(LdapConfigConstant.IP, ldapConfigDto.getIp());
        saveSingle(LdapConfigConstant.BASE, ldapConfigDto.getBase());
        saveSingle(LdapConfigConstant.OBJECT_CLASS, ldapConfigDto.getObjectClass());
        saveSingle(LdapConfigConstant.PASSWORD, ldapConfigDto.getPassword());
        saveSingle(LdapConfigConstant.PORT, String.valueOf(ldapConfigDto.getPort()));
        saveSingle(LdapConfigConstant.USERDN, ldapConfigDto.getUserdn());
        saveSingle(LdapConfigConstant.SEARCH_ATTRIBUTE, ldapConfigDto.getSearchAttribute());
        saveSingle(LdapConfigConstant.DISPLAY_NAME_ATTRIBUTE, ldapConfigDto.getDisplayNameAttribute());
    }

    @Override
    public void update(BeanSystemConfig ldapConfig) {
        ldapConfigMapper.updateById(ldapConfig);
    }

    @Override
    public void disable() {
        BeanSystemConfig isOnConfig = findByConfigName(LdapConfigConstant.IS_ON);
        if (isOnConfig != null) {
            isOnConfig.setConfigValue(LdapConfigConstant.LDAP_DISABLE);
            update(isOnConfig);
        }
    }

    @Override
    public LdapConfigDto queryLdapDetail() {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<>();
        List<BeanSystemConfig> ldapConfigs = ldapConfigMapper.selectList(wrapper);
        LdapConfigDto ldapConfigDto = new LdapConfigDto();
        ldapConfigs.forEach(config -> {
            switch (config.getConfigName()) {
                case LdapConfigConstant.IS_ON:
                    ldapConfigDto.setIsOn(Integer.parseInt(config.getConfigValue()));
                    break;
                case LdapConfigConstant.IP:
                    ldapConfigDto.setIp(config.getConfigValue());
                    break;
                case LdapConfigConstant.BASE:
                    ldapConfigDto.setBase(config.getConfigValue());
                    break;
                case LdapConfigConstant.OBJECT_CLASS:
                    ldapConfigDto.setObjectClass(config.getConfigValue());
                    break;
                case LdapConfigConstant.PASSWORD:
                    ldapConfigDto.setPassword(config.getConfigValue());
                    break;
                case LdapConfigConstant.PORT:
                    ldapConfigDto.setPort(config.getConfigValue());
                    break;
                case LdapConfigConstant.USERDN:
                    ldapConfigDto.setUserdn(config.getConfigValue());
                    break;
                case LdapConfigConstant.SEARCH_ATTRIBUTE:
                    ldapConfigDto.setSearchAttribute(config.getConfigValue());
                    break;
                case LdapConfigConstant.DISPLAY_NAME_ATTRIBUTE:
                    ldapConfigDto.setDisplayNameAttribute(config.getConfigValue());
                    break;
            }
        });
        return ldapConfigDto;
    }

    @Override
    public void connectionCheck(LdapConfigDto ldapConfigDto) {
        paramCheck(ldapConfigDto);
        try {
            log.info("开始ldap连接测试", ldapConfigDto);
            LdapTemplate template = getTemplate(ldapConfigDto);
            AndFilter andFilter = new AndFilter();
            andFilter.and(new EqualsFilter("objectClass", ldapConfigDto.getObjectClass()));
            andFilter.and(new LikeFilter(ldapConfigDto.getSearchAttribute(), "*"));
            andFilter.and(new LikeFilter(ldapConfigDto.getDisplayNameAttribute(), "*"));
            List<Map<String, String>> results = template.search("", andFilter.encode(), (ContextMapper<Map<String, String>>) o -> null);
            if (!(results.size() > 0)) {
                throw new BusinessException(ErrorMessage.LDAP_SERVER_CONNECT_FAILED);
            }
        } catch (Exception e) {
            log.error("ldap连接失败", e);
            throw new BusinessException(ErrorMessage.LDAP_SERVER_CONNECT_FAILED);
        }
    }

    public static LdapTemplate getTemplate(LdapConfigDto ldapConfigDto) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl("ldap://" + ldapConfigDto.getIp() + ":" + ldapConfigDto.getPort() + "");
        contextSource.setBase(ldapConfigDto.getBase());
        contextSource.setUserDn(ldapConfigDto.getUserdn());
        contextSource.setPassword(ldapConfigDto.getPassword());
        contextSource.afterPropertiesSet();
        LdapTemplate template = new LdapTemplate();
        template.setContextSource(contextSource);
        return template;
    }

    public void paramCheck(LdapConfigDto ldapConfigDto) {
        if (StringUtils.isAnyBlank(ldapConfigDto.getIp(), ldapConfigDto.getPort(), ldapConfigDto.getPassword(),
                ldapConfigDto.getUserdn(), ldapConfigDto.getBase(), ldapConfigDto.getPassword(), ldapConfigDto.getSearchAttribute(),
                ldapConfigDto.getDisplayNameAttribute(), ldapConfigDto.getObjectClass(), ldapConfigDto.getSearchAttribute())) {
            throw new BusinessException(ErrorMessage.LDAP_INCOMPLETE_PARAMETERS);
        }
    }

}
