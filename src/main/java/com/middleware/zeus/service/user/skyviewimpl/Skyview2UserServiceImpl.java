package com.middleware.zeus.service.user.skyviewimpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.middleware.zeus.common.model.user.RoleDto;
import com.middleware.zeus.common.model.user.UserRole;
import com.middleware.zeus.service.user.RoleAuthorityService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.MailUserDTO;
import com.middleware.zeus.common.model.user.SystemConfigDto;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.bean.user.BeanUser;
import com.middleware.zeus.service.user.RoleService;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.service.user.abstractService.AbstractUserService;
import com.middleware.zeus.skyview.v2.service.V2UserService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author liyinlong
 * @since 2022/6/8 1:50 下午
 */
@Slf4j
@Service
@Skyview(target = "skyview2")
public class Skyview2UserServiceImpl extends AbstractUserService implements UserService {

    @Autowired
    private RoleService roleService;
    @Autowired
    private V2UserService v2UserService;

    @Override
    public UserDto getUserDto(String userName, String projectId, boolean roleDetail) {
        if (StringUtils.isEmpty(userName)) {
            userName = getUsername();
        }
        return getUserDto(userName, roleDetail);
    }

    @Override
    public BeanUser get(String userName) {
        return null;
    }

    @Override
    public UserDto getUserDto(String userName, boolean roleDetail) {
        UserDto userDto = v2UserService.get(userName);
        setPower(userDto);
        return userDto;
    }

    @Override
    public List<UserDto> list(String keyword) {
        List<UserDto> userDtoList = v2UserService.list();
        if (StringUtils.isNotEmpty(keyword)) {
            userDtoList = userDtoList.stream()
                .filter(userDto -> StringUtils.containsIgnoreCase(userDto.getUserName(), keyword)
                    || StringUtils.containsIgnoreCase(userDto.getAliasName(), keyword)
                    || StringUtils.containsIgnoreCase(userDto.getEmail(), keyword)
                    || StringUtils.containsIgnoreCase(userDto.getPhone(), keyword))
                .collect(Collectors.toList());
        }
        return userDtoList;
    }

    @Override
    public void create(UserDto userDto) throws Exception {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void create(BeanUser beanUser) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void update(UserDto userDto) throws Exception {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void update(BeanUser beanUser) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public Boolean delete(String userName) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public Boolean reset(String userName) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY);
    }

    @Override
    public void bind(String userName, String role) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void changePassword(String userName, String password, String newPassword, String reNewPassword) throws Exception {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public MailUserDTO getUserList(String alertruleId) {
        // todo 邮箱告警相关
        return null;
    }

    @Override
    public Map<String, String> getPower() {
        // todo
        return null;
    }

    @Override
    public void savePasswordExpiredDay(String days) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public SystemConfigDto getPasswordExpiredDay() {
        return null;
    }

    @Override
    public UserRole getUserRole(String username, String organId, String projectId) {
        UserDto userDto = this.getUserDto(username, true);
        if (!CollectionUtils.isEmpty(userDto.getUserRoleList())) {
            List<UserRole> userRoleList = userDto.getUserRoleList().stream()
                .filter(userRole -> StringUtils.isNoneEmpty(userRole.getOrganId(), userRole.getProjectId())
                    && userRole.getOrganId().equals(organId) && userRole.getProjectId().equals(projectId))
                .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(userRoleList)) {
                return userRoleList.get(0);
            }
        }
        return null;
    }

    @Override
    public List<UserDto> getUserRole(List<UserDto> userDtoList) {
        return userDtoList;
    }

    private void setPower(UserDto userDto) {
        if (!CollectionUtils.isEmpty(userDto.getUserRoleList())) {
            // 获取角色信息
            Map<Integer, RoleDto> roleDtoMap =
                roleService.list(null).stream().collect(Collectors.toMap(RoleDto::getId, r -> r));
            userDto.setUserRoleList(userDto.getUserRoleList().stream().peek(userRole -> {
                userRole.setRoleName(roleDtoMap.get(userRole.getRoleId()).getName());
                userRole.setWeight(roleDtoMap.get(userRole.getRoleId()).getWeight());
                userRole.setPower(roleDtoMap.get(userRole.getRoleId()).getPower());
            }).collect(Collectors.toList()));
        }
    }
}
