package com.lyhm.airag.model.dto.notebook;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建笔记本请求 DTO
 * <p>
 * 前端调用创建笔记本接口时传递的参数。
 * 使用 DTO（Data Transfer Object）模式将请求参数与实体类解耦，
 * 避免暴露不需要的字段（如 id、createTime 等），也方便做参数校验。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
public class NotebookAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 笔记本标题（必填）
     */
    private String title;

    /**
     * 笔记本描述（可选）
     */
    private String description;
}
