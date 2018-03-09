package com.zjrealtech.tracedemo.mapper;

import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MaterialFlowRecordMapper {
    /**
     * 根据目标快照Id列表查询下一批物料流转记录
     * @param snapshotIdList 目标快照Id列表
     * @return 物料流转记录
     */
    List<MaterialFlowRecordModel> getNextFlowRecordsByDestSnapshotId(@Param("snapshotIdList") String snapshotIdList);

    /**
     * 根据目标快照Id列表查询对应的物料流转记录
     * @param snapshotIdList 目标快照Id列表
     * @return 物料流转记录
     */
    List<MaterialFlowRecordModel> getFlowRecordsByDestSnapshotId(@Param("snapshotIdList") String snapshotIdList);

    /**
     * 根据目标快照Id列表查询下一批物料流转记录
     * @param srcSnapshotIdList 源快照Id列表
     * @return 物料流转记录
     */
    List<MaterialFlowRecordModel> getPreviousFlowRecordsBySrcSnapshotId(@Param("srcSnapshotIdList") String srcSnapshotIdList);
}