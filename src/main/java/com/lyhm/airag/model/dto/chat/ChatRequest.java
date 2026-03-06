package com.lyhm.airag.model.dto.chat;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 对话请求 DTO
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
public class ChatRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属笔记本 ID（必填）
     */
    private Long notebookId;

    /**
     * 用户消息（必填）
     */
    private String message;

    /**
     * 选中的来源 ID 列表（可选，为空则使用全部来源）
     */
    private List<Long> sourceIds;
}
