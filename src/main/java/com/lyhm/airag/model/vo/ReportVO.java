package com.lyhm.airag.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报告视图对象
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
public class ReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long notebookId;

    private String title;

    private String reportType;

    /**
     * 报告内容（Markdown 格式）
     */
    private String content;

    private String sourceIds;

    private LocalDateTime createTime;
}
