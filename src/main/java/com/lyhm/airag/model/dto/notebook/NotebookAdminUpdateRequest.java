package com.lyhm.airag.model.dto.notebook;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员修改笔记本请求
 */
@Data
public class NotebookAdminUpdateRequest implements Serializable {

    /**
     * 笔记本标题
     */
    private String title;

    /**
     * 笔记本描述
     */
    private String description;

    /**
     * 摘要简介（精选首页展示用）
     */
    private String summary;

    /**
     * 是否精选（0-否, 1-是）
     */
    private Integer isFeatured;
}
