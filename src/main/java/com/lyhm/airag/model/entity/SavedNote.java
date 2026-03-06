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
 * AI对话保存的笔记实体类
 * <p>
 * 用户在 RAG 对话中保存的 AI 回答，形成个人知识卡片集合。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("savedNote")
public class SavedNote implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 所属用户ID
     */
    @Column("userId")
    private Long userId;

    /**
     * 来源笔记本ID（可空）
     */
    @Column("notebookId")
    private Long notebookId;

    /**
     * 笔记标题
     */
    @Column("title")
    private String title;

    /**
     * AI回答正文
     */
    @Column("content")
    private String content;

    /**
     * 触发该回答的原始问题
     */
    @Column("sourceQuestion")
    private String sourceQuestion;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除（0-正常，1-已删除）
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
