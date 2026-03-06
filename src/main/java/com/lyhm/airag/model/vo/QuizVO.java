package com.lyhm.airag.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测验视图对象
 * <p>
 * 注意：返回给前端时，questions 字段中不包含 correctAnswer 和 explanation，
 * 正确答案仅在提交答案后返回。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
public class QuizVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long notebookId;

    private String title;

    private Integer questionCount;

    private String difficulty;

    /**
     * 题目 JSON 数组
     * <p>
     * 生成时不含答案、提交后或查看详情时含答案
     * </p>
     */
    private String questions;

    private String sourceIds;

    private LocalDateTime createTime;
}
