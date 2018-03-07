package com.zjrealtech.tracedemo.domain.enums;

import lombok.extern.slf4j.Slf4j;

/**
 * 追溯操作类型枚举定义
 * 日期：2017/12/29
 * @author hooger
 * @version V1.0
 */
@Slf4j
public enum TraceOpTypeEnum {
    //车间操作
    INPUT(1, "投料"),
    TURN_IN(2, "结转转入"),
    OUTPUT(6, "产出"),
    TURN_OUT(7, "结转转出"),
    RETURN_MATERIAL(8, "退料"),
    ADJUSTMENT_WORKSHOP(11, "车间调整"),
    REWORK_IN(14, "返工入站"),
    REWORK_OUT(15, "返工出站"),


    //仓库操作
    STOCK_TRANSFER(101, "库存转储"),
    STOCK_IN(102, "入库"),
    STOCK_OUT(103, "出库"),
//    PRODUCE_IN(104, "生产入库"),
//    PRODUCE_OUT(105, "生产发料"),
//    PRODUCE_BACK(106, "生产退料"),
//    SALE_BACK(107, "销售退库"),
//    SALE_OUT(108, "销售出库"),
//    PURCHASE_IN(109, "采购入库"),
//    PURCHASE_BACK(110, "采购退库"),
    LOSS_OR_GAINS(111, "库存损益"),
    STOCK_ADJUSTMENT(112, "库存调整"),
//    TRANSFER_IN(113, "调拔入库"),
//    TRANSFER_OUT(114, "调拔出库"),
//    STOCK_OVER(115, "成品发货"),
    AUTO_IN(116, "自动入库"),
    AUTO_OUT(117, "自动出库"),

    //结转、送检和质检
    TURN_INOUT(10002, "结转"),
    QC_SUBMIT(10003, "送检"),
    QC_QUALITY(10004, "质检"),

    //条码管理
    BARCODE_BINDING(205, "条码绑定"),
    BARCODE_UNBIND(206, "条码解绑"),
    CONTAINER_CLEAR(202, "容器清空"),
    CONTAINER_ADD(203, "补料"),
    MODIFY_FAILURE_TIME(204, "修改失效时间");

    /** 车间操作类型区间段
        WAREHOUSE_SECTION_BEGIN(101, "车间操作类型区间起点"),
        WAREHOUSE_SECTION_END(199, "车间操作类型区间结束");
     */
    public static int WAREHOUSE_SECTION_BEGIN = 101;
    public static int WAREHOUSE_SECTION_END = 199;

    private final int code;

    private final String name;

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    TraceOpTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static TraceOpTypeEnum getEnumByCode(int operType) {
        for (TraceOpTypeEnum traceOperTypeEnum : values()) {
            if (traceOperTypeEnum.getCode() == operType) {
                return traceOperTypeEnum;
            }
        }
        return null;
    }

    public static String getNameByCode(int operType) {
        for (TraceOpTypeEnum traceOperTypeEnum : values()) {
            if (traceOperTypeEnum.getCode() == operType) {
                return traceOperTypeEnum.getName();
            }
        }
        return null;
    }
}
