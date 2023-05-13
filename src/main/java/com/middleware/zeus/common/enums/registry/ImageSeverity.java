package com.middleware.zeus.common.enums.registry;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author dengyulong
 * @date 2021/01/21
 * 镜像漏洞级别枚举类
 */
@Getter
public enum ImageSeverity {

    NONE("None"),
    NEGLIGIBLE("Negligible"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical"),
    ;

    private String severity;

    /**
     * 根据严重级别返回枚举类
     *
     * @param severity
     * @return
     */
    public static ImageSeverity findBySeverity(String severity) {
        if (StringUtils.isNotEmpty(severity)) {
            return null;
        }
        for (ImageSeverity imageSeverity : ImageSeverity.values()) {
            if (severity.equalsIgnoreCase(imageSeverity.getSeverity())) {
                return imageSeverity;
            }
        }
        return null;
    }

    ImageSeverity(String severity) {
        this.severity = severity;
    }

}
