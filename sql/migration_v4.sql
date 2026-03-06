-- ===========================
-- AI RAG Notebook v4 迁移 SQL
-- 功能：保存笔记表 + 笔记本摘要字段
-- 执行前请先备份数据库！
-- ===========================

USE lyhm_ai_rag;

-- 1. notebook 表新增 summary 字段（精选卡片摘要简介）
ALTER TABLE `notebook`
    ADD COLUMN `summary` VARCHAR(300) COMMENT '摘要简介（精选首页展示用）'
    AFTER `coverImage`;

-- 2. 新建 savedNote 表（AI对话保存为笔记）
CREATE TABLE IF NOT EXISTS `savedNote`
(
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `userId`         BIGINT      NOT NULL               COMMENT '所属用户ID',
    `notebookId`     BIGINT                             COMMENT '来源笔记本ID',
    `title`          VARCHAR(200)                       COMMENT '笔记标题',
    `content`        TEXT        NOT NULL               COMMENT 'AI回答正文',
    `sourceQuestion` VARCHAR(500)                       COMMENT '触发该回答的原始问题',
    `createTime`     DATETIME    NOT NULL DEFAULT NOW() COMMENT '创建时间',
    `updateTime`     DATETIME    NOT NULL DEFAULT NOW() COMMENT '更新时间',
    `isDelete`       TINYINT(1)  NOT NULL DEFAULT 0     COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_notebookId` (`notebookId`)
) COMMENT = 'AI对话保存的笔记';
