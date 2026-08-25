package com.savory.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.savory.pojo.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.dynamic.datasource.annotation.DS;

@DS("social")
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
