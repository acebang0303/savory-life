-- ==========================================
-- 知味生活 · SavoryLife
-- 数据库: savory_market (营销数据库)
-- ==========================================

CREATE DATABASE IF NOT EXISTS savory_market DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE savory_market;

-- 秒杀活动表
CREATE TABLE seckill_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL COMMENT '活动名称',
    dish_id BIGINT NOT NULL COMMENT '秒杀菜品ID',
    seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价格',
    stock INT NOT NULL COMMENT '秒杀库存',
    limit_per_user INT DEFAULT 1 COMMENT '每人限购数量',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status INT DEFAULT 0 COMMENT '状态 0未开始 1进行中 2已结束',
    create_time DATETIME NOT NULL,
    INDEX idx_time (start_time, end_time)
) COMMENT '秒杀活动表';

-- 优惠券模板表
CREATE TABLE coupon_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL COMMENT '优惠券名称',
    type INT NOT NULL COMMENT '类型 1满减券 2折扣券 3现金券',
    threshold DECIMAL(10,2) COMMENT '使用门槛(满多少可用)',
    discount_value DECIMAL(10,2) NOT NULL COMMENT '优惠值(金额或折扣)',
    total_count INT DEFAULT 0 COMMENT '发放总量, 0不限量',
    per_user_limit INT DEFAULT 1 COMMENT '每人限领数量',
    valid_days INT NOT NULL COMMENT '有效期(自领取起X天)',
    status INT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_time DATETIME NOT NULL
) COMMENT '优惠券模板表';

-- 用户优惠券表
CREATE TABLE user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '模板ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status INT DEFAULT 0 COMMENT '状态 0未使用 1已使用 2已过期 3已锁定(下单中)',
    order_id BIGINT COMMENT '使用的订单ID',
    receive_time DATETIME NOT NULL COMMENT '领取时间',
    use_time DATETIME COMMENT '使用时间',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    INDEX idx_user_id (user_id)
) COMMENT '用户优惠券表';
