package com.savory.user.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.savory.pojo.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;

@DS("user")
@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {
}
