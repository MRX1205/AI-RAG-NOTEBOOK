package com.lyhm.airag.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测验实体类
 * <p>
 * 存储 AI 基于知识库生成的选择题测验。
 * questions 字段以 JSON 格式存储题目数组，包含题目、选项、正确答案和解释。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("quiz")
public class Quiz implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("notebookId")
    private Long notebookId;

    @Column("userId")
    private Long userId;

    /**
     * 测验标题
     */
    @Column("title")
    private String title;

    /**
     * 题目数量
     */
    @Column("questionCount")
    private Integer questionCount;

    /**
     * 难度：easy/medium/hard
     */
    @Column("difficulty")
    private String difficulty;

    /**
     * 题目 JSON 数组
     */
    @Column("questions")
    private String questions;

    /**
     * 参考的来源 ID 列表（逗号分隔）
     */
    @Column("sourceIds")
    private String sourceIds;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
