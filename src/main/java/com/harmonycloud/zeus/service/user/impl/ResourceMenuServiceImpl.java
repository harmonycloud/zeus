package com.harmonycloud.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.zeus.bean.user.BeanResourceMenu;
import com.harmonycloud.zeus.dao.user.BeanResourceMenuMapper;
import com.harmonycloud.zeus.service.user.ResourceMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public List<BeanResourceMenu> list(List<Integer> ids) {
        QueryWrapper<BeanResourceMenu> resourceMenuWrapper = new QueryWrapper<BeanResourceMenu>().in("id", ids);
        return beanResourceMenuMapper.selectList(resourceMenuWrapper);
    }
}
