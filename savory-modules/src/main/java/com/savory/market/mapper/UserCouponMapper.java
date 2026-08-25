package com.savory.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.savory.pojo.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.dynamic.datasource.annotation.DS;

@DS("market")
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
}
