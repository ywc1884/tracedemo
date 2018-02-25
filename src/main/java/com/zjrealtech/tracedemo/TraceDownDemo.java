package com.zjrealtech.tracedemo;

import com.zjrealtech.tracedemo.dao.MaterialFlowRecordDao;
import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TraceDownDemo {

    @Autowired
    MaterialFlowRecordDao materialFlowRecordDao;

    @Value("${split.count}")
    private int splitCount;

    private List<MaterialFlowRecordModel> finalRecordsList = new ArrayList<>();

    public void sqlTest() {
        String snapshotId = "D38542D9-050E-4B1A-9D7B-E8D14B599410";
        List<MaterialFlowRecordModel> models = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(Collections.singletonList(snapshotId));
        models.forEach(model -> log.info("the barcode is: {}", model.getBarcode()));
    }

    public void traceDownTest() {
        try {
            long start = System.currentTimeMillis();
            String startPointDestSnapshotId = "D38542D9-050E-4B1A-9D7B-E8D14B599410";
            List<MaterialFlowRecordModel> startPointsRecords = materialFlowRecordDao.getFlowRecordsByDestSnapshotId(startPointDestSnapshotId);
            traceDownRecords(startPointsRecords, 0);
            long millis = System.currentTimeMillis() - start;
            System.out.println(String.format("it totally took %s to trace down", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))));

            log.info("the final record count: " + finalRecordsList.size());
        } catch (Exception e) {
            log.error("exception in traceDownTest", e);
        }
    }

    private void traceDownRecords(List<MaterialFlowRecordModel> currentFlowRecords, int level) {
        if (currentFlowRecords.size() > splitCount) {
            for (int i = 0, j = splitCount; i < currentFlowRecords.size(); i += splitCount, j += splitCount) {
                j = j > currentFlowRecords.size() ? currentFlowRecords.size() : j;
                List<MaterialFlowRecordModel> subList = new ArrayList<>(currentFlowRecords.subList(i, j));
                if (!subList.isEmpty()) {
                    traceDownRecords(subList, level + 1);
                }
            }
        } else {
            List<MaterialFlowRecordModel> nextFlowRecords = new ArrayList<>();
            try {
                List<String> snapshotIdList = new ArrayList<>();
                currentFlowRecords.forEach(record -> snapshotIdList.add(record.getDestSnapshotId()));
                long start = System.currentTimeMillis();
                nextFlowRecords = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(snapshotIdList);
                long millis = System.currentTimeMillis() - start;
                System.out.println(String.format("it took %s to execute the query", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))));

                if (!CollectionUtils.isEmpty(nextFlowRecords)) {

                    List<MaterialFlowRecordModel> nextFilterRecords = new ArrayList<>();

                    nextFlowRecords.forEach(nextRecord -> {
                        if (finalRecordsList.stream().anyMatch(record -> record.getDestSnapshotId().equalsIgnoreCase(nextRecord.getDestSnapshotId()))) {
                            nextFilterRecords.add(nextRecord);
                        }
                    });

                    if (!CollectionUtils.isEmpty(nextFilterRecords)) {
                        nextFlowRecords.removeAll(nextFilterRecords);
                    }

                    finalRecordsList.addAll(nextFlowRecords);
                }

            } catch (Exception e) {
                log.error("exception in traceDownRecords", e);
            }

            if (!CollectionUtils.isEmpty(nextFlowRecords)) {
                traceDownRecords(nextFlowRecords, level + 1);
            }
        }
    }

    @Async
    public CompletableFuture<List<MaterialFlowRecordModel>> getNextFlowRecordsAsync(List<String> snapshotIdList) {
        List<MaterialFlowRecordModel> nextFlowRecords = new ArrayList<>();
        try {
            nextFlowRecords = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(snapshotIdList);
        } catch (Exception e) {
            log.error("exception in getNextFlowRecordsAsync", e);
        }

        return CompletableFuture.completedFuture(nextFlowRecords);
    }

    private void traceDownRecordsAsync(List<MaterialFlowRecordModel> currentFlowRecords, int level) {
        List<MaterialFlowRecordModel> nextFlowRecords = new ArrayList<>();
        try {
            List<String> snapshotIdList = new ArrayList<>();
            currentFlowRecords.forEach(record -> snapshotIdList.add(record.getDestSnapshotId()));
            long start = System.currentTimeMillis();
            if (snapshotIdList.size() > splitCount){
                List<CompletableFuture<List<MaterialFlowRecordModel>>> futures = new ArrayList<>();
                //if record size larger than split count, split the query into multiple query
                for (int i = 0, j = splitCount; i < currentFlowRecords.size(); i += splitCount, j += splitCount) {
                    j = j > currentFlowRecords.size() ? currentFlowRecords.size() : j;
                    List<String> subList = new ArrayList<>(snapshotIdList.subList(i, j));
                    if (!subList.isEmpty()) {
                        CompletableFuture<List<MaterialFlowRecordModel>> future = getNextFlowRecordsAsync(subList);
                        futures.add(future);
                    }
                }

                //wait for all threads to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();

            }
            nextFlowRecords = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(snapshotIdList);
            long millis = System.currentTimeMillis() - start;
            System.out.println(String.format("it took %s to execute the query", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))));

            if (!CollectionUtils.isEmpty(nextFlowRecords)) {

                List<MaterialFlowRecordModel> nextFilterRecords = new ArrayList<>();

                nextFlowRecords.forEach(nextRecord -> {
                    if (finalRecordsList.stream().anyMatch(record -> record.getDestSnapshotId().equalsIgnoreCase(nextRecord.getDestSnapshotId()))) {
                        nextFilterRecords.add(nextRecord);
                    }
                });

                if (!CollectionUtils.isEmpty(nextFilterRecords)) {
                    nextFlowRecords.removeAll(nextFilterRecords);
                }

                finalRecordsList.addAll(nextFlowRecords);
            }

        } catch (Exception e) {
            log.error("exception in traceDownRecords", e);
        }

        if (!CollectionUtils.isEmpty(nextFlowRecords)) {
            traceDownRecords(nextFlowRecords, level + 1);
        }
    }
}
