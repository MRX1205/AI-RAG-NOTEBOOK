package com.lyhm.airag.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 报告类型枚举
 */
@Getter
public enum ReportTypeEnum {

    BRIEFING("简报文档", "briefing"),
    STUDY_GUIDE("学习指南", "study_guide"),
    FAQ("常见问题", "faq"),
    TIMELINE("时间线", "timeline"),
    CUSTOM("自定义报告", "custom");

    private final String text;
    private final String value;

    ReportTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ReportTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ReportTypeEnum anEnum : ReportTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
