package com.lyhm.airag.model.dto.source;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 添加文本来源请求 DTO
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
public class SourceTextAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属笔记本 ID（必填）
     */
    private Long notebookId;

    /**
     * 来源标题（必填）
     */
    private String title;

    /**
     * 文本内容（必填）
     */
    private String content;
}
