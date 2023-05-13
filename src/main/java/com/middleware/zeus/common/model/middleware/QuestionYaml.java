package com.middleware.zeus.common.model.middleware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @date 2021/06/22
 */
@Accessors(chain = true)
@Data
@JsonInclude(value=JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionYaml {

    private Map<String, String> labels;

    private List<String> categories;

    private List<String> capabilities;

    private List<Question> questions;
}
