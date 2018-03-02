package com.zjrealtech.tracedemo;

import com.zjrealtech.tracedemo.dao.MaterialFlowRecordDao;
import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;
import com.zjrealtech.tracedemo.domain.model.StartPointTraceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Component
@Slf4j
public class TraceDownDemo {

    @Autowired
    MaterialFlowRecordDao materialFlowRecordDao;

    @Value("${split.count}")
    private int splitCount;

    private List<MaterialFlowRecordModel> finalRecordsList = new ArrayList<>();
    private Map<String, MaterialFlowRecordModel> finalRecordsMap = new HashMap<>();
    private Map<String, Boolean> startPointsToBeDel = new HashMap<>();

    public void sqlTest() {
        String snapshotId = "D38542D9-050E-4B1A-9D7B-E8D14B599410";
        List<MaterialFlowRecordModel> models = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(Collections.singletonList(snapshotId));
        models.forEach(model -> log.info("the barcode is: {}", model.getBarcode()));
    }

    public void traceDownTest() {
        try {
            long start = System.currentTimeMillis();
            String startPointDestSnapshotId = "D38542D9-050E-4B1A-9D7B-E8D14B599410";
            //get start point flow records
            List<MaterialFlowRecordModel> startPointsRecords = materialFlowRecordDao.getFlowRecordsByDestSnapshotId(startPointDestSnapshotId);
            //add the start point flow records into final record list
            //finalRecordsList.addAll(startPointsRecords);
            startPointsRecords.forEach(record -> finalRecordsMap.put(getSnapshotKey(record.getSrcSnapshotId(), record.getDestSnapshotId()), record));
            //recursively trace down to get all flow records
            traceDownRecords(startPointsRecords, 0);

            long millis = System.currentTimeMillis() - start;
            System.out.println(String.format("it totally took %s to trace down", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))));

            log.info("the final record count: " + finalRecordsMap.size());

            //draw graph
            long drawStart = System.currentTimeMillis();
            StartPointTraceInfo startPointTraceInfo = new StartPointTraceInfo();
            startPointTraceInfo.setSnapshotId(startPointDestSnapshotId);
            TraceGraphUtil.drawTraceDownGraph(Collections.singletonList(startPointTraceInfo), finalRecordsMap, startPointsToBeDel);
            long drawTime = System.currentTimeMillis() - drawStart;
            System.out.println(String.format("it totally took %s to draw graph", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(drawTime), TimeUnit.MILLISECONDS.toSeconds(drawTime) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(drawTime)))));

            //list all time to draw action nodes
            System.out.println(String.format("it totally took %s to find existing node", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.findExistingNodeTime),
                    TimeUnit.MILLISECONDS.toSeconds(TraceGraphUtil.findExistingNodeTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.findExistingNodeTime)))));
            System.out.println(String.format("it totally took %s to find node to merge", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.findNodeToMergeTime),
                    TimeUnit.MILLISECONDS.toSeconds(TraceGraphUtil.findNodeToMergeTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.findNodeToMergeTime)))));
            System.out.println(String.format("it totally took %s in action node recursion", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.actionNodeRecursionTime),
                    TimeUnit.MILLISECONDS.toSeconds(TraceGraphUtil.actionNodeRecursionTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.actionNodeRecursionTime)))));
        } catch (Exception e) {
            log.error("exception in traceDownTest", e);
        }
    }

    private void traceDownRecords(List<MaterialFlowRecordModel> currentFlowRecords, int level) {
        if (currentFlowRecords.size() > splitCount) {
            for (int i = 0, j = splitCount; i < currentFlowRecords.size(); i += splitCount, j += splitCount) {
                j = j > currentFlowRecords.size() ? currentFlowRecords.size() : j;
                List<MaterialFlowRecordModel> subList = new ArrayList<>(currentFlowRecords.subList(i, j));
                if (!CollectionUtils.isEmpty(subList)) {
                    traceDownRecords(subList, level + 1);
                }
            }
        } else {
            List<MaterialFlowRecordModel> filteredNextFlowRecords = new ArrayList<>();
            try {
                List<String> snapshotIdList = new ArrayList<>();
                for (MaterialFlowRecordModel record : currentFlowRecords) {
                    snapshotIdList.add(record.getDestSnapshotId());
                }
                snapshotIdList = removeDuplicateSnapshotIds(snapshotIdList);
                List<MaterialFlowRecordModel> nextFlowRecords = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(snapshotIdList);

                if (!CollectionUtils.isEmpty(nextFlowRecords)) {
                    nextFlowRecords.forEach(nextRecord -> {
                        String destSnapshotId = nextRecord.getDestSnapshotId();
                        String srcSnapshotId = nextRecord.getSrcSnapshotId();
                        String snapshotKey = getSnapshotKey(srcSnapshotId, destSnapshotId);
                        if (!finalRecordsMap.containsKey(snapshotKey)) {
                            filteredNextFlowRecords.add(nextRecord);
                            finalRecordsMap.put(snapshotKey, nextRecord);
                        } else {
                            //keep a record of duplicate records with duplicate dest snapshot id, used to filter start points
                            startPointsToBeDel.put(destSnapshotId, true);
                        }
                    });
                }
            } catch (Exception e) {
                log.error("exception in traceDownRecords", e);
            }

            if (!CollectionUtils.isEmpty(filteredNextFlowRecords)) {
                traceDownRecords(filteredNextFlowRecords, level + 1);
            }
        }
    }

    private List<MaterialFlowRecordModel> removeDuplicateFlowRecords(List<MaterialFlowRecordModel> originalRecords) {
        Map<String, MaterialFlowRecordModel> map = new HashMap<>();
        List<MaterialFlowRecordModel> filteredRecords = null;

        try {
            originalRecords.forEach(record -> {
                String destSnapshotId = record.getDestSnapshotId();
                if (!map.containsKey(destSnapshotId)) {
                    map.put(destSnapshotId, record);
                }
            });

            if (!map.isEmpty()) {
                filteredRecords = new ArrayList<>(map.values());
            }
        } catch (Exception e) {
            log.error("exception in removeDuplicateFlowRecords", e);
        }

        return filteredRecords;
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
            for (MaterialFlowRecordModel record : currentFlowRecords) {
                snapshotIdList.add(record.getDestSnapshotId());
            }
            snapshotIdList = removeDuplicateSnapshotIds(snapshotIdList);
            if (snapshotIdList.size() > splitCount) {
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
                CompletableFuture[] futureArrays = futures.toArray(new CompletableFuture[futures.size()]);
                CompletableFuture.allOf(futureArrays).join();

                //merge all results into a single list
                for (CompletableFuture<List<MaterialFlowRecordModel>> future : futures) {
                    nextFlowRecords.addAll(future.get());
                }
            } else {
                nextFlowRecords = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(snapshotIdList);
            }

            finalRecordsList.addAll(nextFlowRecords);

        } catch (Exception e) {
            log.error("exception in traceDownRecords", e);
        }

        if (!CollectionUtils.isEmpty(nextFlowRecords)) {
            traceDownRecords(nextFlowRecords, level + 1);
        }
    }

    private List<String> removeDuplicateSnapshotIds(List<String> snapshotIdList) {
        List<String> snapshotIds = new ArrayList<>();
        try {
            Set<String> hs = new HashSet<>(snapshotIdList);
            snapshotIds.addAll(hs);
        } catch (Exception e) {
            log.error("exception in removeDuplicateSnapshotIds", e);
        }

        return snapshotIds;
    }

    private String getSnapshotKey(String srcId, String destId){
        return srcId + "@" + destId;
    }
}
