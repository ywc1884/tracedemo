package com.zjrealtech.tracedemo.dao;

import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;
import com.zjrealtech.tracedemo.mapper.MaterialFlowRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class MaterialFlowRecordDaoImpl implements MaterialFlowRecordDao {

    @Autowired
    private MaterialFlowRecordMapper materialFlowRecordMapper;

    @Override
    public List<MaterialFlowRecordModel> getFlowRecordsByDestSnapshotId(List<String> snapshotIdList) {
        return materialFlowRecordMapper.selectNextRecordsByDestSnapshotId(snapshotIdList);
    }
}


