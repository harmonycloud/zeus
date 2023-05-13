package com.middleware.zeus.util.excel;

import org.apache.poi.ss.util.CellRangeAddress;

import java.io.Serializable;

/**
 * 需要合并的单元格
 */
public class ExcelBean implements Serializable {

    // 单元格填充值
    private String value;

    // 单元格合并规则
    private CellRangeAddress callRangeAddress;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public CellRangeAddress getCallRangeAddress() {
        return callRangeAddress;
    }

    public void setCallRangeAddress(CellRangeAddress callRangeAddress) {
        this.callRangeAddress = callRangeAddress;
    }
}

