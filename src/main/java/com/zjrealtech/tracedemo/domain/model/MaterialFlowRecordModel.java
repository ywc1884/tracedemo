package com.zjrealtech.tracedemo.domain.model;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class MaterialFlowRecordModel {
    /**
     * ETL时间
     */
    private Date updateTime;
    /**
     * 数据源ID
     */
    private Integer sourceId;
    /**
     * 数据源名称
     */
    private String sourceName;
    /**
     * 主键
     */
    private Integer id;
    /**
     * 业务表主键
     */
    private Integer tid;
    /**
     * 源物料快照ID
     */
    private String srcSnapshotId;
    /**
     * 源物料扣减数量
     */
    private BigDecimal srcDeductionNum;
    /**
     * 操作类型
     */
    private Integer opType;
    /**
     * 操作类型名
     */
    private String opTypeName;
    /**
     * 出入库类型
     */
    private Integer stockType;
    /**
     * 出入库类型名
     */
    private String stockTypeName;
    /**
     * 物料状态
     */
    private Integer materialStatus;
    /**
     * 物料状态名
     */
    private String materialStatusName;
    /**
     * 操作记录ID
     */
    private String opId;
    /**
     * 操作批处理ID
     */
    private String opBatchId;
    /**
     * 目标物料快照ID
     */
    private String destSnapshotId;
    /**
     * 记录产生时间
     */
    private Date recordTime;
    /**
     * 目标物料编码
     */
    private String materialCode;
    /**
     * 目标物料名称
     */
    private String materialName;
    /**
     * 目标物料规格
     */
    private String materialSpec;
    /**
     * 目标物料单位
     */
    private String materialUnit;
    /**
     * 目标物料批次
     */
    private String batchNo;
    /**
     * 源条码
     */
    private String srcBarcode;
    /**
     * 目标条码
     */
    private String barcode;
    /**
     * 工单号
     */
    private String doCode;
    /**
     * 目标物料数量
     */
    private BigDecimal quantity;
    /**
     * 目标条码物料品次
     */
    private Integer qualityType;
    /**
     * 失败原因ID
     */
    private Integer failReasonId;
    /**
     * 失败原因ID
     */
    private String failReasonName;
    /**
     * 人员编码
     */
    private String personCode;
    /**
     * 人员名称
     */
    private String personName;
    /**
     * 设备ID
     */
    private Integer equipmentId;
    /**
     * 设备编码
     */
    private String equipmentCode;
    /**
     * 设备名称
     */
    private String equipmentName;
    /**
     * 模号
     */
    private String moldCode;
    /**
     * 工序编码
     */
    private String processCode;
    /**
     * 工序名称
     */
    private String processName;
    /**
     * 工序序号
     */
    private String processSeq;
    /**
     * 班次
     */
    private String shiftName;
    /**
     * 班次开始时间
     */
    private Date shiftStartTime;
    /**
     * 班次结束时间
     */
    private Date shiftEndTime;
    /**
     * 父工单ID
     */
    private Integer parentDoId;
    /**
     * 父工序编码
     */
    private String parentProcessCode;
    /**
     * 父工序名称
     */
    private String parentProcessName;
    /**
     * 原仓库
     */
    private String srcWarehouse;
    /**
     * 原库位
     */
    private String srcWarehouseLocation;
    /**
     * 新仓库
     */
    private String warehouse;
    /**
     * 新库位
     */
    private String warehouseLocation;
    /**
     * 客户、供应商编号
     */
    private String contactNo;
    /**
     * 客户、供应商名称
     */
    private String contactName;
    /**
     * 操作时间
     */
    private Date opTime;
    /**
     *
     */
    private Integer srcDeductionBatchId;
    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 以下非数据库字段
     * 目标物料剩余数量
     */
    private BigDecimal remainQuantity;
    /**
     * 源工单号
     */
    private String srcDoCode;

    /**
     * 构造测试数据
     *
     * @param srcSnapshotId   源快照Id
     * @param srcDeductionNum 源扣减数量
     * @param opType          操作类型
     * @param destSnapshotId  目标快照Id
     * @param materialCode    物料编码
     * @param materialName    物料名称
     * @param processCode     工序编码
     * @param processName     工序名称
     * @param quantity        数量
     * @param qualityType     质量类型
     */
    public MaterialFlowRecordModel(Integer id, String srcSnapshotId, int srcDeductionNum, Integer opType, String destSnapshotId, String materialCode, String materialName, String processCode, String processName, int quantity, int qualityType) {
        this.id = id;
        this.srcSnapshotId = srcSnapshotId;
        this.srcDeductionNum = new BigDecimal(srcDeductionNum);
        this.opType = opType;
        this.destSnapshotId = destSnapshotId;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.processCode = processCode;
        this.processName = processName;
        this.quantity = new BigDecimal(quantity);
        this.qualityType = qualityType;
    }

    public MaterialFlowRecordModel() {
    }

    @SuppressWarnings("ConstantConditions")
    public Date safeGetShiftStartTime() {
        return shiftStartTime == null ? opTime : shiftStartTime;
    }

    @SuppressWarnings("ConstantConditions")
    public Date safeGetShiftEndTime() {
        return shiftEndTime == null ? opTime : shiftEndTime;
    }

    public BigDecimal getQuantity() {
        return quantity == null ? BigDecimal.ZERO : quantity;
    }

    public BigDecimal getSrcDeductionNum() {
        return srcDeductionNum == null ? BigDecimal.ZERO : srcDeductionNum;
    }

    public boolean isSame(MaterialFlowRecordModel model) {
        if (this == model) return true;
        return (srcSnapshotId != null ? srcSnapshotId.equals(model.srcSnapshotId) : model.srcSnapshotId == null) && (destSnapshotId != null ? destSnapshotId.equals(model.destSnapshotId) : model.destSnapshotId == null);
    }
}