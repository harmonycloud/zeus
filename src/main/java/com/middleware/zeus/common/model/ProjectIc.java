package com.middleware.zeus.common.model;

import lombok.Data;

import java.util.Date;

/**
 * @author chenbilong
 */
@Data
public class ProjectIc {
    int id;
    String projectId;
    String clusterId;
    String icNames;
    Date createTime;
    Date updateTime;
}
