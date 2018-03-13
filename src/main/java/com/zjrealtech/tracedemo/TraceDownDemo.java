package com.zjrealtech.tracedemo;

import com.zjrealtech.tracedemo.dao.MaterialFlowRecordDao;
import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;
import com.zjrealtech.tracedemo.domain.model.StartPointTraceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Component
@Slf4j
public class TraceDownDemo {

    @Autowired
    MaterialFlowRecordDao materialFlowRecordDao;

    @Value("${split.count}")
    private int splitCount;
    public static String startPointsKey = "start_points";

    public void sqlTest() {
        String snapshotId = "D38542D9-050E-4B1A-9D7B-E8D14B599410";
        List<MaterialFlowRecordModel> models = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(Collections.singletonList(snapshotId));
        models.forEach(model -> log.info("the barcode is: {}", model.getBarcode()));
    }

    public void traceDownTest() {
        try {
            long start = System.currentTimeMillis();
            String startPointDestSnapshotId = "D38542D9-050E-4B1A-9D7B-E8D14B599410";
            StartPointTraceInfo startPointTraceInfo = new StartPointTraceInfo();
            startPointTraceInfo.setSnapshotId(startPointDestSnapshotId);
            Map<String, List<MaterialFlowRecordModel>> traceRecordsMap = getTraceRecords(Collections.singletonList(startPointTraceInfo), true);

            long millis = System.currentTimeMillis() - start;
            System.out.println(String.format("it totally took %s to trace down", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))));

            int traceDownRecordsCount = traceRecordsMap.values().stream().map(List::size).reduce(0, (l1, l2) -> l1 + l2);
            System.out.println("the total record count in trace down map is: " + traceDownRecordsCount);

//            //draw graph
//            long drawStart = System.currentTimeMillis();
//            StartPointTraceInfo startPointTraceInfo = new StartPointTraceInfo();
//            startPointTraceInfo.setSnapshotId(startPointDestSnapshotId);
//            TraceGraphUtil.drawTraceDownGraph(Collections.singletonList(startPointTraceInfo), traceDownRecordsMap, downStartPointsToFilter);
//            long drawTime = System.currentTimeMillis() - drawStart;
//            System.out.println(String.format("it totally took %s to draw graph", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(drawTime), TimeUnit.MILLISECONDS.toSeconds(drawTime) -
//                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(drawTime)))));
//
//            //list all time to draw action nodes
//            System.out.println(String.format("it totally took %s to find existing node", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.findExistingNodeTime),
//                    TimeUnit.MILLISECONDS.toSeconds(TraceGraphUtil.findExistingNodeTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.findExistingNodeTime)))));
//            System.out.println(String.format("it totally took %s to find node to merge", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.findNodeToMergeTime),
//                    TimeUnit.MILLISECONDS.toSeconds(TraceGraphUtil.findNodeToMergeTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.findNodeToMergeTime)))));
//            System.out.println(String.format("it totally took %s in action node recursion", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.actionNodeRecursionTime),
//                    TimeUnit.MILLISECONDS.toSeconds(TraceGraphUtil.actionNodeRecursionTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(TraceGraphUtil.actionNodeRecursionTime)))));
        } catch (Exception e) {
            log.error("exception in traceDownTest", e);
        }
    }

    private Map<String, List<MaterialFlowRecordModel>> getTraceRecords(List<StartPointTraceInfo> startPointTraceInfoList, boolean traceDown) {
        //set used to filter out duplicate flow records based on src_snapshot_id + dest_snapshot_id
        Set<String> duplicateRecordsFilterSet = new HashSet<>();
        //final records map to return containing all flow records
        Map<String, List<MaterialFlowRecordModel>> traceRecordsMap = new HashMap<>();
        //start points filter list
        Set<String> startPointsFilterSet = new HashSet<>();

        try {
            //get start point flow records
            List<String> startPointsSnapshotIdList = startPointTraceInfoList.stream().map(StartPointTraceInfo::getSnapshotId).collect(Collectors.toList());
            List<MaterialFlowRecordModel> startPointsRecords = materialFlowRecordDao.getFlowRecordsByDestSnapshotId(startPointsSnapshotIdList);
            //add the start point flow record keys into duplicate record filter set
            startPointsRecords.forEach(record -> duplicateRecordsFilterSet.add(getSnapshotKey(record.getSrcSnapshotId(), record.getDestSnapshotId())));

            //call traceRecords to recursively find all flow records
            traceRecords(startPointsRecords, traceRecordsMap, duplicateRecordsFilterSet, startPointsFilterSet, traceDown);

            //filter out the found duplicate start point flow records and add to final trace down map
            List<MaterialFlowRecordModel> filteredStartPointsRecords = new ArrayList<>();
            for (MaterialFlowRecordModel startPointRecord : startPointsRecords) {
                if (!startPointsFilterSet.contains(startPointRecord.getDestSnapshotId())) {
                    filteredStartPointsRecords.add(startPointRecord);
                }
            }
            if (traceDown) {
                traceRecordsMap.put(startPointsKey, filteredStartPointsRecords);
            } else {
                List<MaterialFlowRecordModel> tmpStartPointsRecordList;
                for (MaterialFlowRecordModel record : filteredStartPointsRecords) {
                    String destSnapshotId = record.getDestSnapshotId();
                    if (!traceRecordsMap.containsKey(destSnapshotId)) {
                        tmpStartPointsRecordList = new ArrayList<>(Collections.singletonList(record));
                    } else {
                        tmpStartPointsRecordList = traceRecordsMap.get(destSnapshotId);
                        tmpStartPointsRecordList.add(record);
                    }
                    traceRecordsMap.put(destSnapshotId, tmpStartPointsRecordList);
                }
            }
        } catch (Exception e) {
            log.error("exception in getTraceRecords", e);
        }

        return traceRecordsMap;
    }

    private void traceRecords(List<MaterialFlowRecordModel> currentFlowRecords, Map<String, List<MaterialFlowRecordModel>> traceRecordsMap,
                              Set<String> duplicateRecordsFilterSet, Set<String> startPointsFilterSet, boolean traceDown) {
        if (currentFlowRecords.size() > splitCount) {
            for (int i = 0, j = splitCount; i < currentFlowRecords.size(); i += splitCount, j += splitCount) {
                j = j > currentFlowRecords.size() ? currentFlowRecords.size() : j;
                List<MaterialFlowRecordModel> subList = new ArrayList<>(currentFlowRecords.subList(i, j));
                if (!CollectionUtils.isEmpty(subList)) {
                    traceRecords(subList, traceRecordsMap, duplicateRecordsFilterSet, startPointsFilterSet, traceDown);
                }
            }
        } else {
            List<MaterialFlowRecordModel> nextFlowRecords;
            List<MaterialFlowRecordModel> filteredNextFlowRecords = new ArrayList<>();

            try {
                Set<String> snapshotIds = new HashSet<>();
                for (MaterialFlowRecordModel record : currentFlowRecords) {
                    if (traceDown) {
                        snapshotIds.add(record.getDestSnapshotId());
                    } else {
                        snapshotIds.add(record.getSrcSnapshotId());
                    }
                }

                if (traceDown) {
                    nextFlowRecords = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(new ArrayList<>(snapshotIds));
                } else {
                    nextFlowRecords = materialFlowRecordDao.getPreviousFlowRecordsBySrcSnapshotId(new ArrayList<>(snapshotIds));
                }

                if (!CollectionUtils.isEmpty(nextFlowRecords)) {
                    nextFlowRecords.forEach(nextRecord -> {
                        String destSnapshotId = nextRecord.getDestSnapshotId();
                        String srcSnapshotId = nextRecord.getSrcSnapshotId();
                        String snapshotKey = getSnapshotKey(srcSnapshotId, destSnapshotId);
                        if (!duplicateRecordsFilterSet.contains(snapshotKey)) {
                            duplicateRecordsFilterSet.add(snapshotKey);
                            filteredNextFlowRecords.add(nextRecord);
                        } else {
                            //keep a record of duplicate records with duplicate dest snapshot id, used to filter start points
                            startPointsFilterSet.add(destSnapshotId);
                        }
                    });

                    //add all the next level records into the final map
                    String snapshotId = traceDown ? nextFlowRecords.get(0).getSrcSnapshotId() : nextFlowRecords.get(0).getDestSnapshotId();
                    List<MaterialFlowRecordModel> nextRecords;
                    if (!traceRecordsMap.containsKey(snapshotId)) {
                        nextRecords = new ArrayList<>(nextFlowRecords);
                    } else {
                        nextRecords = traceRecordsMap.get(snapshotId);
                        nextRecords.addAll(nextFlowRecords);
                    }
                    traceRecordsMap.put(snapshotId, nextRecords);
                }

                if (!CollectionUtils.isEmpty(filteredNextFlowRecords)) {
                    traceRecords(filteredNextFlowRecords, traceRecordsMap, duplicateRecordsFilterSet, startPointsFilterSet, traceDown);
                }
            } catch (Exception e) {
                log.error("exception in traceRecords", e);
            }
        }
    }

    private String getSnapshotKey(String srcId, String destId) {
        return srcId + "@" + destId;
    }

    public void traceUpTest() {
        try {
            long start = System.currentTimeMillis();
            String startPointDestSnapshotId = "FB316F10-6694-4CB4-A04A-C9B3010BE9E1";
            StartPointTraceInfo startPointTraceInfo = new StartPointTraceInfo();
            startPointTraceInfo.setSnapshotId(startPointDestSnapshotId);
            //recursively trace up to get all flow records
            Map<String, List<MaterialFlowRecordModel>> traceRecordsMap = getTraceRecords(Collections.singletonList(startPointTraceInfo), false);

            long millis = System.currentTimeMillis() - start;
            System.out.println(String.format("it totally took %s to trace up", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))));

            int traceDownRecordsCount = traceRecordsMap.values().stream().map(List::size).reduce(0, (l1, l2) -> l1 + l2);
            System.out.println("the total record count in trace up map is: " + traceDownRecordsCount);
        } catch (Exception e) {
            log.error("exception in traceUpTest", e);
        }
    }
//    @Async
//    public CompletableFuture<List<MaterialFlowRecordModel>> getNextFlowRecordsAsync(List<String> snapshotIdList) {
//        List<MaterialFlowRecordModel> nextFlowRecords = new ArrayList<>();
//        try {
//            nextFlowRecords = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(snapshotIdList);
//        } catch (Exception e) {
//            log.error("exception in getNextFlowRecordsAsync", e);
//        }
//
//        return CompletableFuture.completedFuture(nextFlowRecords);
//    }

//    private void traceDownRecordsAsync(List<MaterialFlowRecordModel> currentFlowRecords, int level) {
//        List<MaterialFlowRecordModel> nextFlowRecords = new ArrayList<>();
//        try {
//            List<String> snapshotIdList = new ArrayList<>();
//            for (MaterialFlowRecordModel record : currentFlowRecords) {
//                snapshotIdList.add(record.getDestSnapshotId());
//            }
//            snapshotIdList = removeDuplicateSnapshotIds(snapshotIdList);
//            if (snapshotIdList.size() > splitCount) {
//                List<CompletableFuture<List<MaterialFlowRecordModel>>> futures = new ArrayList<>();
//                //if record size larger than split count, split the query into multiple query
//                for (int i = 0, j = splitCount; i < currentFlowRecords.size(); i += splitCount, j += splitCount) {
//                    j = j > currentFlowRecords.size() ? currentFlowRecords.size() : j;
//                    List<String> subList = new ArrayList<>(snapshotIdList.subList(i, j));
//                    if (!subList.isEmpty()) {
//                        CompletableFuture<List<MaterialFlowRecordModel>> future = getNextFlowRecordsAsync(subList);
//                        futures.add(future);
//                    }
//                }
//
//                //wait for all threads to complete
//                CompletableFuture[] futureArrays = futures.toArray(new CompletableFuture[futures.size()]);
//                CompletableFuture.allOf(futureArrays).join();
//
//                //merge all results into a single list
//                for (CompletableFuture<List<MaterialFlowRecordModel>> future : futures) {
//                    nextFlowRecords.addAll(future.get());
//                }
//            } else {
//                nextFlowRecords = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(snapshotIdList);
//            }
//
//            finalRecordsList.addAll(nextFlowRecords);
//
//        } catch (Exception e) {
//            log.error("exception in traceRecords", e);
//        }
//
//        if (!CollectionUtils.isEmpty(nextFlowRecords)) {
//            traceRecords(nextFlowRecords, level + 1);
//        }
//    }
}
