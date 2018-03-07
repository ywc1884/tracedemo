package com.zjrealtech.tracedemo.domain.model;

import lombok.Data;

/**
 * 选择起点进行追溯service层对象定义
 * 日期：2017/12/29
 * @author hooger
 * @version V1.0
 */
@Data
public class StartPointTraceInfo {
    private String snapshotId;
    private String materialCode;
    private String batchNo;
}
