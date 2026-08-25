package com.savory.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.savory.pojo.entity.MerchantInfo;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.dynamic.datasource.annotation.DS;

@DS("merchant")
@Mapper
public interface MerchantInfoMapper extends BaseMapper<MerchantInfo> {
}
