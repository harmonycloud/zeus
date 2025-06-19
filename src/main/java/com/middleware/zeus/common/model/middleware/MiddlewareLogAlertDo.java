package com.middleware.zeus.common.model.middleware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;
import com.middleware.zeus.util.date.DateUtils;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/5/18 下午3:37
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("中间件日志告警")
@Accessors(chain = true)
public class MiddlewareLogAlertDo {

    @ApiModelProperty("告警类型")
    private String type;

    @ApiModelProperty("匹配索引")
    private String index;

    @ApiModelProperty("静默时间")
    private String silence;

    @ApiModelProperty("周期")
    private TimeFrame timeframe;

    @ApiModelProperty("次数")
    @JSONField(name = "num_events")
    private int numEvents;

    @ApiModelProperty("黑名单匹配时的关键词")
    @JSONField(name = "compare_key")
    private String compareKey;

    @ApiModelProperty("黑名单列表")
    private List<String> blacklist;

    @ApiModelProperty("过滤器")
    private List<Filter> filter;

    @ApiModelProperty("告警目标")
    private List<String> alert;

    @ApiModelProperty("告警内容")
    @JSONField(name = "alert_text")
    private String alertText;

    @ApiModelProperty("告警内容中所需参数")
    @JSONField(name = "alert_text_args")
    private List<String> alertTextArgs;

    @ApiModelProperty("告警内容类型")
    @JSONField(name = "alert_text_type")
    private String alertTextType;

    @ApiModelProperty("alertmanager地址")
    @JSONField(name = "alertmanager_hosts")
    private List<String> alertmanagerHosts;

    @ApiModelProperty("alertmanager: 告警名称")
    @JSONField(name = "alertmanager_alertname")
    private String alertmanagerAlertname;

    @ApiModelProperty("alertmanager: 注解")
    @JSONField(name = "alertmanager_annotations")
    private Map<String, String> alertmanagerAnnotations;

    @ApiModelProperty("alertmanager: 告警处理时间")
    @JSONField(name = "alertmanager_resolve_time")
    private ResolveTime alertmanagerResolveTime;

    @ApiModelProperty("alertmanager: 标签")
    @JSONField(name = "alertmanager_labels")
    private Map<String, String> alertmanagerLabels;

    @Data
    public static class TimeFrame {
        private Integer minutes;
    }

    @Data
    @Accessors(chain = true)
    public static class Filter {
        private Map<String, String> term;
        @JSONField(name = "query_string")
        private Map<String, String> queryString;
    }

    @Data
    public static class ResolveTime {
        private Integer minutes;
    }
}
