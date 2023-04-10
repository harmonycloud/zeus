package com.middleware.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.common.model.user.UserRole;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.bean.user.BeanRoleAuthority;
import com.middleware.zeus.dao.user.BeanRoleAuthorityMapper;
import com.middleware.zeus.service.user.ResourceMenuRoleService;
import com.middleware.zeus.service.user.RoleAuthorityService;
import com.middleware.zeus.service.user.UserRoleService;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2022/3/28 5:21 下午
 */
@Service
@Slf4j
public class RoleAuthorityServiceImpl implements RoleAuthorityService {

    @Autowired
    private BeanRoleAuthorityMapper beanRoleAuthorityMapper;
    @Autowired
    private ResourceMenuRoleService resourceMenuRoleService;
    @Autowired
    private UserService userService;

    @Override
    public void insert(Integer roleId, String type, String power) {
        BeanRoleAuthority beanRoleAuthority = new BeanRoleAuthority();
        beanRoleAuthority.setRoleId(roleId);
        beanRoleAuthority.setType(type);
        beanRoleAuthority.setPower(power);
        beanRoleAuthorityMapper.insert(beanRoleAuthority);
    }

    @Override
    public void delete(Integer roleId) {
        QueryWrapper<BeanRoleAuthority> wrapper = new QueryWrapper<BeanRoleAuthority>().eq("role_id", roleId);
        beanRoleAuthorityMapper.delete(wrapper);
    }

    @Override
    public List<BeanRoleAuthority> list(Integer roleId) {
        QueryWrapper<BeanRoleAuthority> wrapper = new QueryWrapper<>();
        if (roleId != null){
            wrapper.eq("role_id", roleId);
        }
        return beanRoleAuthorityMapper.selectList(wrapper);
    }

    @Override
    public void update(Integer roleId, Map<String, String> power) {
        boolean ops = false;
        // 删除当前权限
        QueryWrapper<BeanRoleAuthority> wrapper = new QueryWrapper<BeanRoleAuthority>().eq("role_id", roleId);
        beanRoleAuthorityMapper.delete(wrapper);
        // 添加新权限
        for (String key : power.keySet()){
            if (Integer.parseInt(power.get(key).split("")[1]) == 1){
                ops = true;
            }
            insert(roleId, key, power.get(key));
        }
        // 判断该角色是否拥有运维权限
        resourceMenuRoleService.updateOpsMenu(roleId, ops);
    }

    @Override
    public Boolean checkExistByType(String type) {
        QueryWrapper<BeanRoleAuthority> wrapper = new QueryWrapper<BeanRoleAuthority>().eq("type", type);
        List<BeanRoleAuthority> beanRoleAuthorityList = beanRoleAuthorityMapper.selectList(wrapper);
        return !CollectionUtils.isEmpty(beanRoleAuthorityList);
    }

    @Override
    public Boolean checkOps(String roleId, String type) {
        boolean flag = false;
        String organId = RequestUtil.getOrganId();
        String projectId = RequestUtil.getProjectId();
        if (StringUtils.isEmpty(organId) || StringUtils.isEmpty(projectId)) {
            return false;
        }
        if (StringUtils.isEmpty(roleId)) {
            String username = CurrentUserRepository.getUser().getUsername();
            UserDto userDto = userService.getUserDto(username);
            if (userDto.getIsAdmin()) {
                return true;
            }
            List<UserRole> userRoleList = userDto.getUserRoleList().stream()
                .filter(userRole -> StringUtils.isNoneEmpty(userRole.getOrganId(), userRole.getProjectId())
                    && userRole.getOrganId().equals(organId) && userRole.getProjectId().equals(projectId))
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(userRoleList)) {
                return true;
            }
            roleId = String.valueOf(userRoleList.get(0).getRoleId());
        }
        QueryWrapper<BeanRoleAuthority> wrapper = new QueryWrapper<BeanRoleAuthority>().eq("role_id", roleId);
        if (StringUtils.isNotEmpty(type)) {
            wrapper.eq("type", type);
        }

        // 判断是否拥有运维权限
        List<BeanRoleAuthority> beanRoleAuthorityList = beanRoleAuthorityMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanRoleAuthorityList)) {
            if (beanRoleAuthorityList.stream()
                .anyMatch(roleAuthority -> Integer.parseInt(roleAuthority.getPower().split("")[1]) == 1)) {
                flag = true;
            }
        }
        return flag;
    }

    @Override
    public Set<String> listOpsMiddleware(Integer roleId) {
        List<BeanRoleAuthority> authorities = list(roleId);
        return authorities.stream().map(BeanRoleAuthority::getType).
                filter(type -> checkOps(String.valueOf(roleId), type)).
                collect(Collectors.toSet());
    }
}
