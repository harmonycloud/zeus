package com.middleware.zeus.common.constants.registry;

/**
 * @author dengyulong
 * @date 2020/12/03
 * 制品服务常量
 */
public class RegistryConstant {

    /**
     * 子系统单个项目的默认镜像数量配额，-1表示无限制
     */
    public static final byte DEFAULT_COUNT_PER_PROJECT = -1;
    /**
     * 子系统单个项目的默认磁盘存储配额，-1表示无限制
     */
    public static final byte DEFAULT_STORAGE_PER_PROJECT = -1;
    /**
     * 观云台的单个项目的默认镜像数量配额，1万个镜像
     */
    public static final int DEFAULT_QUOTA_NUM = 10000;
    /**
     * 制品服务单个项目的默认磁盘配额，500GB
     */
    public static final long DEFAULT_QUOTA_SIZE = 536870912000L;
    /**
     * 制品服务的默认镜像数量，100000000
     */
    public static final long LIMIT_QUOTA_NUM = 100000000L;
    /**
     * 制品服务的默认磁盘配额，1024TB
     */
    public static final long LIMIT_QUOTA_SIZE = 1125899906842624L;

    /**
     * imagepullsecret data key
     */
    public static final String KEY_IMAGE_PULL_SECRET =  ".dockerconfigjson";


    public static final String IMAGE_PULL_SECRET_TYPE = "kubernetes.io/dockerconfigjson";

}
