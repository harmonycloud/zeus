package com.harmonycloud.zeus.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.zeus.bean.BeanSystemConfig;
import com.harmonycloud.zeus.dao.BeanSystemConfigMapper;
import com.harmonycloud.zeus.service.system.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.harmonycloud.caas.common.constants.NameConstant.CONFIG_NAME;

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
        String username = CurrentUserRepository.getUser().getUsername();
        BeanSystemConfig config = new BeanSystemConfig();
        config.setConfigName(name);
        config.setConfigValue(value);
        config.setCreateUser(username);
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
    public BeanSystemConfig getConfig(String name) {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<BeanSystemConfig>().eq(CONFIG_NAME, name);
        return beanSystemConfigMapper.selectOne(wrapper);
    }

    @Override
    public BeanSystemConfig getConfigForUpdate(String name) {
        QueryWrapper<BeanSystemConfig> wrapper =
            new QueryWrapper<BeanSystemConfig>().eq(CONFIG_NAME, name).last("for update");
        return beanSystemConfigMapper.selectOne(wrapper);
    }
}
