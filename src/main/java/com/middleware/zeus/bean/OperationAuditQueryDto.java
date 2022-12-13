package com.middleware.zeus.bean;

import lombok.Data;

import java.util.List;

/**
 * 操作审计查询条件dto
 * @author liyinlong
 * @date 2021/7/27 4:07 下午
 */
@Data
public class OperationAuditQueryDto {

    /**
     * 搜索关键词
     */
    private String searchKeyWord;

    /**
     * 父模块
     */
    private List<String> modules;

    /**
     * 子模块
     */
    private List<String> childModules;

    /**
     * 请求方法
     */
    private List<String> requestMethods;

    /**
     * 角色
     */
    private List<String> roles;

    /**
     * 当前页
     */
    private int current;

    /**
     * 每页记录数量
     */
    private int size;

    /**
     * ip
     */
    private String ip;

    /**
     * 搜索类型：IP,URL,ALL
     */
    private String searchType;

    /**
     * 耗时排序 true：正序 false：倒序
     */
    private Boolean beginTimeNormalOrder;

    /**
     * 请求时间排序 true：正序   false：倒序
     */
    private Boolean executeTimeNormalOrder;

    /**
     * 状态码排序 true：正序 false：倒序
     */
    private Boolean statusOrder;
}
