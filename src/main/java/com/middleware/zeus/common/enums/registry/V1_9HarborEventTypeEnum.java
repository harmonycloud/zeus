package com.middleware.zeus.common.enums.registry;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author 邓玉龙
 * @since 2021/01/28
 */
public enum V1_9HarborEventTypeEnum {

    DOWNLOAD_CHART(0,"downloadChart"),
    DELETE_CHART(1,"deleteChart"),
    UPLOAD_CHART(2,"uploadChart"),
    DELETE_IMAGE(3,"deleteImage"),
    PULL_IMAGE(4,"pullImage"),
    PUSH_IMAGE(5,"pushImage"),
    SCANNING_FAILED(6,"scanningFailed"),
    SCANNING_COMPLETED(7,"scanningCompleted"),
    ;

    private int code;
    private String event;

    private static List<String> harborEventList = new ArrayList<>();

    static {
        for (V1_9HarborEventTypeEnum type : EnumSet.allOf(V1_9HarborEventTypeEnum.class)) {
            harborEventList.add(type.event);
        }
    }

    public static List<String> getHarborEventList() {
        return harborEventList;
    }

    V1_9HarborEventTypeEnum(int code, String event) {
        this.code = code;
        this.event = event;
    }

    public int getCode() {
        return code;
    }

    public String getEvent() {
        return event;
    }

}
