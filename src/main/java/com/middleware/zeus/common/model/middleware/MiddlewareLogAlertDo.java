package com.middleware.zeus.common.model.middleware;

import java.util.ArrayList;
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

    @ApiModelProperty("告警名称")
    private String name;

    @ApiModelProperty("告警类型")
    private String type;

    @ApiModelProperty("匹配索引")
    private String index;

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

    public MiddlewareLogAlertDo(MiddlewareLogAlertDto alertDTto) {
        this.name = alertDTto.getAlert();
        this.type = alertDTto.getAlertMode();
        this.index = alertDTto.getIndex();
        this.alert = List.of(alertDTto.getAlert());

        if (alertDTto.getAlertMode().equals("frequency")) {
            TimeFrame timeframe = new TimeFrame();
            timeframe.setMinutes(alertDTto.getTimeframe());
            this.timeframe = timeframe;
            this.numEvents = alertDTto.getNumEvents();
        }

        List<Filter> filterList = new ArrayList<>();
        // 设置中间件通用的命中规则
        filterList.add(new Filter().setTerm(Map.of("middleware_name", alertDTto.getMiddlewareName())));
        filterList.add(new Filter().setTerm(Map.of("k8s_pod_namespace", alertDTto.getNamespace())));
        // 对用户设置的命中规则进行处理
        // 设置模糊匹配规则
        alertDTto.getMatchRuleList().forEach(rule -> {
            // 若fuzzy为true，则为模糊匹配
            if (rule.getFuzzy() != null && rule.getFuzzy()) {
                filterList.add(new Filter().setQueryString(Map.of(rule.getKey(), rule.getValue())));
            }
            if (rule.getFuzzy() != null && !rule.getFuzzy()) {
                filterList.add(new Filter().setTerm(Map.of(rule.getKey(), rule.getValue())));
            }
        });
        this.filter = filterList;

        String content = alertDTto.getContent();
        if (content != null) {
            StringBuilder updatedContent = new StringBuilder(content);
            int index = 0;
            List<String> alertTextArgs = new ArrayList<>();
            while (true) {
                int start = updatedContent.indexOf("${");
                int end = updatedContent.indexOf("}", start);
                if (start == -1 || end == -1) {
                    break;
                }
                String placeholder = updatedContent.substring(start + 2, end);
                alertTextArgs.add(placeholder);
                updatedContent.replace(start, end + 1, "{" + index + "}");
                index++;
            }
            this.alertTextArgs = alertTextArgs;
            this.alertText = updatedContent.toString();
        }
        
        // 设置alertmanager信息
        this.alertTextType = "alert_text_only";
        this.alertmanagerHosts = List.of("http://alertmanager-alertmanager.monitoring:9093");
        this.alertmanagerAlertname = alertDTto.getAlert();

        // 设置默认alertmanager resolve time
        ResolveTime resolveTime = new ResolveTime();
        resolveTime.setMinutes(10);
        this.alertmanagerResolveTime = resolveTime;
        
        // 设置默认alertmanager labels和annotations
        this.alertmanagerLabels = Map.of("source", "elastalert");
        this.alertmanagerAnnotations = Map.of("severity", alertDTto.getLevel());
        // 当存在更新时间时，设置更新时间
        if (alertDTto.getUpdateTime() != null) {
            this.alertmanagerAnnotations.put("update_time",
                DateUtils.DateToString(alertDTto.getUpdateTime(), DateUtils.YYYY_MM_DD_HH_MM_SS));
        }
    }

}
