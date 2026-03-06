package com.lyhm.airag.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开精选笔记本详情 VO（用于未登录用户浏览，不包含敏感信息）
 * <p>
 * 包含笔记本基本信息 + 来源列表（只读预览）
 * </p>
 */
@Data
public class PublicNotebookDetailVO implements Serializable {

    /**
     * 笔记本 ID
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
     * 摘要简介
     */
    private String summary;

    /**
     * 封面图片 URL
     */
    private String coverImage;

    /**
     * 所属用户名
     */
    private String userName;

    /**
     * 来源数量
     */
    private Integer sourceCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 来源列表（只读，展示文件名、类型等元数据）
     */
    private List<SourceVO> sources;
}
