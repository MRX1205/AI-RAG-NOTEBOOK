package com.lyhm.airag.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测验答题记录实体类
 * <p>
 * 记录用户每次答题的详细信息和成绩。
 * answers 字段以 JSON 格式存储用户的答案详情。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("quiz_record")
public class QuizRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 关联测验 ID
     */
    @Column("quizId")
    private Long quizId;

    /**
     * 答题用户 ID
     */
    @Column("userId")
    private Long userId;

    /**
     * 得分（百分制）
     */
    @Column("score")
    private Integer score;

    /**
     * 正确题数
     */
    @Column("correctCount")
    private Integer correctCount;

    /**
     * 总题数
     */
    @Column("totalCount")
    private Integer totalCount;

    /**
     * 答题用时（秒）
     */
    @Column("timeCost")
    private Integer timeCost;

    /**
     * 用户答案详情 JSON
     */
    @Column("answers")
    private String answers;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 是否删除（逻辑删除）
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
