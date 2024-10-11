package com.middleware.zeus.service.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.bean.BeanOperationAudit;
import com.middleware.zeus.bean.OperationAuditQueryDto;
import com.middleware.zeus.common.model.OperationAuditConditionDto;

import java.util.List;

public interface OperationAuditService {
    void insert(BeanOperationAudit beanOperationAudit);

    /**
     * 查询操作审计列表
     * @param operationAuditQueryDto 查询条件
     * @return
     */
    Page<BeanOperationAudit> list(OperationAuditQueryDto operationAuditQueryDto);

    /**
     * 查询操作审计菜单信息
     * @return
     */
    OperationAuditConditionDto listAllCondition();

    /**
     * 查询最近num条审计信息
     *
     * @param num
     * @return
     */
    List<BeanOperationAudit> listRecent(Integer num);

    /**
     * 查询操作审计详情
     * @param id
     * @return
     */
    BeanOperationAudit get(Integer id);
}
