package com.zjrealtech.tracedemo.dao;

import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;

import java.util.List;

public interface MaterialFlowRecordDao {

    List<MaterialFlowRecordModel> getFlowRecordsByDestSnapshotId(List<String> snapshotIdList);
}
