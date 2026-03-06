# 基于LangChain4j的RAG知识库系统 — 数据库增量脚本
# 说明：本脚本创建报告和测验功能所需的 5 张新表

use lyhm_ai_rag;

-- ===========================
-- 1. 笔记本表 notebook
-- 说明：用户知识空间的顶层组织单元
-- ===========================
create table if not exists notebook
(
    id           bigint auto_increment comment 'id' primary key,
    userId       bigint                                 not null comment '创建用户id',
    title        varchar(256)                           not null comment '笔记本标题',
    description  varchar(1024)                          null comment '笔记本描述',
    sourceCount  int          default 0                 not null comment '来源数量（冗余字段，避免查询时JOIN）',
    coverImage   varchar(1024)                          null comment '封面图片URL',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除（逻辑删除）',
    INDEX idx_userId (userId)
) comment '笔记本' collate = utf8mb4_unicode_ci;

-- ===========================
-- 2. 知识来源表 source
-- 说明：用户上传到笔记本中的文档或文本
-- ===========================
create table if not exists source
(
    id            bigint auto_increment comment 'id' primary key,
    notebookId    bigint                                 not null comment '所属笔记本id',
    userId        bigint                                 not null comment '上传用户id',
    fileName      varchar(512)                           not null comment '文件名/来源名称',
    fileType      varchar(64)                            not null comment '文件类型：pdf/txt/md/docx/text',
    filePath      varchar(1024)                          null comment '文件存储路径（文件上传时有值）',
    fileSize      bigint       default 0                 not null comment '文件大小（字节）',
    content       mediumtext                             null comment '来源的纯文本内容（用于预览）',
    segmentCount  int          default 0                 not null comment '文档被分块后的数量',
    status        varchar(32)  default 'processing'      not null comment '处理状态：processing/completed/failed',
    errorMessage  varchar(1024)                          null comment '处理失败的错误信息',
    createTime    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint      default 0                 not null comment '是否删除',
    INDEX idx_notebookId (notebookId),
    INDEX idx_userId (userId)
) comment '知识来源' collate = utf8mb4_unicode_ci;

-- ===========================
-- 3. 报告表 report
-- 说明：AI 基于知识库生成的结构化报告
-- ===========================
create table if not exists report
(
    id            bigint auto_increment comment 'id' primary key,
    notebookId    bigint                                 not null comment '所属笔记本id',
    userId        bigint                                 not null comment '生成用户id',
    title         varchar(512)                           not null comment '报告标题',
    reportType    varchar(64)                            not null comment '报告类型：briefing/study_guide/faq/timeline/custom',
    customPrompt  varchar(2048)                          null comment '自定义报告提示词（type=custom时使用）',
    content       mediumtext                             not null comment '报告内容（Markdown格式）',
    sourceIds     varchar(1024)                          null comment '参考的来源id列表（逗号分隔）',
    createTime    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint      default 0                 not null comment '是否删除',
    INDEX idx_notebookId (notebookId),
    INDEX idx_userId (userId)
) comment '报告' collate = utf8mb4_unicode_ci;

-- ===========================
-- 4. 测验表 quiz
-- 说明：AI 基于知识库生成的选择题测验
-- questions 字段存储 JSON 格式题目数组
-- ===========================
create table if not exists quiz
(
    id            bigint auto_increment comment 'id' primary key,
    notebookId    bigint                                 not null comment '所属笔记本id',
    userId        bigint                                 not null comment '生成用户id',
    title         varchar(512)                           not null comment '测验标题',
    questionCount int                                    not null comment '题目数量',
    difficulty    varchar(32)                            not null comment '难度：easy/medium/hard',
    questions     mediumtext                             not null comment '题目JSON数组',
    sourceIds     varchar(1024)                          null comment '参考的来源id列表（逗号分隔）',
    createTime    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint      default 0                 not null comment '是否删除',
    INDEX idx_notebookId (notebookId),
    INDEX idx_userId (userId)
) comment '测验' collate = utf8mb4_unicode_ci;

-- ===========================
-- 5. 测验答题记录表 quiz_record
-- 说明：记录用户每次答题的详情和成绩
-- answers 字段存储 JSON 格式答案详情
-- ===========================
create table if not exists quiz_record
(
    id            bigint auto_increment comment 'id' primary key,
    quizId        bigint                                 not null comment '关联测验id',
    userId        bigint                                 not null comment '答题用户id',
    score         int                                    not null comment '得分（百分制）',
    correctCount  int                                    not null comment '正确题数',
    totalCount    int                                    not null comment '总题数',
    timeCost      int                                    null comment '答题用时（秒）',
    answers       mediumtext                             not null comment '用户答案详情JSON',
    createTime    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    isDelete      tinyint      default 0                 not null comment '是否删除',
    INDEX idx_quizId (quizId),
    INDEX idx_userId (userId)
) comment '测验答题记录' collate = utf8mb4_unicode_ci;
