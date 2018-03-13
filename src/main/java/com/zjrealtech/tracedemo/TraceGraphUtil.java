package com.zjrealtech.tracedemo;


import com.zjrealtech.tracedemo.domain.dto.TraceDetailInfoDto;
import com.zjrealtech.tracedemo.domain.dto.TraceGraphNodeDto;
import com.zjrealtech.tracedemo.domain.enums.QualityTypeEnum;
import com.zjrealtech.tracedemo.domain.enums.TraceNodeTypeEnum;
import com.zjrealtech.tracedemo.domain.enums.TraceOpTypeEnum;
import com.zjrealtech.tracedemo.domain.model.MaterialFlowRecordModel;
import com.zjrealtech.tracedemo.domain.model.StartPointTraceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
@Slf4j
public class TraceGraphUtil {

    public static long findExistingNodeTime;
    public static long findNodeToMergeTime;
    public static long actionNodeRecursionTime;

    public static List<TraceGraphNodeDto> downGenerateActionNodesByStartPoints(List<StartPointTraceInfo> startPointTraceInfoList, Map<String, List<MaterialFlowRecordModel>> traceResult) {
        List<TraceGraphNodeDto> nodeDtoList = new ArrayList<>();
        if (traceResult != null) {
            List<MaterialFlowRecordModel> startPointRecords = traceResult.get(TraceDownDemo.startPointsKey);
            for (StartPointTraceInfo startPointTraceInfo : startPointTraceInfoList) {
                // 获取起点对应的流转记录
                List<MaterialFlowRecordModel> startPointFlowRecords = startPointRecords.stream()
                        .filter(record -> record.getDestSnapshotId().equalsIgnoreCase(startPointTraceInfo.getSnapshotId()))
                        .collect(Collectors.toList());

                //从起点开始生成动作节点
                downGenerateActionNodes(nodeDtoList, startPointFlowRecords, TraceGraphNodeDto.ROOT_KEY);
            }
        }
        return nodeDtoList;
    }

    private static void downGenerateActionNodes(List<TraceGraphNodeDto> nodeDtoList, List<MaterialFlowRecordModel> flowRecordList, String pointer) {
        for (MaterialFlowRecordModel currentFlowRecord : flowRecordList) {
            // 判断已生成节点中是否存在对应快照ID
            long findExistingNodeStart = System.currentTimeMillis();
            TraceGraphNodeDto existingNode = findExistingNodeBySnapshotId(currentFlowRecord.getDestSnapshotId(), nodeDtoList);
            findExistingNodeTime += System.currentTimeMillis() - findExistingNodeStart;

            if (existingNode != null) {
                //说明物理流上又合并到一个分支上，该节点已经处理过，后续流转记录已经生成了节点
                //只用添加该节点为当前节点的子节点
                existingNode.addDetailInfoList(currentFlowRecord);
                existingNode.addParents(pointer);
            } else {
                //当前节点的子节点中有没有这个类型的节点
                long findNodeMergeStart = System.currentTimeMillis();
                TraceGraphNodeDto nodeDto = findNodeToMerge(pointer, currentFlowRecord.getOpType(), currentFlowRecord.getProcessCode(), currentFlowRecord.getMaterialCode(), nodeDtoList);
                findNodeToMergeTime += System.currentTimeMillis() - findNodeMergeStart;

                if (nodeDto == null) {
                    nodeDto = createTraceGraphActionNode(nodeDtoList, currentFlowRecord, pointer);
                }

                //添加动作详情
                if (null != nodeDto) {
                    nodeDto.addDetailInfoList(currentFlowRecord);

                    long resursionStart = System.currentTimeMillis();

                    //向后递归处理
                    List<MaterialFlowRecordModel> nextFlowRecords = null;//TraceDownDemo.traceDownRecordsMap.get(currentFlowRecord.getDestSnapshotId());

                    //sum the recursion time
                    actionNodeRecursionTime += System.currentTimeMillis() - resursionStart;

                    if (nextFlowRecords != null){
                        downGenerateActionNodes(nodeDtoList, nextFlowRecords, nodeDto.getKey());
                    }
                }
            }
        }
    }

    private static TraceGraphNodeDto findExistingNodeBySnapshotId(String snapshotId, List<TraceGraphNodeDto> nodeDtoList) {
        for (TraceGraphNodeDto nodeDto : nodeDtoList) {
            if (nodeDto.getDetailInfoMap().containsKey(snapshotId))
                return nodeDto;
        }
        return null;
    }

    private static TraceGraphNodeDto findNodeToMerge(String pointer, int opType, String processCode, String materialCode, List<TraceGraphNodeDto> nodeDtoList) {
        List<TraceGraphNodeDto> nodeList = nodeDtoList.stream().filter(node -> node.isChildOfNode(pointer) && node.getOpType() == opType).collect(Collectors.toList());

        if (!nodeList.isEmpty()) {
            if (opType == TraceOpTypeEnum.INPUT.getCode() || opType == TraceOpTypeEnum.OUTPUT.getCode()) {
                return nodeList.stream().filter(node -> node.getCode().equals(processCode)).findAny().orElse(null);
            } else {
                return nodeList.stream().filter(node -> materialCode.equals(node.getMaterialCode())).findAny().orElse(null);
            }
        }
        return null;
    }

    private static TraceGraphNodeDto createTraceGraphActionNode(List<TraceGraphNodeDto> nodeDtoList, MaterialFlowRecordModel currentFlowRecord, String parent) {
        try {
            Assert.notNull(currentFlowRecord.getMaterialCode(), "material_code must not be null");
        } catch (Exception e) {
            return null;
        }
        int opType = currentFlowRecord.getOpType();
        TraceGraphNodeDto nodeDto = new TraceGraphNodeDto();
        nodeDto.setKey(String.valueOf(nodeDtoList.size() + 1));
        nodeDto.setParents(parent);
        nodeDto.setOpType(opType);
        nodeDto.setNodeType(opType);

        //设置编码和名称
        if (opType == TraceOpTypeEnum.INPUT.getCode() || opType == TraceOpTypeEnum.OUTPUT.getCode()) {
            //投入、产出动作节点的编码和名字设置为 工序编码、工序名称
            nodeDto.setCode(currentFlowRecord.getProcessCode());
            nodeDto.setName(currentFlowRecord.getProcessName());

            //产线型工序相关信息
            nodeDto.setGroupCode(currentFlowRecord.getParentProcessCode());
            nodeDto.setGroupName(currentFlowRecord.getParentProcessName());
        } else {
            //其他的分别设置为 动作类型 和 动作名称
            nodeDto.setCode(String.valueOf(opType));
            nodeDto.setName(currentFlowRecord.getOpTypeName());

            if (opType == TraceOpTypeEnum.TURN_INOUT.getCode()) {
                nodeDto.setNodeType(TraceNodeTypeEnum.TURN.getCode());
            }

            nodeDto.setMaterialCode(currentFlowRecord.getMaterialCode());
        }
        nodeDtoList.add(nodeDto);

        return nodeDto;
    }

    public static void downAddMaterialNodes(List<TraceGraphNodeDto> nodeDtoList, String pointer, List<String> pointerList) {
        if (!nodeDtoList.isEmpty()) {
            if (pointerList.contains(pointer)) {
                return;
            }
            pointerList.add(pointer);

            TraceGraphNodeDto pointerNode = findNodeByKey(nodeDtoList, pointer);

            //查找当前节点下面的子节点
            List<TraceGraphNodeDto> childNodes = nodeDtoList.stream().
                    filter(nodeDto -> nodeDto.isChildOfNode(pointer) && nodeDto.getOpType() != 0).
                    collect(Collectors.toList());

            if (childNodes.isEmpty()) {
                //叶子节点特殊处理
                assert pointerNode != null;
                if (pointerNode.getOpType() == TraceOpTypeEnum.INPUT.getCode()) {
                    //在叶子节点前面加上物料节点
                    addMaterialBeforeInNode(nodeDtoList, pointerNode, null);
                } else if (pointerNode.getOpType() != TraceOpTypeEnum.CONTAINER_CLEAR.getCode()) {
                    //在叶子节点后面加上物料节点
                    addMaterialNodeAfterOutNode(nodeDtoList, pointerNode.getDetailInfoMap().values(), pointer);
                }
            } else {
                //遍历处理每一个子节点
                for (TraceGraphNodeDto currentNode : childNodes) {
                    //当前节点的节点类型为产出时，找前面的投入或者转入节点
                    if (currentNode.getOpType() == TraceOpTypeEnum.OUTPUT.getCode()) {
                        //判断投产物料是否相同
                        if (null != pointerNode && pointerNode.getOpType() == TraceOpTypeEnum.INPUT.getCode()) {
                            addMaterialBeforeInNode(nodeDtoList, pointerNode, currentNode);
                        }
                        //处理废品
                        Set<TraceDetailInfoDto> detailInfos = currentNode.getDetailInfoMap().values().stream().
                                filter(detailInfo -> detailInfo.getQualityType().equals(QualityTypeEnum.SCRAP.getCode())).
                                collect(Collectors.toSet());
                        addMaterialNodeAfterOutNode(nodeDtoList, detailInfos, currentNode.getKey());
                    }

                    //当前节点作为父节点接着递归
                    downAddMaterialNodes(nodeDtoList, currentNode.getKey(), pointerList);
                }
            }
        }
    }

    private static TraceGraphNodeDto findNodeByKey(List<TraceGraphNodeDto> nodeDtoList, String key) {
        for (TraceGraphNodeDto nodeDto : nodeDtoList) {
            if (nodeDto.getKey().equals(key)) {
                return nodeDto;
            }
        }
        return null;
    }

    private static void addMaterialBeforeInNode(List<TraceGraphNodeDto> nodeDtoList, TraceGraphNodeDto inNode, TraceGraphNodeDto outNode) {
        //默认加在投入节点前面，当找到的是转入节点时，则加在产出节点前面
        TraceGraphNodeDto actionNode = inNode;
        if (inNode.getOpType() == TraceOpTypeEnum.TURN_IN.getCode()) {
            actionNode = outNode;
        }
        //判断投入包含的物料是否已经在产出节点中出现
        for (TraceDetailInfoDto detailInfo : inNode.getDetailInfoMap().values()) {
            String materialCode = detailInfo.getMaterialCode();
            String materialName = detailInfo.getMaterialName();
            if (null == outNode || outNode.getDetailInfoMap().values().stream().noneMatch(dto -> dto.getMaterialCode().equals(materialCode))) {
                //看是否已生成对应的物料节点
                TraceGraphNodeDto materialNode = findMaterialNodeInParents(nodeDtoList, materialCode, actionNode);
                if (materialNode == null) {
                    materialNode = new TraceGraphNodeDto();
                    materialNode.setKey(String.valueOf(nodeDtoList.size() + 1));
                    materialNode.setParents(actionNode.getParents());
                    materialNode.setNodeType(TraceNodeTypeEnum.NORMAL_MATERIAL.getCode());
                    materialNode.setCode(materialCode);
                    materialNode.setName(materialName);
                    //投入节点加上一个父节点
                    if (containMaterialNodeInParents(nodeDtoList, actionNode)) {
                        actionNode.addParents(materialNode.getKey());
                    } else {
                        actionNode.setParents(materialNode.getKey());
                    }
                    nodeDtoList.add(materialNode);
                }
                //有的话就直接加数据
                materialNode.getDetailInfoMap().put(detailInfo.getSnapshotId(), detailInfo);
            }
        }
    }

    private static boolean containMaterialNodeInParents(List<TraceGraphNodeDto> nodeDtoList, TraceGraphNodeDto node) {
        for (TraceGraphNodeDto nodeDto : nodeDtoList) {
            boolean hasMaterialNodeInParents = nodeDto.isParentNode(node.getParents())
                    && (nodeDto.getNodeType() == TraceNodeTypeEnum.NORMAL_MATERIAL.getCode() || node.getNodeType() == TraceNodeTypeEnum.WASTE.getCode());
            if (hasMaterialNodeInParents) {
                return true;
            }
        }
        return false;
    }

    private static TraceGraphNodeDto findMaterialNodeInParents(List<TraceGraphNodeDto> nodeDtoList, String inputMaterialCode, TraceGraphNodeDto node) {
        for (TraceGraphNodeDto nodeDto : nodeDtoList) {
            if (nodeDto.isParentNode(node.getParents()) && nodeDto.getCode().equals(inputMaterialCode)) {
                return nodeDto;
            }
        }
        return null;
    }

    private static void addMaterialNodeAfterOutNode(List<TraceGraphNodeDto> nodeDtoList, Collection<TraceDetailInfoDto> detailInfos, String key) {
        for (TraceDetailInfoDto detailInfo : detailInfos) {
            int nodeType = TraceNodeTypeEnum.NORMAL_MATERIAL.getCode();
            if (null != detailInfo.getQualityType() && detailInfo.getQualityType().equals(QualityTypeEnum.SCRAP.getCode())) {
                nodeType = TraceNodeTypeEnum.WASTE.getCode();
            }
            String outputMaterialCode = detailInfo.getMaterialCode();
            String outputMaterialName = detailInfo.getMaterialName();
            TraceGraphNodeDto materialNode = findMaterialNodeInChildren(nodeDtoList, key, outputMaterialCode, nodeType);
            if (null == materialNode) {
                materialNode = new TraceGraphNodeDto();
                materialNode.setKey(String.valueOf(nodeDtoList.size() + 1));
                materialNode.setParents(key);
                materialNode.setNodeType(nodeType);
                materialNode.setCode(outputMaterialCode);
                materialNode.setName(outputMaterialName);
                nodeDtoList.add(materialNode);
            }
            materialNode.getDetailInfoMap().put(detailInfo.getSnapshotId(), detailInfo);
        }
    }

    private static TraceGraphNodeDto findMaterialNodeInChildren(List<TraceGraphNodeDto> nodeDtoList, String key, String materialCode, int nodeType) {
        return nodeDtoList.stream().
                filter(node -> node.isChildOfNode(key) && node.getCode().equals(materialCode) && node.getNodeType() == nodeType).
                findAny().orElse(null);
    }

    public static void downMergeInOutNodes(List<TraceGraphNodeDto> nodeDtoList, String pointer, List<String> pointerList) {
        if (!nodeDtoList.isEmpty()) {
            if (pointerList.contains(pointer)) {
                return;
            }
            pointerList.add(pointer);

            TraceGraphNodeDto pointerNode = findNodeByKey(nodeDtoList, pointer);

            for (TraceGraphNodeDto currentNode : nodeDtoList.stream().
                    filter(nodeDto -> nodeDto.isChildOfNode(pointer)).
                    collect(Collectors.toList())) {

                //单个节点也要合并，改变节点类型
                boolean singleInNode = currentNode.getOpType() == TraceOpTypeEnum.INPUT.getCode() && nodeDtoList.stream().
                        noneMatch(node -> node.isChildOfNode(currentNode.getKey())
                                && node.getOpType() == TraceOpTypeEnum.OUTPUT.getCode()
                                && node.getCode().equals(currentNode.getCode()));
                boolean singleOutNode = currentNode.getOpType() == TraceOpTypeEnum.OUTPUT.getCode()
                        && !(null != pointerNode && pointerNode.getOpType() == TraceOpTypeEnum.INPUT.getCode() && pointerNode.getCode().equals(currentNode.getCode()));
                if (singleInNode || singleOutNode) {
                    currentNode.setNodeType(TraceNodeTypeEnum.PROCESS.getCode());
                }

                //投入、产出合并
                if (currentNode.getOpType() == TraceOpTypeEnum.OUTPUT.getCode()
                        && null != pointerNode && pointerNode.getCode().equals(currentNode.getCode()) && pointerNode.getOpType() == TraceOpTypeEnum.INPUT.getCode()) {
                    //当前节点的数据加到父节点上
                    currentNode.getDetailInfoMap().forEach(pointerNode.getDetailInfoMap()::putIfAbsent);
                    //合并时改变节点类型
                    pointerNode.setNodeType(TraceNodeTypeEnum.PROCESS.getCode());

                    //当前节点标记为待删除节点
                    currentNode.setIsToBeDelete(true);

                    //子节点接着递归
                    downMergeInOutNodes(nodeDtoList, currentNode.getKey(), pointerList);

                    //子节点的父指向爷爷节点
                    for (TraceGraphNodeDto childNode : nodeDtoList.stream().
                            filter(nodeDto -> nodeDto.isChildOfNode(currentNode.getKey())).
                            collect(Collectors.toList())) {
                        childNode.setParents(childNode.getParents().replace(currentNode.getKey(), pointer));
                    }
                }

                //继续递归
                if (!currentNode.getIsToBeDelete()) {
                    downMergeInOutNodes(nodeDtoList, currentNode.getKey(), pointerList);
                }

            }
        }
    }

    public static List<TraceGraphNodeDto> drawTraceDownGraph(List<StartPointTraceInfo> startPointTraceInfoList, Map<String, List<MaterialFlowRecordModel>> traceResult, Set<String> startPointsToBeDel) {

        //过滤后的起点集合
        List<StartPointTraceInfo> filteredStartPoints = new ArrayList<>();
        //过滤起点
        for (StartPointTraceInfo startPointTraceInfo : startPointTraceInfoList) {
            if (!startPointsToBeDel.contains(startPointTraceInfo.getSnapshotId())) {
                filteredStartPoints.add(startPointTraceInfo);
            }
        }

        //生成动作节点
        long actionNodeStart = System.currentTimeMillis();
        List<TraceGraphNodeDto> nodeDtoList = downGenerateActionNodesByStartPoints(filteredStartPoints, traceResult);
        long drawActionNodeTime = System.currentTimeMillis() - actionNodeStart;
        System.out.println(String.format("it totally took %s to draw action nodes", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(drawActionNodeTime), TimeUnit.MILLISECONDS.toSeconds(drawActionNodeTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(drawActionNodeTime)))));

        //添加物料节点
        long materialNodeStart = System.currentTimeMillis();
        downAddMaterialNodes(nodeDtoList, TraceGraphNodeDto.ROOT_KEY, new ArrayList<>());
        long drawMaterialNodeTime = System.currentTimeMillis() - materialNodeStart;
        System.out.println(String.format("it totally took %s to draw material nodes", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(drawMaterialNodeTime), TimeUnit.MILLISECONDS.toSeconds(drawMaterialNodeTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(drawMaterialNodeTime)))));

        //标记需要合并的节点为待删除
        long mergeNodeStart = System.currentTimeMillis();
        downMergeInOutNodes(nodeDtoList, TraceGraphNodeDto.ROOT_KEY, new ArrayList<>());
        long mergeNodeTime = System.currentTimeMillis() - mergeNodeStart;
        System.out.println(String.format("it totally took %s to merge nodes", String.format("%d mins, %d secs", TimeUnit.MILLISECONDS.toMinutes(mergeNodeTime), TimeUnit.MILLISECONDS.toSeconds(mergeNodeTime) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mergeNodeTime)))));

        return nodeDtoList.stream().filter(nodeDto -> !nodeDto.getIsToBeDelete()).collect(Collectors.toList());
    }
}
