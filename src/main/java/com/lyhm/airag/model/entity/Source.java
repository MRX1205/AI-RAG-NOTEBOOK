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
 * 知识来源实体类
 * <p>
 * 来源是用户上传到笔记本中的文档或文本。
 * 每条来源记录对应一个上传的文件或手动输入的文本。
 * 上传后会经过：文件存储 → 文档解析 → 文本分块 → 向量化 → 存入向量数据库。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("source")
public class Source implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法生成）
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 所属笔记本ID
     */
    @Column("notebookId")
    private Long notebookId;

    /**
     * 上传用户ID
     */
    @Column("userId")
    private Long userId;

    /**
     * 文件名/来源名称
     */
    @Column("fileName")
    private String fileName;

    /**
     * 文件类型：pdf/txt/md/docx/text
     */
    @Column("fileType")
    private String fileType;

    /**
     * 文件存储路径（文件上传时有值）
     */
    @Column("filePath")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @Column("fileSize")
    private Long fileSize;

    /**
     * 来源的纯文本内容（用于预览）
     */
    @Column("content")
    private String content;

    /**
     * 文本分块数量
     */
    @Column("segmentCount")
    private Integer segmentCount;

    /**
     * 处理状态：processing/completed/failed
     */
    @Column("status")
    private String status;

    /**
     * 处理失败的错误信息
     */
    @Column("errorMessage")
    private String errorMessage;

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
