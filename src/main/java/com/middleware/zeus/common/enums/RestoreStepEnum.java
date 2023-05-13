package com.middleware.zeus.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 中间件恢复进度枚举
 *
 * @author liyinlong
 * @since 2021/11/17 9:22 上午
 */
public enum RestoreStepEnum {

    WAIT_RESTORE("step_wait_restore", 1, "等待组件启动"),
    FILLUP("step_fillup", 2, "获取组件信息"),
    STOP_MIDDLEWARE("step_stop_middleware", 3, "开始停止组件"),
    WAIT_STOP_MIDDLEWARE("step_wait_stop_middleware", 4, "等待组件完全停止"),
    RUN_JOB("step_run_job", 5, "正在运行恢复任务"),
    STATR_MIDDLEWARE("step_statr_middleware", 6, "数据恢复完毕，正在启动组件"),
    FINSH("step_finish", 7, "恢复完成"),
    ;

    private String step;

    private Integer stepNum;

    private String stepDescription;

    private static Map<String, RestoreStepEnum> stepEnumMap = new HashMap<>();

    static {
        stepEnumMap.put(WAIT_RESTORE.step, WAIT_RESTORE);
        stepEnumMap.put(FILLUP.step, FILLUP);
        stepEnumMap.put(STOP_MIDDLEWARE.step, STOP_MIDDLEWARE);
        stepEnumMap.put(WAIT_STOP_MIDDLEWARE.step, WAIT_STOP_MIDDLEWARE);
        stepEnumMap.put(RUN_JOB.step, RUN_JOB);
        stepEnumMap.put(STATR_MIDDLEWARE.step, STATR_MIDDLEWARE);
    }

    RestoreStepEnum(String step, Integer stepNum, String stepDescription) {
        this.step = step;
        this.stepNum = stepNum;
        this.stepDescription = stepDescription;
    }

    public static String findStepDescriptionByStep(String step) {
        RestoreStepEnum restoreStepEnum = stepEnumMap.get(step);
        if (restoreStepEnum == null) {
            return "";
        }
        return restoreStepEnum.getStepDescription();
    }

    public static Integer findStepNumByStep(String step) {
        RestoreStepEnum restoreStepEnum = stepEnumMap.get(step);
        if (restoreStepEnum == null) {
            return 1;
        }
        return restoreStepEnum.getStepNum();
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    public void setStepDescription(String stepDescription) {
        this.stepDescription = stepDescription;
    }

    public Integer getStepNum() {
        return stepNum;
    }

    public void setStepNum(Integer stepNum) {
        this.stepNum = stepNum;
    }
}
