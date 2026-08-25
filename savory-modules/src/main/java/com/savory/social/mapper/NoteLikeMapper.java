package com.savory.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.savory.pojo.entity.NoteLike;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.dynamic.datasource.annotation.DS;

@DS("social")
@Mapper
public interface NoteLikeMapper extends BaseMapper<NoteLike> {
}
