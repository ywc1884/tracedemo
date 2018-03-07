package com.zjrealtech.tracedemo.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zjrealtech.tracedemo.domain.enums.QualityTypeEnum;
import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;
import lombok.Data;

import java.util.*;

/**
 * 主图节点DTO定义
 * 日期：2017/12/29
 *
 * @author hooger
 * @version V1.0
 */
@Data
public class TraceGraphNodeDto {
    /**
     * 虚拟根节点的key
     */
    public static final String ROOT_KEY = "0";
    /**
     * 分隔符
     */
    private static final String SEPARATOR = ",";
    /**
     * 节点标识
     */
    private String key;
    /**
     * 父节点key列表，以","分隔
     */
    private String parents;
    /**
     * 动作类型：投入、产出、入库、出库
     */
    private int opType;
    /**
     * 节点类型：工序、结转、正常物料、废品
     */
    private int nodeType;
    /**
     * 节点编码
     */
    private String code;
    /**
     * 节点名称
     */
    private String name;
    /**
     * 产线型工序编码
     */
    private String groupCode;
    /**
     * 产线型工序名称
     */
    private String groupName;
    /**
     * 物料编码
     */
    private String materialCode;
    /**
     * 明细数据
     */
    private Map<String, TraceDetailInfoDto> detailInfoMap = new HashMap<>();
    /**
     * 是否待删除节点
     */
    @JsonIgnore
    private Boolean isToBeDelete = false;

    public void addParents(String key) {
        if (parents == null) {
            parents = key;
        }
        //不存在该父，则加上
        else if (!Arrays.asList(parents.split(SEPARATOR)).contains(key)) {
            parents += SEPARATOR + key;
        }
    }

    public void addDetailInfoList(MaterialFlowRecordModel model) {
        TraceDetailInfoDto detailInfo = new TraceDetailInfoDto();
        detailInfo.setOpId(model.getOpId());
        detailInfo.setOpType(model.getOpType());
        detailInfo.setSnapshotId(model.getDestSnapshotId());
        detailInfo.setMaterialCode(model.getMaterialCode());
        detailInfo.setMaterialName(model.getMaterialName());
        detailInfo.setBatchNo(model.getBatchNo());
        detailInfo.setEquipmentId(model.getEquipmentId());
        detailInfo.setEquipmentName(model.getEquipmentName());
        detailInfo.setQuantity(model.getQuantity());
        detailInfo.setRemainQuantity(model.getRemainQuantity());
        detailInfo.setQualityType(model.getQualityType());
        detailInfo.setQualityTypeName(QualityTypeEnum.getNameByCode(model.getQualityType()));
        detailInfo.setShiftStartTime(model.safeGetShiftStartTime());
        detailInfo.setShiftEndTime(model.safeGetShiftEndTime());
        detailInfo.setSrcWarehouse(model.getSrcWarehouse());
        detailInfo.setSrcWarehouseLocation(model.getSrcWarehouseLocation());
        detailInfo.setDestWarehouse(model.getWarehouse());
        detailInfo.setDestWarehouseLocation(model.getWarehouseLocation());
        detailInfo.setSrcBarcode(model.getSrcBarcode());
        detailInfo.setBarcode(model.getBarcode());
        detailInfo.setAdjustQuantity(model.getSrcDeductionNum());
        detailInfo.setDoCode(model.getDoCode());
        detailInfo.setSrcDoCode(model.getSrcDoCode());
        detailInfoMap.put(model.getDestSnapshotId(), detailInfo);
    }

    /**
     * 判断当前节点是否为某个节点的子节点
     *
     * @param key 节点标识
     * @return 是否子节点
     */
    public boolean isChildOfNode(String key) {
        return Arrays.asList(parents.split(SEPARATOR)).contains(key) && !this.key.equals(key);
    }

    /**
     * 判断当前节点是否为某个节点的父节点
     *
     * @param parents 节点的父
     * @return 是否父节点
     */
    public boolean isParentNode(String parents) {
        return Arrays.asList(parents.split(SEPARATOR)).contains(key);
    }
}
