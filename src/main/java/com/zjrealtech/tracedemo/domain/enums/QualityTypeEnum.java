package com.zjrealtech.tracedemo.domain.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 质量类型枚举定义
 * 日期：2017/12/29
 * @author hooger
 * @version V1.0
 */
public enum QualityTypeEnum {
    //质量类型
    QUALIFIED(1, "合格"),
    SCRAP(2, "报废"),
    UNQUALIFIED(3, "不合格");

    private final Integer code;

    private final String name;

    private static final Map<Integer, QualityTypeEnum> MAP = new HashMap<>();

    QualityTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    static {
        for (QualityTypeEnum type : QualityTypeEnum.values()) {
            MAP.put(type.getCode(), type);
        }
    }

    public static QualityTypeEnum getEnumByValue(int valueType) {
        for (QualityTypeEnum qualityTypeEnum : values()) {
            if (qualityTypeEnum.getCode() == valueType) {
                return qualityTypeEnum;
            }
        }
        return SCRAP;
    }

    public static String getNameByCode(Integer code) {
        if (MAP.containsKey(code)) {
            return MAP.get(code).getName();
        }
        return "";
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}