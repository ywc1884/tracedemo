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

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Component
@Slf4j
public class TraceDownDemo {

    @Autowired
    MaterialFlowRecordDao materialFlowRecordDao;

    @Value("${split.count}")
    private int splitCount;

    private Set<String> downDuplicateRecordFilterSet = new HashSet<>();
    private Set<String> upDuplicateRecordFilterSet = new HashSet<>();
    public static Map<String, List<MaterialFlowRecordModel>> traceDownRecordsMap = new HashMap<>();
    public static Map<String, List<MaterialFlowRecordModel>> traceUpRecordsMap = new HashMap<>();
    private Set<String> downStartPointsToFilter = new HashSet<>();
    private Set<String> upStartPointsToFilter = new HashSet<>();
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
            //get start point flow records
            List<MaterialFlowRecordModel> startPointsRecords = materialFlowRecordDao.getFlowRecordsByDestSnapshotId(Collections.singletonList(startPointDestSnapshotId));
            //add the start point flow records into duplicate record filter set
            startPointsRecords.forEach(record -> downDuplicateRecordFilterSet.add(getSnapshotKey(record.getSrcSnapshotId(), record.getDestSnapshotId())));
            //recursively trace down to get all flow records
            traceRecords(startPointsRecords);

            log.info("the duplicate record filter set size: " + downDuplicateRecordFilterSet.size());

            //filter out the found duplicate start point flow records and add to final trace down map
            List<MaterialFlowRecordModel> filteredStartPointsRecords = new ArrayList<>();
            for (MaterialFlowRecordModel startPointRecord : startPointsRecords){
                if(!downStartPointsToFilter.contains(startPointRecord.getDestSnapshotId())){
                    filteredStartPointsRecords.add(startPointRecord);
                }
            }
            traceDownRecordsMap.put(startPointsKey, filteredStartPointsRecords);

            long millis = System.currentTimeMillis() - start;
            System.out.println(String.format("it totally took %s to trace down", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))));

            int traceDownRecordsCount = traceDownRecordsMap.values().stream().map(List::size).reduce(0, (l1, l2) -> l1 + l2);
            System.out.println("the total record count in trace down map is: " + traceDownRecordsCount);

            //draw graph
            long drawStart = System.currentTimeMillis();
            StartPointTraceInfo startPointTraceInfo = new StartPointTraceInfo();
            startPointTraceInfo.setSnapshotId(startPointDestSnapshotId);
            TraceGraphUtil.drawTraceDownGraph(Collections.singletonList(startPointTraceInfo), traceDownRecordsMap, downStartPointsToFilter);
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

//    private Map<String, List<MaterialFlowRecordModel>> getTraceRecords(List<StartPointTraceInfo> startPointTraceInfoList){
//        try {
//            //get start point flow records
//            List<MaterialFlowRecordModel> startPointsRecords = materialFlowRecordDao.getFlowRecordsByDestSnapshotId(startPointDestSnapshotId);
//            //add the start point flow records into duplicate record filter set
//            startPointsRecords.forEach(record -> downDuplicateRecordFilterSet.add(getSnapshotKey(record.getSrcSnapshotId(), record.getDestSnapshotId())));
//        } catch (Exception e){
//
//        }
//    }

    private void traceRecords(List<MaterialFlowRecordModel> currentFlowRecords) {
        if (currentFlowRecords.size() > splitCount) {
            for (int i = 0, j = splitCount; i < currentFlowRecords.size(); i += splitCount, j += splitCount) {
                j = j > currentFlowRecords.size() ? currentFlowRecords.size() : j;
                List<MaterialFlowRecordModel> subList = new ArrayList<>(currentFlowRecords.subList(i, j));
                if (!CollectionUtils.isEmpty(subList)) {
                    traceRecords(subList);
                }
            }
        } else {
            List<MaterialFlowRecordModel> filteredNextFlowRecords = new ArrayList<>();
            List<MaterialFlowRecordModel> nextFlowRecords = new ArrayList<>();

            try {
                Set<String> snapshotIds = new HashSet<>();
                for (MaterialFlowRecordModel record : currentFlowRecords) {
                    snapshotIds.add(record.getDestSnapshotId());
                }
                nextFlowRecords = materialFlowRecordDao.getNextFlowRecordsByDestSnapshotId(new ArrayList<>(snapshotIds));

                if (!CollectionUtils.isEmpty(nextFlowRecords)) {
                    nextFlowRecords.forEach(nextRecord -> {
                        String destSnapshotId = nextRecord.getDestSnapshotId();
                        String srcSnapshotId = nextRecord.getSrcSnapshotId();
                        String snapshotKey = getSnapshotKey(srcSnapshotId, destSnapshotId);
                        if (!downDuplicateRecordFilterSet.contains(snapshotKey)) {
                            filteredNextFlowRecords.add(nextRecord);
                            downDuplicateRecordFilterSet.add(snapshotKey);
                        } else {
                            //keep a record of duplicate records with duplicate dest snapshot id, used to filter start points
                            downStartPointsToFilter.add(destSnapshotId);
                        }
                    });

                }
            } catch (Exception e) {
                log.error("exception in traceRecords", e);
            }

            if (!CollectionUtils.isEmpty(nextFlowRecords)) {
                String srcSnapshotId = nextFlowRecords.get(0).getSrcSnapshotId();
                List<MaterialFlowRecordModel> nextRecords;
                if (!traceDownRecordsMap.containsKey(srcSnapshotId)){
                    nextRecords = new ArrayList<>(nextFlowRecords);
                } else {
                    nextRecords = traceDownRecordsMap.get(srcSnapshotId);
                    nextRecords.addAll(nextFlowRecords);
                }
                traceDownRecordsMap.put(srcSnapshotId, nextRecords);

                traceRecords(filteredNextFlowRecords);
            }
        }
    }

    private String getSnapshotKey(String srcId, String destId){
        return srcId + "@" + destId;
    }

    public void traceUpTest(){
        try {
            long start = System.currentTimeMillis();
            String startPointDestSnapshotId = "FB316F10-6694-4CB4-A04A-C9B3010BE9E1";
            //get start point flow records
            List<MaterialFlowRecordModel> startPointsRecords = materialFlowRecordDao.getFlowRecordsByDestSnapshotId(Collections.singletonList(startPointDestSnapshotId));
            //add the start point flow records into duplicate record filter set
            startPointsRecords.forEach(record -> upDuplicateRecordFilterSet.add(getSnapshotKey(record.getSrcSnapshotId(), record.getDestSnapshotId())));
            //recursively trace up to get all flow records
            traceUpRecords(startPointsRecords);

            log.info("the duplicate record filter set size: " + upDuplicateRecordFilterSet.size());

            //filter out the found duplicate start point flow records and add to final trace down map
            List<MaterialFlowRecordModel> filteredStartPointsRecords = new ArrayList<>();
            for (MaterialFlowRecordModel startPointRecord : startPointsRecords){
                if(!upStartPointsToFilter.contains(startPointRecord.getDestSnapshotId())){
                    filteredStartPointsRecords.add(startPointRecord);
                }
            }
            List<MaterialFlowRecordModel> tmpList;
            for(MaterialFlowRecordModel record : filteredStartPointsRecords){
                String destSnapshotId = record.getDestSnapshotId();
                if (!traceUpRecordsMap.containsKey(destSnapshotId)){
                    tmpList = new ArrayList<>(Collections.singletonList(record));
                } else {
                    tmpList = traceUpRecordsMap.get(destSnapshotId);
                    tmpList.add(record);
                }
                traceUpRecordsMap.put(destSnapshotId, tmpList);
            }

            long millis = System.currentTimeMillis() - start;
            System.out.println(String.format("it totally took %s to trace up", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))));

            int traceDownRecordsCount = traceUpRecordsMap.values().stream().map(List::size).reduce(0, (l1, l2) -> l1 + l2);
            System.out.println("the total record count in trace up map is: " + traceDownRecordsCount);
        } catch (Exception e){

        }
    }

    private void traceUpRecords(List<MaterialFlowRecordModel> currentFlowRecords) {
        if (currentFlowRecords.size() > splitCount) {
            for (int i = 0, j = splitCount; i < currentFlowRecords.size(); i += splitCount, j += splitCount) {
                j = j > currentFlowRecords.size() ? currentFlowRecords.size() : j;
                List<MaterialFlowRecordModel> subList = new ArrayList<>(currentFlowRecords.subList(i, j));
                if (!CollectionUtils.isEmpty(subList)) {
                    traceUpRecords(subList);
                }
            }
        } else {
            List<MaterialFlowRecordModel> filteredPreviousFlowRecords = new ArrayList<>();
            List<MaterialFlowRecordModel> previousFlowRecords = new ArrayList<>();

            try {
                Set<String> snapshotIds = new HashSet<>();
                for (MaterialFlowRecordModel record : currentFlowRecords) {
                    snapshotIds.add(record.getSrcSnapshotId());
                }
                previousFlowRecords = materialFlowRecordDao.getPreviousFlowRecordsBySrcSnapshotId(new ArrayList<>(snapshotIds));

                if (!CollectionUtils.isEmpty(previousFlowRecords)) {
                    previousFlowRecords.forEach(previousRecord -> {
                        String destSnapshotId = previousRecord.getDestSnapshotId();
                        String srcSnapshotId = previousRecord.getSrcSnapshotId();
                        String snapshotKey = getSnapshotKey(srcSnapshotId, destSnapshotId);
                        if (!upDuplicateRecordFilterSet.contains(snapshotKey)) {
                            filteredPreviousFlowRecords.add(previousRecord);
                            upDuplicateRecordFilterSet.add(snapshotKey);
                        } else {
                            //keep a record of duplicate records with duplicate dest snapshot id, used to filter start points
                            upStartPointsToFilter.add(destSnapshotId);
                        }
                    });

                }
            } catch (Exception e) {
                log.error("exception in traceRecords", e);
            }

            if (!CollectionUtils.isEmpty(previousFlowRecords)) {
                String destSnapshotId = previousFlowRecords.get(0).getDestSnapshotId();
                List<MaterialFlowRecordModel> previousRecords;
                if (!traceUpRecordsMap.containsKey(destSnapshotId)){
                    previousRecords = new ArrayList<>(previousFlowRecords);
                } else {
                    previousRecords = traceUpRecordsMap.get(destSnapshotId);
                    previousRecords.addAll(previousFlowRecords);
                }
                traceUpRecordsMap.put(destSnapshotId, previousRecords);

                traceUpRecords(filteredPreviousFlowRecords);
            }
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
