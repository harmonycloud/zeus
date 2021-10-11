package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2021/9/24 4:30 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("kube_config")
public class BeanKubeConfig implements Serializable {

    private static final long serialVersionUID = -8967832121462L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 集群id
     */
    @TableField("cluster_id")
    private String clusterId;

    /**
     * 集群id
     */
    @TableField("conf")
    private String conf;

}
