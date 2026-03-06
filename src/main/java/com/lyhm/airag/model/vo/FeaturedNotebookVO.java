package com.lyhm.airag.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 精选笔记本视图对象（公开接口使用）
 * <p>
 * 包含所属用户名，用于首页精选展示区域
 * </p>
 */
@Data
public class FeaturedNotebookVO implements Serializable {

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
     * 摘要简介（精选卡片首要展示内容）
     */
    private String summary;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 所属用户名
     */
    private String userName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
