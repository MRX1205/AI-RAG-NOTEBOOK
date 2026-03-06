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
 * 笔记本实体类
 * <p>
 * 笔记本是系统的顶层组织单元，每个用户可以创建多个笔记本。
 * 每个笔记本相当于一个独立的知识空间，包含多个来源（Source）、报告（Report）和测验（Quiz）。
 * 类似 Google NotebookLM 中的 Notebook 概念。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notebook")
public class Notebook implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法生成）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 创建用户ID
     * <p>
     * 关联 user 表，标识该笔记本的所有者
     * </p>
     */
    @Column("userId")
    private Long userId;

    /**
     * 笔记本标题
     * <p>
     * 例如："基于LangChain4j的RAG系统"
     * </p>
     */
    @Column("title")
    private String title;

    /**
     * 笔记本描述
     * <p>
     * 可选，用于补充说明笔记本的用途
     * </p>
     */
    @Column("description")
    private String description;

    /**
     * 来源数量（冗余字段）
     * <p>
     * 为了在列表页快速展示来源数量，避免每次 JOIN 查询 source 表
     * </p>
     */
    @Column("sourceCount")
    private Integer sourceCount;

    /**
     * 封面图片URL
     */
    @Column("coverImage")
    private String coverImage;

    /**
     * 摘要简介（精选首页展示用）
     */
    @Column("summary")
    private String summary;

    /**
     * 是否精选（0-否, 1-是）
     */
    @Column("isFeatured")
    private Integer isFeatured;

    /**
     * 编辑时间
     */
    @Column("editTime")
    private LocalDateTime editTime;

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
     * 是否删除（逻辑删除：0-未删除，1-已删除）
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
