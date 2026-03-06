-- ===========================
-- AI RAG Notebook 新功能迁移 SQL
-- 执行前请先备份数据库！
-- ===========================

-- 1. notebook 表新增 isFeatured 字段
--    （若字段已存在则跳过，MySQL 8.0+ 支持 IF NOT EXISTS 通过 PROCEDURE）
ALTER TABLE notebook
    ADD COLUMN isFeatured TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '是否精选: 0-否 1-是'
    AFTER coverImage;

-- 2. 确认 user 表中 userAvatar 字段存在（应已存在，无需变更）
-- SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'userAvatar';

-- 3. （可选）为精选查询建索引，提升 GET /notebook/featured 性能
ALTER TABLE notebook ADD INDEX idx_isFeatured (isFeatured);
