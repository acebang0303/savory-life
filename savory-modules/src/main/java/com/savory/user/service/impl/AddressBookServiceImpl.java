package com.savory.user.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.savory.common.context.BaseContext;
import com.savory.user.mapper.AddressBookMapper;
import com.savory.user.service.AddressBookService;
import com.savory.pojo.entity.AddressBook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@DS("user")
@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Override
    public void add(AddressBook addressBook) {
        //1、设置用户ID
        addressBook.setUserId(BaseContext.getCurrentId());

        //2、如果是第一个地址，自动设为默认
        Long count = addressBookMapper.selectCount(
                new LambdaQueryWrapper<AddressBook>().eq(AddressBook::getUserId, addressBook.getUserId()));
        if (count == 0) {
            addressBook.setIsDefault(1);
        }

        //3、新增地址
        addressBookMapper.insert(addressBook);
        log.info("新增收货地址成功，userId: {}", addressBook.getUserId());
    }

    @Override
    public List<AddressBook> list(Long userId) {
        LambdaQueryWrapper<AddressBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AddressBook::getUserId, userId)
               .orderByDesc(AddressBook::getIsDefault)
               .orderByDesc(AddressBook::getUpdateTime);
        return addressBookMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void setDefault(Long id, Long userId) {
        //1、清除旧的默认地址
        LambdaUpdateWrapper<AddressBook> clearWrapper = new LambdaUpdateWrapper<>();
        clearWrapper.eq(AddressBook::getUserId, userId)
                     .set(AddressBook::getIsDefault, 0);
        addressBookMapper.update(null, clearWrapper);

        //2、设置新的默认地址
        LambdaUpdateWrapper<AddressBook> setWrapper = new LambdaUpdateWrapper<>();
        setWrapper.eq(AddressBook::getId, id)
                  .set(AddressBook::getIsDefault, 1);
        addressBookMapper.update(null, setWrapper);

        log.info("设置默认地址成功，addressId: {}, userId: {}", id, userId);
    }
}
