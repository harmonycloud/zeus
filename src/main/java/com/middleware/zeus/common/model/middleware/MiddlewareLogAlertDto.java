package com.middleware.zeus.common.model.middleware;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.middleware.zeus.util.DateUtil;
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

    @ApiModelProperty("完全匹配")
    private Map<String, String> exactMatch;

    @ApiModelProperty("模糊匹配")
    private Map<String, String> fuzzyMatch;

    @ApiModelProperty("周期")
    private Integer timeframe;

    @ApiModelProperty("次数")
    private Integer numEvents;

    @ApiModelProperty("沉默时间")
    private Integer silence;

    @ApiModelProperty("备注")
    private Map<String, String> annotations;

    @ApiModelProperty("标签")
    private Map<String, String> labels;

    @ApiModelProperty("更新时间")
    private Date updateTime;

    public MiddlewareLogAlertDto(MiddlewareLogAlertDo alertDo) {
        this.alert = alertDo.getName();
        this.alertMode = alertDo.getType();
        this.index = alertDo.getIndex();
        this.compareKey = alertDo.getCompareKey();
        this.blacklist = alertDo.getBlacklist();
        this.timeframe = alertDo.getTimeframe().getMinutes();
        this.numEvents = alertDo.getNumEvents();
        this.annotations = alertDo.getAlertmanagerAnnotations();
        this.labels = alertDo.getAlertmanagerLabels();
        // 从annotations中获取告警等级
        if (alertDo.getAlertmanagerAnnotations().containsKey("severity")){
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

        // 通过解析filter中的内容，分别设置完全匹配和模糊匹配的规则
        if (alertDo.getFilter() != null) {
            Map<String, String> exactMatch = new HashMap<>();
            Map<String, String> fuzzyMatch = new HashMap<>();
            for (MiddlewareLogAlertDo.Filter filter : alertDo.getFilter()) {
                if (filter.getTerm() != null) {
                    exactMatch.putAll(filter.getTerm());
                }
                if (filter.getQueryString() != null) {
                    fuzzyMatch.putAll(filter.getQueryString());
                }
            }
            this.exactMatch = exactMatch;
            this.fuzzyMatch = fuzzyMatch;
        }
        
        // 设置更新时间
        if (alertDo.getAlertmanagerAnnotations().containsKey("update_time")) {
            this.updateTime = DateUtils.parseDate(alertDo.getAlertmanagerAnnotations().get("update_time"),
                DateUtils.YYYY_MM_DD_HH_MM_SS);
        }

        // todo 设置告警沉默时间

    }

}
