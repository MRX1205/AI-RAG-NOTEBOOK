package com.lyhm.airag.model.dto.note;

import lombok.Data;

import java.io.Serializable;

/**
 * 保存笔记请求体
 */
@Data
public class SaveNoteRequest implements Serializable {

    /**
     * 来源笔记本ID（可选）
     */
    private Long notebookId;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * AI回答正文（必填）
     */
    private String content;

    /**
     * 触发该回答的原始问题
     */
    private String sourceQuestion;
}
