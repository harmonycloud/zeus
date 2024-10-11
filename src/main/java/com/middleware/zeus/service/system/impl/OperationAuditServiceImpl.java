package com.middleware.zeus.service.system.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.middleware.zeus.common.model.OperationAuditConditionDto;
import com.skyview.language.annotations.TranslateAfterResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.constants.CommonConstant;
import com.middleware.zeus.common.constants.OperationAuditConstant;
import com.middleware.zeus.bean.BeanOperationAudit;
import com.middleware.zeus.bean.OperationAuditQueryDto;
import com.middleware.zeus.dao.BeanOperationAuditMapper;
import com.middleware.zeus.service.system.OperationAuditService;
import com.middleware.zeus.service.user.RoleService;

/**
 * 操作审计服务
 *
 * @author liyinlongØØ
 * @date 2021/7/27 10:04 上午
 */
@Service
public class OperationAuditServiceImpl implements OperationAuditService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BeanOperationAuditMapper operationAuditMapper;

    /**
     * ip地址正则表达式，仅包含数字或小数点即为ip
     */
    private final static Pattern ipPattern = Pattern.compile("[0-9\\.]*");
    /**
     * url路径正则表达式，包含下划线即为url路径
     */
    private final static Pattern urlPattern = Pattern.compile(".*/.*");

    @Override
    public void insert(BeanOperationAudit beanOperationAudit) {
        operationAuditMapper.insert(beanOperationAudit);
    }

    @Override
    @TranslateAfterResult
    public Page<BeanOperationAudit> list(OperationAuditQueryDto operationAuditQueryDto) {

        convertOperationAudit(operationAuditQueryDto);

        QueryWrapper<BeanOperationAudit> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(operationAuditQueryDto.getSearchKeyWord())) {
            switch (operationAuditQueryDto.getSearchType()) {
                case OperationAuditConstant.SEARCH_TYPE_IP:
                    queryWrapper.like("remote_ip", operationAuditQueryDto.getSearchKeyWord());
                    break;
                case OperationAuditConstant.SEARCH_TYPE_URL:
                    queryWrapper.like("url", operationAuditQueryDto.getSearchKeyWord());
                    break;
                case OperationAuditConstant.SEARCH_TYPE_OTHER:
                    queryWrapper.like("user_name", operationAuditQueryDto.getSearchKeyWord());
                    queryWrapper.or().like("account", operationAuditQueryDto.getSearchKeyWord());
                default:
                    break;
            }
        }

        if (CollectionUtils.isNotEmpty(operationAuditQueryDto.getRequestMethods())) {
            queryWrapper.in("request_method", operationAuditQueryDto.getRequestMethods());
        }

        if (CollectionUtils.isNotEmpty(operationAuditQueryDto.getModules())) {
            queryWrapper.in("module_ch_desc", operationAuditQueryDto.getModules());
        }

        if (CollectionUtils.isNotEmpty(operationAuditQueryDto.getChildModules())) {
            queryWrapper.in("child_module_ch_desc", operationAuditQueryDto.getChildModules());
        }

        if (CollectionUtils.isNotEmpty(operationAuditQueryDto.getRoles())) {
            queryWrapper.in("role_name", operationAuditQueryDto.getRoles());
        }

        if (operationAuditQueryDto.getBeginTimeNormalOrder() != null) {
            if (operationAuditQueryDto.getBeginTimeNormalOrder()) {
                queryWrapper.orderByAsc("begin_time");
            } else {
                queryWrapper.orderByDesc("begin_time");
            }
        }

        if (operationAuditQueryDto.getExecuteTimeNormalOrder() != null) {
            if (operationAuditQueryDto.getExecuteTimeNormalOrder()) {
                queryWrapper.orderByAsc("execute_time");
            } else {
                queryWrapper.orderByDesc("execute_time");
            }
        }

        if (operationAuditQueryDto.getStatusOrder() != null) {
            if (operationAuditQueryDto.getStatusOrder()) {
                queryWrapper.orderByAsc("status");
            } else {
                queryWrapper.orderByDesc("status");
            }
        }
        queryWrapper.select(BeanOperationAudit.class, tableFieldInfo
                -> !tableFieldInfo.getColumn().equals("request_params") && !tableFieldInfo.getColumn().equals("response"));
        Page<BeanOperationAudit> page = new Page<>(operationAuditQueryDto.getCurrent(), operationAuditQueryDto.getSize());
        Page<BeanOperationAudit> beanOperationAuditPage = operationAuditMapper.selectPage(page, queryWrapper);
        
        return beanOperationAuditPage;
    }

    @Override
    @TranslateAfterResult
    public OperationAuditConditionDto listAllCondition() {
        OperationAuditConditionDto operationAuditConditionDto = new OperationAuditConditionDto();

        QueryWrapper<BeanOperationAudit> moduleWrapper = new QueryWrapper<>();
        moduleWrapper.select("DISTINCT module_ch_desc").isNotNull("module_ch_desc");
        List<String> moduleList = operationAuditMapper.selectList(moduleWrapper).stream().
                map(operationAudit -> operationAudit.getModuleChDesc()).collect(Collectors.toList());
        //Map<String, List<String>> moduleMenu = new HashMap<>();

        List<OperationAuditConditionDto.Modules> modulesList = new ArrayList<>();
        moduleList.forEach(item -> {
            OperationAuditConditionDto.Modules modules = operationAuditConditionDto.new Modules();

            QueryWrapper<BeanOperationAudit> childModuleWrapper = new QueryWrapper<>();
            childModuleWrapper.eq("module_ch_desc", item).select("DISTINCT child_module_ch_desc").isNotNull("child_module_ch_desc");
            List<OperationAuditConditionDto.Modules.ChildModules> childModules = operationAuditMapper.selectList(childModuleWrapper).stream().
                    map(beanOperationAudit -> modules.new ChildModules().setName(beanOperationAudit.getChildModuleChDesc())).collect(Collectors.toList());

            modules.setName(item);
            modules.setChildModules(childModules);
            modulesList.add(modules);
            //moduleMenu.put(item, childModule);
        });
        operationAuditConditionDto.setModulesList(modulesList);
        //res.put("modules", moduleMenu);

        QueryWrapper<BeanOperationAudit> roleWrapper = new QueryWrapper<>();
        roleWrapper.select("DISTINCT role_name").isNotNull("role_name");
        List<OperationAuditConditionDto.Role> roleList = operationAuditMapper.selectList(roleWrapper).stream().
                map(operationAudit -> operationAuditConditionDto.new Role().setName(operationAudit.getRoleName())).collect(Collectors.toList());
        operationAuditConditionDto.setRoles(roleList);
        //res.put("roles", roleList);

        QueryWrapper<BeanOperationAudit> requestMethodWrapper = new QueryWrapper<>();
        requestMethodWrapper.select("DISTINCT request_method").isNotNull("request_method");
        List<String> methodList = operationAuditMapper.selectList(requestMethodWrapper).stream().
                map(operationAudit -> operationAudit.getRequestMethod()).collect(Collectors.toList());
        operationAuditConditionDto.setMethods(methodList);
        //res.put("methods", methodList);
        return operationAuditConditionDto;
    }

    /**
     * 处理查询条件
     * @param operationAuditQueryDto
     * @return
     * @author liyinlong
     * @date 2021/7/28 3:45 下午
     */
    private OperationAuditQueryDto convertOperationAudit(OperationAuditQueryDto operationAuditQueryDto) {

        if (operationAuditQueryDto.getCurrent() == 0) {
            operationAuditQueryDto.setCurrent(CommonConstant.NUM_ONE);
        }

        if (operationAuditQueryDto.getSize() == 0) {
            operationAuditQueryDto.setSize(CommonConstant.DEFAULT_PAGE_SIZE_10);
        }

        if (StringUtils.isBlank(operationAuditQueryDto.getSearchKeyWord())) {
            return operationAuditQueryDto;
        }

        if (ipPattern.matcher(operationAuditQueryDto.getSearchKeyWord()).matches()) {
            //关键词为ip
            operationAuditQueryDto.setSearchType(OperationAuditConstant.SEARCH_TYPE_IP);
        } else if (urlPattern.matcher(operationAuditQueryDto.getSearchKeyWord()).matches()) {
            //关键词为路径
            operationAuditQueryDto.setSearchType(OperationAuditConstant.SEARCH_TYPE_URL);
        } else {
            //关键词类型为账户或用户名
            operationAuditQueryDto.setSearchType(OperationAuditConstant.SEARCH_TYPE_OTHER);
        }
        return operationAuditQueryDto;
    }

    @Override
    @TranslateAfterResult
    public List<BeanOperationAudit> listRecent(Integer num) {
        if (num == null) {
            num = 20;
        }
        QueryWrapper<BeanOperationAudit> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("begin_time");
        Page<BeanOperationAudit> page = new Page<>(1, num);
        Page<BeanOperationAudit> beanOperationAuditPage = operationAuditMapper.selectPage(page, queryWrapper);
        return beanOperationAuditPage.getRecords();
    }

    @Override
    @TranslateAfterResult
    public BeanOperationAudit get(Integer id) {
        QueryWrapper<BeanOperationAudit> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        List<BeanOperationAudit> beanOperationAudits = operationAuditMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(beanOperationAudits)){
            return null;
        }
        return beanOperationAudits.get(0);
    }

}
