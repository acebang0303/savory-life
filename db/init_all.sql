-- ==========================================
-- 知味生活 · SavoryLife
-- 一键初始化所有数据库
-- 用法: mysql -u root -p < init_all.sql
-- ==========================================

SOURCE 01_auth.sql;
SOURCE 02_user.sql;
SOURCE 03_merchant.sql;
SOURCE 04_trade.sql;
SOURCE 05_market.sql;
SOURCE 06_social.sql;

-- pgvector 请在 PostgreSQL 中单独执行:
-- psql -U postgres -d savory_ai -f 07_pgvector.sql
