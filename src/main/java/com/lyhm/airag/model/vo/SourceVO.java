package com.lyhm.airag.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 来源视图对象
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
public class SourceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long notebookId;

    /**
     * 文件名/来源名称
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文本分块数量
     */
    private Integer segmentCount;

    /**
     * 处理状态：processing/completed/failed
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    private LocalDateTime createTime;
}
