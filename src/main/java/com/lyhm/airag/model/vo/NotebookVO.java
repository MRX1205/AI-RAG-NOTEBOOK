package com.lyhm.airag.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 笔记本视图对象（View Object）
 */
@Data
public class NotebookVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 笔记本ID
     */
    private Long id;

    /**
     * 笔记本标题
     */
    private String title;

    /**
     * 笔记本描述
     */
    private String description;

    /**
     * 来源数量
     */
    private Integer sourceCount;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 是否精选（0-否, 1-是）
     */
    private Integer isFeatured;

    /**
     * 所属用户名（管理员视图用）
     */
    private String userName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
