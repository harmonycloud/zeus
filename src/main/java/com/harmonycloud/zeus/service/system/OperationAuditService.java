package com.harmonycloud.zeus.service.system;

import com.middleware.caas.common.base.BaseResult;
import com.harmonycloud.zeus.bean.BeanOperationAudit;
import com.harmonycloud.zeus.bean.OperationAuditQueryDto;

import java.util.List;

public interface OperationAuditService {
    void insert(BeanOperationAudit beanOperationAudit);

    /**
     * 查询操作审计列表
     * @param operationAuditQueryDto 查询条件
     * @return
     */
    BaseResult list(OperationAuditQueryDto operationAuditQueryDto);

    /**
     * 查询操作审计菜单信息
     * @return
     */
    BaseResult listAllCondition();

    /**
     * 查询最近num条审计信息
     *
     * @param num
     * @return
     */
    List<BeanOperationAudit> listRecent(Integer num);
}
