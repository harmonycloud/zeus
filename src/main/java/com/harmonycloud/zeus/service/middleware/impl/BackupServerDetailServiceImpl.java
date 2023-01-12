package com.harmonycloud.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.BackupServerDetailDTO;
import com.harmonycloud.zeus.bean.BeanBackupServerDetail;
import com.harmonycloud.zeus.dao.BeanBackupServerDetailMapper;
import com.harmonycloud.zeus.dao.BeanBackupServerMapper;
import com.harmonycloud.zeus.service.middleware.BackupServerDetailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2023/1/11 11:42 上午
 */
@Service
@Slf4j
public class BackupServerDetailServiceImpl implements BackupServerDetailService {

    @Autowired
    private BeanBackupServerDetailMapper backupServerDetailMapper;

    @Override
    public List<BackupServerDetailDTO> selectBackupServerDetailDTOByServerId(Integer backupServerId) {
        QueryWrapper<BeanBackupServerDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("backup_server_id", backupServerId);
        List<BeanBackupServerDetail> serverDetails = backupServerDetailMapper.selectList(wrapper);
        return serverDetails.stream().map(beanBackupServerDetail -> {
            BackupServerDetailDTO backupServerDetailDTO = new BackupServerDetailDTO();
            BeanUtil.copyProperties(beanBackupServerDetail, backupServerDetailDTO);
            return backupServerDetailDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public void create(int serverId, List<BackupServerDetailDTO> serverDetailDTOS) {
        for (BackupServerDetailDTO serverDetailDTO : serverDetailDTOS) {
            BeanBackupServerDetail serverDetail = new BeanBackupServerDetail();
            BeanUtil.copyProperties(serverDetailDTO, serverDetail);
            serverDetail.setBackupServerId(serverId);
            serverDetail.setCreateTime(new Date());
            backupServerDetailMapper.insert(serverDetail);
        }
    }

    @Override
    public void update(List<BackupServerDetailDTO> serverDetailDTOS) {
        for (BackupServerDetailDTO serverDetailDTO : serverDetailDTOS) {
            BeanBackupServerDetail serverDetail = new BeanBackupServerDetail();
            BeanUtil.copyProperties(serverDetail, serverDetailDTO);
            backupServerDetailMapper.updateById(serverDetail);
        }
    }

    @Override
    public void deleteByServerId(Integer serverId) {
        QueryWrapper<BeanBackupServerDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("server_id", serverId);
        backupServerDetailMapper.delete(wrapper);
    }

}
