package com.zjrealtech.tracedemo.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 主图节点明细DTO定义
 * 日期：2017/12/29
 *
 * @author hooger
 * @version V1.0
 */
@Data
public class TraceDetailInfoDto {
    /**
     * 操作记录ID
     */
    private String opId;
    /**
     * 操作类型
     */
    private int opType;
    /**
     * 目标快照ID
     */
    private String snapshotId;
    /**
     * 物料编码
     */
    private String materialCode;
    /**
     * 物料名称
     */
    private String materialName;
    /**
     * 批次
     */
    private String batchNo;
    /**
     * 设备ID
     */
    private Integer equipmentId;
    /**
     * 设备名称
     */
    private String equipmentName;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 滞留数量
     */
    private BigDecimal remainQuantity;
    /**
     * 质量类型
     */
    private Integer qualityType;
    /**
     * 质量类型名称
     */
    private String qualityTypeName;
    /**
     * 班次开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date shiftStartTime;
    /**
     * 班次结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date shiftEndTime;
    /**
     * 原仓库
     */
    private String srcWarehouse;
    /**
     * 原仓库库位
     */
    private String srcWarehouseLocation;
    /**
     * 目标仓库
     */
    private String destWarehouse;
    /**
     * 目标仓库库位
     */
    private String destWarehouseLocation;
    /**
     * 源条码
     */
    private String srcBarcode;
    /**
     * 目标条码
     */
    private String barcode;
    /**
     * 调整数量
     */
    private BigDecimal adjustQuantity;
    /**
     * 工单
     */
    private String doCode;
    /**
     * 源工单
     */
    private String srcDoCode;

    @Override
    public int hashCode() {
        return this.snapshotId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (null != obj && obj instanceof TraceDetailInfoDto) {
            TraceDetailInfoDto detailInfo = (TraceDetailInfoDto) obj;
            return this.snapshotId.equals(detailInfo.snapshotId);
        }
        return false;
    }
}
