package com.middleware.zeus.common.constants;

/**
 * @author chwetion
 * @since 2021/1/6 5:54 下午
 */
public class LabelConstant {
    public static final String APP_LABEL_KEY = "app";
    public static final String NODE_POOL_LABEL_KEY = "node-pool";
    public static final String IGNORE_NODE_POOL_LABEL_VALUE = "ignore";

    public static final String INGRESS_CA_NAME_LABEL_KEY = "ca-name";

    public static final String WORK_NODE_LABEL_KEY = "node-role.kubernetes.io/work";
    public static final String NODE_UNSCHEDULEABLE_TAINT_LABEL_KEY = "node.kubernetes.io/unschedulable";
}
