-- 订单明细补充菜品/套餐标识，支持「再来一单」还原购物车
ALTER TABLE order_detail ADD COLUMN dish_id BIGINT NULL COMMENT '菜品ID（套餐明细为空）' AFTER order_id;
ALTER TABLE order_detail ADD COLUMN setmeal_id BIGINT NULL COMMENT '套餐ID（菜品明细为空）' AFTER dish_id;
