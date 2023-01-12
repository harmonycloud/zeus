package com.harmonycloud.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.model.BackupPositionDTO;
import com.harmonycloud.caas.common.model.BackupServerDTO;
import com.harmonycloud.zeus.bean.BeanBackupPosition;
import com.harmonycloud.zeus.bean.user.BeanProject;
import com.harmonycloud.zeus.dao.BeanBackupPositionMapper;
import com.harmonycloud.zeus.service.middleware.BackupPositionService;
import com.harmonycloud.zeus.service.middleware.BackupServerService;
import com.harmonycloud.zeus.service.user.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2023/1/12 8:00 上午
 */
@Service
@Transactional
public class BackupPositionServiceImpl implements BackupPositionService {

    @Autowired
    private BeanBackupPositionMapper backupPositionMapper;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private BackupServerService backupServerService;

    /**
     * 查询指定备份服务器的全部备份位置
     * @param backupServerId
     * @return
     */
    @Override
    public List<BackupPositionDTO> selectBackupPositionDTOList(Integer backupServerId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("backup_server_id", backupServerId);
        List<BeanBackupPosition> beanBackupPositions = backupPositionMapper.selectList(wrapper);
        return convert(beanBackupPositions);
    }

    /**
     * 查询指定项目的全部备份位置
     * @param projectId
     * @return
     */
    @Override
    public List<BackupPositionDTO> selectBackupPositionDTOList(String projectId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        List<BeanBackupPosition> beanBackupPositions = backupPositionMapper.selectList(wrapper);
        return convert(beanBackupPositions);
    }

    /**
     * 查询项目备份位置列表
     * @param projectId
     * @return
     */
    @Override
    public List<BackupServerDTO> selectBackupServerDTOList(String projectId) {
        return backupServerService.listBackupPosition(projectId);
    }

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

    // 转换数据类型
    private List<BackupPositionDTO> convert(List<BeanBackupPosition> beanBackupPositions) {
        return beanBackupPositions.stream().map(beanBackupPosition -> {
            BackupPositionDTO backupPositionDTO = new BackupPositionDTO();
            BeanUtil.copyProperties(beanBackupPosition, backupPositionDTO);
            BeanProject beanProject = projectService.get(beanBackupPosition.getProjectId());
            backupPositionDTO.setProjectName(beanProject.getName());
            return backupPositionDTO;
        }).collect(Collectors.toList());
    }

}
