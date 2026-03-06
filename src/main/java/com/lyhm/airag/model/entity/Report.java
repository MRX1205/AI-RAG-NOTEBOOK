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
 * 报告实体类
 * <p>
 * 存储用户通过 AI 基于知识库生成的各类报告。
 * 支持简报、学习指南、FAQ、时间线、自定义等类型。
 * 报告内容以 Markdown 格式存储。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("report")
public class Report implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("notebookId")
    private Long notebookId;

    @Column("userId")
    private Long userId;

    /**
     * 报告标题
     */
    @Column("title")
    private String title;

    /**
     * 报告类型：briefing/study_guide/faq/timeline/custom
     */
    @Column("reportType")
    private String reportType;

    /**
     * 自定义报告提示词（type=custom 时使用）
     */
    @Column("customPrompt")
    private String customPrompt;

    /**
     * 报告内容（Markdown 格式）
     */
    @Column("content")
    private String content;

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
