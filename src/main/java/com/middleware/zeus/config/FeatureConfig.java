package com.middleware.zeus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/5/4 8:56 下午
 */
@Component
@ConfigurationProperties("system")
@Data
public class FeatureConfig {

    private Map<String, Boolean> feature;
}


