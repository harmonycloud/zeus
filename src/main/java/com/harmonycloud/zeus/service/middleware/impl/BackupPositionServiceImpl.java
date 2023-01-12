package com.harmonycloud.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.harmonycloud.caas.common.model.BackupPositionDTO;
import com.harmonycloud.zeus.bean.BeanBackupPosition;
import com.harmonycloud.zeus.dao.BeanBackupPositionMapper;
import com.harmonycloud.zeus.service.middleware.BackupPositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author liyinlong
 * @since 2023/1/12 8:00 上午
 */
@Service
@Transactional
public class BackupPositionServiceImpl implements BackupPositionService {

    @Autowired
    private BeanBackupPositionMapper backupPositionMapper;

    @Override
    public void create(BackupPositionDTO backupPositionDTO) {
        BeanBackupPosition backupPosition = new BeanBackupPosition();
        BeanUtil.copyProperties(backupPositionDTO, backupPosition);
        backupPositionMapper.insert(backupPosition);
    }

    @Override
    public void delete(Integer id) {
        backupPositionMapper.deleteById(id);
    }

}
