package com.lyhm.airag.model.dto.note;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改笔记请求体
 */
@Data
public class UpdateNoteRequest implements Serializable {

    /**
     * 新标题（可选）
     */
    private String title;

    /**
     * 新内容（可选）
     */
    private String content;
}
