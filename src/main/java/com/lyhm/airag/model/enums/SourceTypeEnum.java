package com.lyhm.airag.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 来源类型枚举
 */
@Getter
public enum SourceTypeEnum {

    PDF("PDF 文档", "pdf"),
    TXT("纯文本", "txt"),
    MD("Markdown", "md"),
    DOCX("Word 文档", "docx"),
    TEXT("手动输入文本", "text");

    private final String text;
    private final String value;

    SourceTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static SourceTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SourceTypeEnum anEnum : SourceTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
