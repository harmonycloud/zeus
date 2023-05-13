package com.middleware.zeus.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 中间件备份进度枚举
 *
 * @author liyinlong
 * @since 2021/11/17 9:22 上午
 */
public enum BackupStepEnum {

    FILLUP("step_fillup", 1, "获取恢复组件信息"),
    RUN_JOB("step_run_job", 2, "正在运行备份任务"),
    FINISH("step_finish", 3, "备份结束");

    private String step;

    private Integer stepNum;

    private String stepDescription;

    private static Map<String, BackupStepEnum> stepEnumMap = new HashMap<>();

    static {
        stepEnumMap.put(FILLUP.step, FILLUP);
        stepEnumMap.put(RUN_JOB.step, RUN_JOB);
        stepEnumMap.put(FINISH.step, FINISH);
    }

    BackupStepEnum(String step, Integer stepNum, String stepDescription) {
        this.step = step;
        this.stepNum = stepNum;
        this.stepDescription = stepDescription;
    }

    public static String findStepDescriptionByStep(String step) {
        BackupStepEnum restoreStepEnum = stepEnumMap.get(step);
        if (restoreStepEnum == null) {
            return "";
        }
        return restoreStepEnum.getStepDescription();
    }

    public static Integer findStepNumByStep(String step) {
        BackupStepEnum restoreStepEnum = stepEnumMap.get(step);
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

    public static Map<String, BackupStepEnum> getStepEnumMap() {
        return stepEnumMap;
    }

    public static void setStepEnumMap(Map<String, BackupStepEnum> stepEnumMap) {
        BackupStepEnum.stepEnumMap = stepEnumMap;
    }

    public Integer getStepNum() {
        return stepNum;
    }

    public void setStepNum(Integer stepNum) {
        this.stepNum = stepNum;
    }
}
