package com.middleware.zeus.config;

import com.middleware.zeus.annotation.Skyview;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/3/22 12:36 上午
 */
public class SkyviewCondition implements Condition {

    private static final String TARGET = "target";


    @Override
    public boolean matches(ConditionContext context, @NotNull AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String userCenter = env.getProperty("system.usercenter");
        if (StringUtils.isEmpty(userCenter)) {
            userCenter = "zeus";
        }
        Map<String, Object> attrs = metadata.getAnnotationAttributes(Skyview.class.getName());
        if (!CollectionUtils.isEmpty(attrs)){
            return attrs.containsKey(TARGET) && attrs.get(TARGET).equals(userCenter);
        }
        return false;
    }
}
