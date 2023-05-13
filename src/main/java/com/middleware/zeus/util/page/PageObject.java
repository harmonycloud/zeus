package com.middleware.zeus.util.page;

import java.util.List;

/**
 * @author chwetion
 * @since 2020/11/27 10:56 上午
 */
public class PageObject<T> {
    private List<T> data;
    private Integer count;

    public PageObject(List<T> data, Integer count) {
        this.data = data;
        this.count = count;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "PageObject{" +
                "data=" + data +
                ", count=" + count +
                '}';
    }
}
