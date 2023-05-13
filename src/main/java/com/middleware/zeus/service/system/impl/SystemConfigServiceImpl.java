package com.middleware.zeus.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.bean.BeanSystemConfig;
import com.middleware.zeus.dao.BeanSystemConfigMapper;
import com.middleware.zeus.service.system.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.middleware.zeus.common.constants.NameConstant.CONFIG_NAME;

/**
 * @author xutianhong
 * @Date 2022/11/1 3:01 下午
 */
@Service
@Slf4j
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private BeanSystemConfigMapper beanSystemConfigMapper;

    @Override
    public void addConfig(String name, String value) {
        String username = null;
        CurrentUser user = CurrentUserRepository.getUser();
        if (user != null){
            username = user.getUsername();
        }
        BeanSystemConfig config = new BeanSystemConfig();
        config.setConfigName(name);
        config.setConfigValue(value);
        config.setCreateUser(username);
        config.setCreateTime(LocalDateTime.now());
        beanSystemConfigMapper.insert(config);
    }

    @Override
    public void updateConfig(String name, String value) {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<BeanSystemConfig>().eq(CONFIG_NAME, name);
        BeanSystemConfig config = new BeanSystemConfig();
        config.setConfigName(name);
        config.setConfigValue(value);
        beanSystemConfigMapper.update(config, wrapper);
    }

    @Override
    public void saveConfig(String name, String value) {
        BeanSystemConfig config = getConfig(name);
        if (config == null) {
            addConfig(name, value);
        } else {
            updateConfig(name, value);
        }
    }

    @Override
    public BeanSystemConfig getConfig(String name) {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<BeanSystemConfig>().eq(CONFIG_NAME, name);
        return beanSystemConfigMapper.selectOne(wrapper);
    }

    @Override
    public BeanSystemConfig getConfigForUpdate(String name) {
        try {
            QueryWrapper<BeanSystemConfig> wrapper =
                    new QueryWrapper<BeanSystemConfig>().eq(CONFIG_NAME, name).last("for update");
            return beanSystemConfigMapper.selectOne(wrapper);
        } catch (Exception e){
            log.error("select for update 获取锁超时, {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String name) {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<BeanSystemConfig>().eq("config_name", name);
        beanSystemConfigMapper.delete(wrapper);
    }
}
