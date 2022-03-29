package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCluster;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yushuaikang
 * @date 2022/3/25 上午11:00
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("middleware_cluster")
public class BeanMiddlewareCluster {

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 集群ID
     */
    @TableField(value = "clusterId")
    private String clusterId;

    /**
     * 集群对象
     */
    @TableField(value = "middleware_cluster")
    private String middlewareCluster;
}
