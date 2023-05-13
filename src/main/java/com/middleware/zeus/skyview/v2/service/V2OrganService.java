package com.middleware.zeus.skyview.v2.service;

import com.middleware.zeus.common.model.ResourceQuotaDo;
import com.middleware.zeus.common.model.middleware.Namespace;
import com.middleware.zeus.common.model.user.OrganizationDto;
import com.middleware.zeus.common.model.user.UserDto;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/23 8:18 下午
 */
public interface V2OrganService {

    /**
     * 查询租户列表
     *
     * @return List<ResourceQuotaDo>
     */
    List<OrganizationDto> list();

    /**
     * 查询租户详情
     *
     * @return List<ResourceQuotaDo>
     */
    OrganizationDto get(String organId);

    /**
     * 查询分区列表
     * @param organId 组织id
     *
     * @return List<Namespace>
     */
    List<Namespace> nsList(String organId);


    /**
     * 查询租户下资源配额
     * @param organId 组织id
     *
     * @return List<ResourceQuotaDo>
     */
    List<ResourceQuotaDo> quotas(String organId);

    /**
     * 查询租户下用户列表
     * @param organId 组织id
     *
     * @return List<ResourceQuotaDo>
     */
    List<UserDto> userList(String organId);

}
