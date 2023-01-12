package com.harmonycloud.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.BackupServerDTO;
import com.harmonycloud.caas.common.model.middleware.BackupServerDetailDTO;
import com.harmonycloud.zeus.bean.BeanBackupServer;
import com.harmonycloud.zeus.bean.BeanBackupServerDetail;
import com.harmonycloud.zeus.dao.BeanBackupServerDetailMapper;
import com.harmonycloud.zeus.dao.BeanBackupServerMapper;
import com.harmonycloud.zeus.service.middleware.BackupServerDetailService;
import com.harmonycloud.zeus.service.middleware.BackupServerService;
import com.harmonycloud.zeus.service.middleware.ProjectBackupServerService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/9 2:29 下午
 */
@Service
@Slf4j
public class BackupServerServiceImpl implements BackupServerService {

    @Autowired
    private BeanBackupServerMapper backupServerMapper;
    @Autowired
    private BackupServerDetailService backupServerDetailService;
    @Autowired
    private ProjectBackupServerService projectBackupServerService;

    @Override
    public List<BackupServerDTO> list(String clusterId) {

        return null;
    }

    @Override
    public BeanBackupServer get(Integer id) {
        return backupServerMapper.selectById(id);
    }

    @Override
    public void create(BackupServerDTO backupServerDTO) {
        BeanBackupServer backupServer = new BeanBackupServer();
        backupServer.setName(backupServerDTO.getName());
        backupServer.setType(backupServerDTO.getType());
        backupServer.setCreateTime(new Date());
        backupServerMapper.insert(backupServer);

        Integer serverId = backupServer.getId();
        List<BackupServerDetailDTO> serverDetailList = backupServerDTO.getServerDetailList();
        if (!CollectionUtils.isEmpty(serverDetailList)) {
            backupServerDetailService.create(serverId, serverDetailList);
        } else {
            throw new BusinessException(ErrorMessage.PARAMETER_NOT_COMPLETE);
        }
    }

    @Override
    public void update(BackupServerDTO backupServerDTO) {
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        wrapper.eq("id", backupServerDTO.getId());
        BeanBackupServer beanBackupServer = new BeanBackupServer();
        BeanUtil.copyProperties(backupServerDTO, beanBackupServer);
        backupServerMapper.updateById(beanBackupServer);

    }

    @Override
    public void allocate(Integer id, String clusterId) {
        // TODO 先校验改备份服务器是否已被项目关联，若已关联项目，则需要先解除关联
        BeanBackupServer beanBackupServer = get(id);
        beanBackupServer.setClusterId(clusterId);
        backupServerMapper.updateById(beanBackupServer);
    }

    @Override
    public void delete(Integer id) {
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        backupServerMapper.delete(wrapper);
        backupServerDetailService.deleteByServerId(id);
    }


}
