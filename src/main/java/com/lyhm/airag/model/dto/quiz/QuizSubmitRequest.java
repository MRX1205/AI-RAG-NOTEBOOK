package com.lyhm.airag.model.dto.quiz;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 测验提交答案请求 DTO
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
public class QuizSubmitRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 测验 ID（必填）
     */
    private Long quizId;

    /**
     * 用户答案列表（必填）
     */
    private List<QuizAnswer> answers;

    /**
     * 答题用时（秒）
     */
    private Integer timeCost;

    /**
     * 单个题目的答案
     */
    @Data
    public static class QuizAnswer implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 题目编号
         */
        private Integer questionId;

        /**
         * 用户选择的答案（A/B/C/D）
         */
        private String answer;
    }
}
