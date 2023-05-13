package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 双活可用区注解
 * @author liyinlong
 * @since 2023/1/14 10:53 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "可用区注解")
public class ActiveAreaAnnotationDto {

    /**
     * 可用区A注解
     */
    private Map<String, String> zoneAAnnotation;

    /**
     * 可用区B注解
     */
    private Map<String, String> zoneBAnnotation;

    public ActiveAreaAnnotationDto(Map<String, String> zoneAAnnotation, Map<String, String> zoneBAnnotation) {
        this.zoneAAnnotation = zoneAAnnotation;
        this.zoneBAnnotation = zoneBAnnotation;
    }

}
