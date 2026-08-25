package com.savory.user.controller;

import com.savory.common.result.Result;
import com.savory.pojo.entity.AddressBook;
import com.savory.user.service.AddressBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端收货地址接口
 */
@RestController
@RequestMapping("/user/address")
@Slf4j
@Tag(name = "收货地址相关接口")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增收货地址
     */
    @PostMapping
    @Operation(summary = "新增收货地址")
    public Result<String> add(@RequestBody AddressBook addressBook) {
        log.info("新增收货地址: {}", addressBook);
        addressBookService.add(addressBook);
        return Result.success();
    }

    /**
     * 查询收货地址列表
     */
    @GetMapping
    @Operation(summary = "查询收货地址列表")
    public Result<List<AddressBook>> list() {
        log.info("查询收货地址列表");
        List<AddressBook> list = addressBookService.list(
                com.savory.common.context.BaseContext.getCurrentId());
        return Result.success(list);
    }

    /**
     * 设为默认地址
     */
    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认地址")
    public Result<String> setDefault(@PathVariable Long id) {
        log.info("设为默认地址，id: {}", id);
        addressBookService.setDefault(id, com.savory.common.context.BaseContext.getCurrentId());
        return Result.success();
    }
}
