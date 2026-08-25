package com.savory.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.savory.pojo.entity.AddressBook;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.dynamic.datasource.annotation.DS;

@DS("user")
@Mapper
public interface AddressBookMapper extends BaseMapper<AddressBook> {
}
