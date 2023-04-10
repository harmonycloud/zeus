package com.middleware.zeus.service.user;

import com.middleware.caas.common.model.user.ResourceMenuDto;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/7/29 9:41 上午
 */
public interface ResourceMenuService {

    /**
     * 获取菜单详细信息
     *
     * @return List<BeanResourceMenu>
     */
    List<ResourceMenuDto> list();

    /**
     * 获取菜单详细信息
     * @param ids 菜单id列表
     *
     * @return List<BeanResourceMenu>
     */
    List<ResourceMenuDto> list(List<Integer> ids);

}
