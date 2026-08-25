-- ==========================================
-- 知味生活 · SavoryLife
-- 数据库: savory_trade (交易数据库)
-- ==========================================

CREATE DATABASE IF NOT EXISTS savory_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE savory_trade;

-- 购物车表(MySQL兜底, 主存储为Redis)
CREATE TABLE shopping_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    dish_id BIGINT COMMENT '菜品ID',
    setmeal_id BIGINT COMMENT '套餐ID',
    dish_flavor VARCHAR(256) COMMENT '口味',
    name VARCHAR(64) COMMENT '名称',
    image VARCHAR(256) COMMENT '图片',
    amount DECIMAL(10,2) COMMENT '单价',
    number INT DEFAULT 1 COMMENT '数量',
    create_time DATETIME NOT NULL,
    INDEX idx_user_id (user_id)
) COMMENT '购物车表';

-- 订单表
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    number VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    merchant_id BIGINT NOT NULL COMMENT '店铺ID',
    address_book_id BIGINT NOT NULL COMMENT '地址ID',
    address_detail VARCHAR(512) COMMENT '地址快照(冗余)',
    user_coupon_id BIGINT COMMENT '使用的优惠券ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    discount_amount DECIMAL(10,2) DEFAULT 0 COMMENT '优惠金额',
    delivery_fee DECIMAL(10,2) DEFAULT 0 COMMENT '配送费',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    pay_method INT COMMENT '支付方式 1微信支付',
    pay_status INT DEFAULT 0 COMMENT '支付状态 0未支付 1已支付 2已退款',
    status INT DEFAULT 1 COMMENT '订单状态 1待支付 2待接单 3备货中 4待取餐 5已完成 6已取消 7已退款',
    transaction_id VARCHAR(64) COMMENT '微信支付交易号',
    cancel_reason VARCHAR(256) COMMENT '取消原因',
    remark VARCHAR(256) COMMENT '用户备注',
    is_seckill INT DEFAULT 0 COMMENT '是否秒杀订单 1是 0否',
    estimated_delivery_time DATETIME COMMENT '预计送达时间',
    delivery_time DATETIME COMMENT '实际送达时间',
    pay_time DATETIME COMMENT '支付时间',
    cancel_time DATETIME COMMENT '取消时间',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_number (number),
    INDEX idx_status (status)
) COMMENT '订单表';

-- 订单明细表
CREATE TABLE order_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    name VARCHAR(64) COMMENT '菜品/套餐名称',
    image VARCHAR(256) COMMENT '图片',
    dish_flavor VARCHAR(256) COMMENT '口味',
    amount DECIMAL(10,2) COMMENT '单价',
    number INT DEFAULT 1 COMMENT '数量',
    INDEX idx_order_id (order_id)
) COMMENT '订单明细表';
