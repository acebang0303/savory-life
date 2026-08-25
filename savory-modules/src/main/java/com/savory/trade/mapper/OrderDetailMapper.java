package com.savory.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.savory.pojo.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.dynamic.datasource.annotation.DS;

@DS("trade")
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {
}
