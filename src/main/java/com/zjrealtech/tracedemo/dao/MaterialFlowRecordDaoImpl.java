package com.zjrealtech.tracedemo.dao;

import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;
import com.zjrealtech.tracedemo.mapper.MaterialFlowRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.StringJoiner;

@Repository
@Slf4j
public class MaterialFlowRecordDaoImpl implements MaterialFlowRecordDao {

    @Autowired
    private MaterialFlowRecordMapper materialFlowRecordMapper;

    @Override
    public List<MaterialFlowRecordModel> getNextFlowRecordsByDestSnapshotId(List<String> snapshotIdList) {
        StringJoiner stringJoiner = new StringJoiner(",", "'", "'");
        snapshotIdList.forEach(stringJoiner::add);
        return materialFlowRecordMapper.getNextFlowRecordsByDestSnapshotId(stringJoiner.toString());
    }

    @Override
    public List<MaterialFlowRecordModel> getFlowRecordsByDestSnapshotId(List<String> snapshotIdList) {
        StringJoiner stringJoiner = new StringJoiner(",", "'", "'");
        snapshotIdList.forEach(stringJoiner::add);
        return materialFlowRecordMapper.getFlowRecordsByDestSnapshotId(stringJoiner.toString());
    }

    @Override
    public List<MaterialFlowRecordModel> getPreviousFlowRecordsBySrcSnapshotId(List<String> srcSnapshotIdList) {
        StringJoiner stringJoiner = new StringJoiner(",", "'", "'");
        srcSnapshotIdList.forEach(stringJoiner::add);
        return materialFlowRecordMapper.getPreviousFlowRecordsBySrcSnapshotId(stringJoiner.toString());
    }
}


