package com.harmonycloud.zeus.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.constants.OperationAuditConstant;
import com.harmonycloud.zeus.bean.BeanOperationAudit;
import com.harmonycloud.zeus.bean.OperationAuditQueryDto;
import com.harmonycloud.zeus.dao.BeanOperationAuditMapper;
import com.harmonycloud.zeus.service.system.OperationAuditService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    public BaseResult list(OperationAuditQueryDto operationAuditQueryDto) {

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

        Page<BeanOperationAudit> page = new Page<>(operationAuditQueryDto.getCurrent(), operationAuditQueryDto.getSize());
        Page<BeanOperationAudit> beanOperationAuditPage = operationAuditMapper.selectPage(page, queryWrapper);
        return BaseResult.ok(beanOperationAuditPage);
    }

    @Override
    public BaseResult listAllCondition() {
        Map<String, Object> res = new HashMap<>();

        QueryWrapper<BeanOperationAudit> moduleWrapper = new QueryWrapper<>();
        moduleWrapper.select("DISTINCT module_ch_desc").isNotNull("module_ch_desc");
        List<String> moduleList = operationAuditMapper.selectList(moduleWrapper).stream().
                map(operationAudit -> operationAudit.getModuleChDesc()).collect(Collectors.toList());
        Map<String, List<String>> moduleMenu = new HashMap<>();

        moduleList.forEach(item -> {
            QueryWrapper<BeanOperationAudit> childModuleWrapper = new QueryWrapper<>();
            childModuleWrapper.eq("module_ch_desc", item).select("DISTINCT child_module_ch_desc").isNotNull("child_module_ch_desc");
            List<String> childModule = operationAuditMapper.selectList(childModuleWrapper).stream().
                    map(operationAudit -> operationAudit.getChildModuleChDesc()).collect(Collectors.toList());
            moduleMenu.put(item, childModule);
        });
        res.put("modules", moduleMenu);

        QueryWrapper<BeanOperationAudit> roleWrapper = new QueryWrapper<>();
        roleWrapper.select("DISTINCT role_name").isNotNull("role_name");
        List<String> roleList = operationAuditMapper.selectList(roleWrapper).stream().
                map(operationAudit -> operationAudit.getRoleName()).collect(Collectors.toList());
        res.put("roles", roleList);

        QueryWrapper<BeanOperationAudit> requestMethodWrapper = new QueryWrapper<>();
        requestMethodWrapper.select("DISTINCT request_method").isNotNull("request_method");
        List<String> methodList = operationAuditMapper.selectList(requestMethodWrapper).stream().
                map(operationAudit -> operationAudit.getRequestMethod()).collect(Collectors.toList());
        res.put("methods", methodList);
        return BaseResult.ok(res);
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

}
