package com.middleware.zeus.common.enums;

/**
 * @author chwetion
 * @since 2020/12/9 6:00 下午
 */
public enum ClusterAuthType {
    TOKEN("token"),
    KUBECONFIG("kubeconfig"),
    ;

    private String type;

    ClusterAuthType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static boolean contains(String type) {
        for (ClusterAuthType enums : ClusterAuthType.values()) {
            if (enums.getType().equals(type.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
