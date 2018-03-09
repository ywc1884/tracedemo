package com.zjrealtech.tracedemo.dao;

import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;

import java.util.List;

public interface MaterialFlowRecordDao {

    List<MaterialFlowRecordModel> getNextFlowRecordsByDestSnapshotId(List<String> snapshotIdList);

    List<MaterialFlowRecordModel> getFlowRecordsByDestSnapshotId(List<String> snapshotId);

    public List<MaterialFlowRecordModel> getPreviousFlowRecordsBySrcSnapshotId(List<String> srcSnapshotIdList);
}
