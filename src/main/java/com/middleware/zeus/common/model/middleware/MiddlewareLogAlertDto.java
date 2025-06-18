package com.middleware.zeus.common.model.middleware;

import java.util.*;

import com.middleware.zeus.util.date.DateUtils;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/5/19 下午5:31
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("中间件日志告警规则")
public class MiddlewareLogAlertDto {

    @ApiModelProperty("集群ID")
    private String clusterId;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("规则名称")
    private String alert;

    @ApiModelProperty("告警内容")
    private String content;

    @ApiModelProperty("告警等级")
    private String level;

    @ApiModelProperty("告警模式")
    private String alertMode;

    @ApiModelProperty("匹配索引")
    private String index;

    @ApiModelProperty("匹配索引")
    private String compareKey;

    @ApiModelProperty("黑名单")
    private List<String> blacklist;

    @ApiModelProperty("匹配规则")
    private List<MatchRule> matchRuleList;

    @ApiModelProperty("周期")
    private Integer timeframe;

    @ApiModelProperty("次数")
    private Integer numEvents;

    @ApiModelProperty("沉默时间")
    private String silence;

    @ApiModelProperty("沉没事件单位")
    private String unit;

    @ApiModelProperty("备注")
    private Map<String, String> annotations;

    @ApiModelProperty("标签")
    private Map<String, String> labels;

    @ApiModelProperty("更新时间")
    private Date updateTime;

    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MatchRule {

        @ApiModelProperty("键值对key")
        private String key;

        @ApiModelProperty("键值对value")
        private String value;

        @ApiModelProperty("模糊匹配")
        private Boolean fuzzy;
    }

    public MiddlewareLogAlertDto(MiddlewareLogAlertDo alertDo) {
        this.alert = alertDo.getAlertmanagerAlertname();
        this.alertMode = alertDo.getType();
        this.index = alertDo.getIndex();
        this.compareKey = alertDo.getCompareKey();
        this.blacklist = alertDo.getBlacklist();
        if (alertDo.getTimeframe() != null) {
            this.timeframe = alertDo.getTimeframe().getMinutes();
        }
        this.numEvents = alertDo.getNumEvents();
        this.annotations = alertDo.getAlertmanagerAnnotations();
        this.labels = alertDo.getAlertmanagerLabels();
        // 从annotations中获取告警等级
        if (alertDo.getAlertmanagerAnnotations().containsKey("severity")) {
            this.level = alertDo.getAlertmanagerAnnotations().get("severity");
        }

        // 处理alertText，将其中的{0} {1} {2}替换成alertTextArgs中的字段
        if (alertDo.getAlertText() != null && alertDo.getAlertTextArgs() != null) {
            String alertText = alertDo.getAlertText();
            for (int i = 0; i < alertDo.getAlertTextArgs().size(); i++) {
                alertText = alertText.replace("{" + i + "}", "${" + alertDo.getAlertTextArgs().get(i) + "}");
            }
            this.content = alertText;
        }

        List<MatchRule> matchRuleList = new ArrayList<>();
        // 通过解析filter中的内容，设置完全匹配和模糊匹配的规则
        if (alertDo.getFilter() != null) {
            for (MiddlewareLogAlertDo.Filter filter : alertDo.getFilter()) {
                if (filter.getTerm() != null) {
                    for (String key : filter.getTerm().keySet()) {
                        matchRuleList
                            .add(new MatchRule().setKey(key).setValue(filter.getTerm().get(key)).setFuzzy(false));
                    }
                }
                if (filter.getQueryString() != null) {
                    for (String key : filter.getQueryString().keySet()) {
                        matchRuleList
                            .add(new MatchRule().setKey(key).setValue(filter.getQueryString().get(key)).setFuzzy(true));
                    }
                }
            }
        }
        this.matchRuleList = matchRuleList;

        // 设置更新时间
        if (alertDo.getAlertmanagerAnnotations().containsKey("update_time")) {
            this.updateTime = DateUtils.parseDate(alertDo.getAlertmanagerAnnotations().get("update_time"),
                DateUtils.YYYY_MM_DD_HH_MM_SS);
        }

        // 设置告警沉默时间 并添加默认单位m
        if (alertDo.getAlertmanagerAnnotations().containsKey("silence")) {
            String silence = alertDo.getAlertmanagerAnnotations().get("silence");
            if (!silence.endsWith("m") || !silence.endsWith("h")) {
                silence = silence + "m";
            }
            this.silence = silence;
        }
        // 设置告警时间单位
        if (alertDo.getAlertmanagerAnnotations().containsKey("unit")) {
            this.unit = alertDo.getAlertmanagerAnnotations().get("unit");
        }
    }

}
