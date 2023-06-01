package com.middleware.zeus.common.model.middleware;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2021/11/4 9:45 上午
 */
@Data
@Accessors(chain = true)
public class PodInfoGroup {

    /**
     * pod分组角色
     */
    private String role;

    /**
     * 状态
     */
    private String status;

    /**
     * 是否拥有子分组
     */
    private Boolean hasChildGroup;

    /**
     * 子分组（如果有）
     */
    private List<PodInfoGroup> listChildGroup;

    /**
     * 该分组pod
     */
    private List<PodInfo> pods;

    public PodInfoGroup() {
    }

    public PodInfoGroup(String role, Boolean hasChildGroup, List<PodInfoGroup> listChildGroup, List<PodInfo> pods) {
        this.role = role;
        this.hasChildGroup = hasChildGroup;
        this.listChildGroup = listChildGroup;
        this.pods = pods;
    }
}
