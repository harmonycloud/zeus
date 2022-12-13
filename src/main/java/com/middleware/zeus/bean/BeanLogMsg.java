package com.middleware.zeus.bean;


/**
 * Created by ttx on 2019/7/11.
 * 日志输出对象
 */
public class BeanLogMsg {

    private long offset;
    private String timestamp;
    private String msg;

    public BeanLogMsg(long offset, String timestamp, String msg) {
        this.offset = offset;
        this.timestamp = timestamp;
        this.msg = msg;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
