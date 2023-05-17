package com.middleware.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.model.user.ResourceMenuDto;
import com.middleware.zeus.bean.user.BeanResourceMenu;
import com.middleware.zeus.dao.user.BeanResourceMenuMapper;
import com.middleware.zeus.service.user.ResourceMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2021/7/29 9:41 上午
 */
@Service
@Slf4j
public class ResourceMenuServiceImpl implements ResourceMenuService {

    @Value("${system.disasterRecovery:true}")
    private String disasterEnable;

    @Autowired
    private BeanResourceMenuMapper beanResourceMenuMapper;

    @Override
    public List<ResourceMenuDto> list() {
        QueryWrapper<BeanResourceMenu> resourceMenuWrapper = new QueryWrapper<>();
        List<BeanResourceMenu> beanResourceMenuList = beanResourceMenuMapper.selectList(resourceMenuWrapper);
        return beanResourceMenuList.stream().map(beanResourceMenu -> {
            ResourceMenuDto resourceMenuDto = new ResourceMenuDto();
            BeanUtils.copyProperties(beanResourceMenu, resourceMenuDto);
            return resourceMenuDto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ResourceMenuDto> list(List<Integer> ids) {
        QueryWrapper<BeanResourceMenu> resourceMenuWrapper = new QueryWrapper<>();
        List<BeanResourceMenu> beanResourceMenuList = beanResourceMenuMapper.selectList(resourceMenuWrapper);
        return beanResourceMenuList.stream().map(beanResourceMenu -> {
            ResourceMenuDto resourceMenuDto = new ResourceMenuDto();
            BeanUtils.copyProperties(beanResourceMenu, resourceMenuDto);
            if (ids.stream().anyMatch(id -> beanResourceMenu.getId().equals(id))) {
                resourceMenuDto.setOwn(true);
            }else {
                resourceMenuDto.setOwn(false);
            }
            return resourceMenuDto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ResourceMenuDto> convertMenu(List<ResourceMenuDto> resourceMenuDtoList) {
        Map<Integer, List<ResourceMenuDto>> resourceMenuDtoMap =
                resourceMenuDtoList.stream().collect(Collectors.groupingBy(ResourceMenuDto::getParentId));
        List<ResourceMenuDto> firstMenuList = resourceMenuDtoMap.get(0);
        // 空菜单返回
        if (CollectionUtils.isEmpty(resourceMenuDtoList) || CollectionUtils.isEmpty(firstMenuList)) {
            return new ArrayList<>();
        }
        resourceMenuDtoMap.remove(0);
        firstMenuList.forEach(firstMenu -> {
            if (!resourceMenuDtoMap.containsKey(firstMenu.getWeight())) {
                return;
            }
            firstMenu.setSubMenu(resourceMenuDtoMap.get(firstMenu.getWeight()));
            Collections.sort(firstMenu.getSubMenu());
        });
        Collections.sort(firstMenuList);
        return firstMenuList;
    }
}
