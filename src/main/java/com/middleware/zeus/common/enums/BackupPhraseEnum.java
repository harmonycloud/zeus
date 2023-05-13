package com.middleware.zeus.common.enums;

/**
 * 备份状态枚举
 * @author liyinlong
 * @since 2021/11/17 9:22 上午
 */
public enum BackupPhraseEnum {
    UNKNOWN("Unknown"),
    FAILED("Failed"),
    RUNNING("Running"),
    SUCCESS("Success"),
    ;

    private String phrase;

    BackupPhraseEnum(String phrase) {
        this.phrase = phrase;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }
}
