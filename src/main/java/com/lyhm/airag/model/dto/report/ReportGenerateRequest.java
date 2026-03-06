package com.lyhm.airag.model.dto.report;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 报告生成请求 DTO
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
public class ReportGenerateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属笔记本 ID（必填）
     */
    private Long notebookId;

    /**
     * 报告类型：briefing/study_guide/faq/timeline/custom（必填）
     */
    private String reportType;

    /**
     * 选中的来源 ID 列表（可选，为空则使用全部来源）
     */
    private List<Long> sourceIds;

    /**
     * 自定义报告提示词（reportType=custom 时必填）
     */
    private String customPrompt;
}
