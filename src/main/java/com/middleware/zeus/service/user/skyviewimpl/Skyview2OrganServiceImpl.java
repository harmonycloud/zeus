package com.middleware.zeus.service.user.skyviewimpl;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.ResourceQuotaDo;
import com.middleware.zeus.common.model.user.OrganizationDto;
import com.middleware.zeus.common.model.user.OrganizationQuota;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.service.user.OrganizationService;
import com.middleware.zeus.service.user.abstractService.AbstractOrganizationService;
import com.middleware.zeus.skyview.v2.service.V2OrganService;

/**
 * @author xutianhong
 * @Date 2023/3/24 2:22 下午
 */
@Skyview(target = "skyview2")
public class Skyview2OrganServiceImpl extends AbstractOrganizationService implements OrganizationService {
    
    @Autowired
    private V2OrganService v2OrganService;

    @Override
    public void add(OrganizationDto organizationDto) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void update(OrganizationDto organizationDto) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public List<OrganizationDto> list(String keyword) {
        List<OrganizationDto> organizationDtoList = v2OrganService.list();
        if (StringUtils.isNotEmpty(keyword)) {
            organizationDtoList = organizationDtoList.stream()
                .filter(organizationDto -> StringUtils.containsIgnoreCase(organizationDto.getName(), keyword))
                .collect(Collectors.toList());
        }
        return organizationDtoList;
    }

    @Override
    public OrganizationDto get(String organId) {
        return v2OrganService.get(organId);
    }

    @Override
    public void delete(String organId) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void allocateQuota(OrganizationQuota organizationQuota) {
        // 记录备份服务器
        // todo 校验备份服务器是否已被使用
        if (!CollectionUtils.isEmpty(organizationQuota.getBackupServerDTOList())) {
            allocateBackupServer(organizationQuota);
        }
    }

    @Override
    public List<ResourceQuotaDo> getStorageQuota(String organId, String clusterIds, boolean detail) {
        return v2OrganService.quotas(organId);
    }

    @Override
    public void removeStorageQuota(String organId, String storageId, String clusterId) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public List<ResourceQuotaDo> getCpuMemoryQuota(String organId, boolean detail) {
        return v2OrganService.quotas(organId);
    }

    @Override
    public void removeCpuMemoryQuota(String organId, String clusterId) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public List<UserDto> listOrganUser(String organId, Boolean allocatable) {
        return v2OrganService.userList(organId);
    }

    @Override
    public void addOrganUser(OrganizationDto organizationDto) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void updateOrganUser(String organId, String username, Integer roleId) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void deleteOrganUser(String organId, String username) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void clear(String clusterId) {
    }
}
