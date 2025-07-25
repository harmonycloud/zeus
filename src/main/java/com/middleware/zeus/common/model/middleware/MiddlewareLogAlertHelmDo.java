package com.middleware.zeus.common.model.middleware;

import com.alibaba.fastjson.annotation.JSONField;
import com.middleware.zeus.util.date.DateUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2025/6/19 下午4:20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("中间件日志告警")
@Accessors(chain = true)
public class MiddlewareLogAlertHelmDo {

    @ApiModelProperty("告警类型")
    private String type;

    @ApiModelProperty("匹配索引")
    private String index;

    @ApiModelProperty("匹配索引")
    private String alertLevel;

    @ApiModelProperty("静默时间")
    private String silence;

    @ApiModelProperty("周期，映射timeframe")
    private Integer interval;

    @ApiModelProperty("阈值，映射numEvents")
    private Integer threshold;

    @ApiModelProperty("黑名单匹配时的关键词")
    @JSONField(name = "compare_key")
    private String compareKey;

    @ApiModelProperty("黑名单列表")
    private List<String> blacklist;

    @ApiModelProperty("过滤器")
    private List<MiddlewareLogAlertDo.Filter> filter;

    @ApiModelProperty("告警内容")
    private String alertText;

    @ApiModelProperty("告警内容中所需参数")
    private List<String> alertTextArgs;

    @ApiModelProperty("更新时间")
    private String updateTime;

    public MiddlewareLogAlertHelmDo(MiddlewareLogAlertDto alertDto) {
        this.type = alertDto.getAlertMode();
        this.index = alertDto.getIndex();
        this.alertLevel = alertDto.getLevel();
        this.compareKey = alertDto.getCompareKey();
        this.blacklist = alertDto.getBlacklist();
        this.silence = alertDto.getSilence();
        this.interval = alertDto.getTimeframe();
        this.threshold = alertDto.getNumEvents();
        this.updateTime = alertDto.getUpdateTime() == null ? null : DateUtils.DateToString(alertDto.getUpdateTime(), DateUtils.YYYY_MM_DD_HH_MM_SS) ;

        List<MiddlewareLogAlertDo.Filter> filterList = new ArrayList<>();
        // 对用户设置的命中规则进行处理
        // 设置模糊匹配规则
        alertDto.getMatchRuleList().forEach(rule -> {
            // 若fuzzy为true，则为模糊匹配
            if (rule.getFuzzy() != null && rule.getFuzzy()) {
//                filterList.add(new MiddlewareLogAlertDo.Filter()
//                    .setBool(List.of(new MiddlewareLogAlertDo.Bool().setMinimumShouldMatch(1).setShould(
//                        List.of(new MiddlewareLogAlertDo.Should().setTerm(Map.of(rule.getKey(), rule.getValue())))))));
            } else {
                filterList.add(new MiddlewareLogAlertDo.Filter().setTerm(Map.of(rule.getKey(), rule.getValue())));
            }
        });
        this.filter = filterList;

        String content = alertDto.getContent();
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

    }

}
