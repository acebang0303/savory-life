package com.savory.merchant.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.savory.common.result.PageResult;
import com.savory.merchant.dto.DishDTO;
import com.savory.merchant.dto.DishPageQueryDTO;
import com.savory.merchant.mapper.DishFlavorMapper;
import com.savory.merchant.mapper.DishMapper;
import com.savory.merchant.mq.DishEmbeddingProducer;
import com.savory.merchant.service.DishService;
import com.savory.pojo.entity.Dish;
import com.savory.pojo.entity.DishFlavor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@DS("merchant")
@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private DishEmbeddingProducer dishEmbeddingProducer;

    @Override
    @Transactional
    @CacheEvict(value = "dish", allEntries = true)
    public void save(DishDTO dishDTO) {
        //1、构建菜品实体并保存
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        log.info("新增菜品成功，dishId: {}", dish.getId());

        //2、保存口味列表
        if (dishDTO.getFlavors() != null) {
            List<DishFlavor> flavors = dishDTO.getFlavors().stream().map(f -> {
                DishFlavor flavor = new DishFlavor();
                flavor.setDishId(dish.getId());
                flavor.setName(f.getName());
                flavor.setValue(f.getValue());
                return flavor;
            }).collect(Collectors.toList());
            flavors.forEach(dishFlavorMapper::insert);
        }

        //3、发消息同步菜品向量
        dishEmbeddingProducer.send(dish.getId());
    }

    @Override
    @Cacheable(value = "dish", key = "#dishPageQueryDTO.page + '_' + #dishPageQueryDTO.pageSize")
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        //1、构建分页条件
        Page<Dish> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dishPageQueryDTO.getCategoryId() != null, Dish::getCategoryId, dishPageQueryDTO.getCategoryId())
               .eq(dishPageQueryDTO.getStatus() != null, Dish::getStatus, dishPageQueryDTO.getStatus())
               .like(dishPageQueryDTO.getName() != null, Dish::getName, dishPageQueryDTO.getName())
               .orderByDesc(Dish::getCreateTime);

        //2、执行分页查询
        Page<Dish> result = dishMapper.selectPage(page, wrapper);
        return new PageResult(result.getTotal(), result.getRecords());
    }

    @Override
    @Cacheable(value = "dish", key = "#categoryId")
    public List<Dish> list(Long categoryId) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getCategoryId, categoryId)
               .eq(Dish::getStatus, 1)
               .orderByAsc(Dish::getCreateTime);
        return dishMapper.selectList(wrapper);
    }

    @Override
    @CacheEvict(value = "dish", allEntries = true)
    public void updateStatus(Long id, Integer status) {
        Dish dish = Dish.builder().id(id).status(status).build();
        dishMapper.updateById(dish);
        log.info("更新菜品状态，dishId: {}, status: {}", id, status);

        //发消息同步菜品向量（消费端按 status 决定重建或删除）
        dishEmbeddingProducer.send(id);
    }
}
