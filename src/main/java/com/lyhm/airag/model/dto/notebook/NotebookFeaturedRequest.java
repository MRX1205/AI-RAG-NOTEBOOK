package com.lyhm.airag.model.dto.notebook;

import lombok.Data;

import java.io.Serializable;

/**
 * 切换精选状态请求
 */
@Data
public class NotebookFeaturedRequest implements Serializable {

    /**
     * 是否精选（0-否, 1-是）
     */
    private Integer isFeatured;
}
