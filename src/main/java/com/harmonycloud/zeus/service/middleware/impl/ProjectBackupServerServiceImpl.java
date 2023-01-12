package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.zeus.bean.BeanProjectBackupServer;
import com.harmonycloud.zeus.dao.BeanProjectBackupServerMapper;
import com.harmonycloud.zeus.service.middleware.ProjectBackupServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/11 3:04 下午
 */
@Service
@Transactional
public class ProjectBackupServerServiceImpl implements ProjectBackupServerService {

    @Autowired
    private BeanProjectBackupServerMapper projectBackupServerMapper;

    @Override
    public List<BeanProjectBackupServer> listByBackupServerId(Integer serverId) {
        QueryWrapper<BeanProjectBackupServer> wrapper = new QueryWrapper<>();
        wrapper.eq("server_id", serverId);
        return projectBackupServerMapper.selectList(wrapper);
    }


}
