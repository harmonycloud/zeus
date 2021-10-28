package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 中间件备份记录spec
 * @author  liyinlong
 * @since 2021/9/15 10:06 上午
 */
@Data
@Accessors(chain = true)
public class MiddlewareBackupSpec {

    /**
     * 备份记录名称
     */
    private String name;

    /**
     * pod名称
     */
    private String pod;

    /**
     * 中间件类型
     */
    private String type;

    public MiddlewareBackupSpec() {
    }

}

