package com.harmonycloud.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.zeus.bean.user.BeanResourceMenu;
import com.harmonycloud.zeus.dao.user.BeanResourceMenuMapper;
import com.harmonycloud.zeus.service.user.ResourceMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2021/7/29 9:41 上午
 */
@Service
@Slf4j
public class ResourceMenuServiceImpl implements ResourceMenuService {

    @Autowired
    private BeanResourceMenuMapper beanResourceMenuMapper;

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
}
