package com.zjrealtech.tracedemo;

import com.zjrealtech.tracedemo.dao.MaterialFlowRecordDao;
import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class TraceDownDemo {

    @Autowired
    MaterialFlowRecordDao materialFlowRecordDao;

    public void sqlTest(){
        String snapshotId = "D38542D9-050E-4B1A-9D7B-E8D14B599410";
         List<MaterialFlowRecordModel> models = materialFlowRecordDao.getFlowRecordsByDestSnapshotId(Collections.singletonList(snapshotId));
         models.forEach(model -> log.info("the barcode is: {}", model.getBarcode()));
    }

    public void traceDownTest(){

    }
}
