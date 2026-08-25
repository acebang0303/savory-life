package com.savory.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 订单明细表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_detail")
public class OrderDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    //订单ID
    private Long orderId;
    //菜品/套餐名称
    private String name;
    //图片
    private String image;
    //口味
    private String dishFlavor;
    //单价
    private BigDecimal amount;
    //数量
    private Integer number;
}
