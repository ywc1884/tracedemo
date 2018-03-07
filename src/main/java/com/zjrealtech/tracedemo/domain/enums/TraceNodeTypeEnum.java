package com.zjrealtech.tracedemo.domain.enums;

/**
 * 追溯主图节点类型定义
 * 日期：2017/12/29
 * @author hooger
 * @version V1.0
 */
public enum TraceNodeTypeEnum {
    //主图节点类型
    PROCESS(10001, "工序"),
    TURN(10002, "结转"),
    NORMAL_MATERIAL(10003, "正常物料"),
    WASTE(10004, "废品");

    private final int code;

    private final String name;

    TraceNodeTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
