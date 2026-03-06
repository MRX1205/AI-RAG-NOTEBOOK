-- ===========================
-- AI RAG Notebook v3 迁移 SQL
-- 功能：用户状态字段（启用/禁用）
-- 执行前请先备份数据库！
-- ===========================

use lyhm_ai_rag;

-- 1. user 表新增 status 字段（账号状态：0-禁用 1-启用）
--    若字段已存在则此语句会报错，可忽略或注释掉
ALTER TABLE `user`
    ADD COLUMN `status` TINYINT(1) NOT NULL DEFAULT 1
        COMMENT '账号状态：0-禁用，1-启用'
    AFTER `userRole`;
