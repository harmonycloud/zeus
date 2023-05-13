package com.middleware.zeus.common.model.middleware;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @date 2021/06/22
 */
@Accessors(chain = true)
@Data
@JsonInclude(value=JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Question {

    private String variable;

    @JSONField(name = "default")
    private String defaultValue;

    private String description;

    private String type;

    private String label;

    @JSONField(name = "show_subquestion_if")
    private String showSubQuestionIf;

    private String group;

    @JSONField(name = "subquestions")
    private List<Question> subQuestions;

    @JSONField(name = "show_if")
    private String showIf;

    private List<String> options;

    private String pattern;

    private String message;

    private Boolean detail;

    private Boolean required;
}
