package com.middleware.zeus.common.enums.registry;

/**
 * @author chwetion
 * @since 2020/12/14 3:44 下午
 */
public enum RetentionRulePolicy {
    ALL("always"),
    LAST_PUSH_N_IMAGE("latestPushedK"),
    LAST_PULL_N_IMAGE("latestPulledN"),
    LAST_N_DAY_PUSH_IMAGE("nDaysSinceLastPush"),
    LAST_N_DAY_PULL_IMAGE("nDaysSinceLastPull"),
    ;

    private String v1HarborPolicyName;

    RetentionRulePolicy(String v1HarborPolicyName) {
        this.v1HarborPolicyName = v1HarborPolicyName;
    }

    public String getHarborPolicyName() {
        return v1HarborPolicyName;
    }
}
