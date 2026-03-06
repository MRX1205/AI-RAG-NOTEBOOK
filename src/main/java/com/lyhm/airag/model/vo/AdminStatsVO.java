package com.lyhm.airag.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 管理员仪表盘统计数据 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsVO implements Serializable {

    /** 注册用户总数 */
    private Long userCount;

    /** 笔记本总数 */
    private Long notebookCount;

    /** 文档来源总数 */
    private Long sourceCount;
}
