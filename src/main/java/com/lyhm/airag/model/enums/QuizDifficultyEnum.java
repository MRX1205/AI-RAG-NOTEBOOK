package com.lyhm.airag.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 测验难度枚举
 */
@Getter
public enum QuizDifficultyEnum {

    EASY("简单", "easy"),
    MEDIUM("中等", "medium"),
    HARD("困难", "hard");

    private final String text;
    private final String value;

    QuizDifficultyEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static QuizDifficultyEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (QuizDifficultyEnum anEnum : QuizDifficultyEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
