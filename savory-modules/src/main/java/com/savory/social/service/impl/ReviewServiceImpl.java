package com.savory.social.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.savory.common.context.BaseContext;
import com.savory.common.result.PageResult;
import com.savory.pojo.entity.Review;
import com.savory.social.mapper.ReviewMapper;
import com.savory.social.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评价服务实现类
 */
@DS("social")
@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Override
    public void publish(Review review) {
        //1、设置作者ID和审核状态
        review.setUserId(BaseContext.getCurrentId());
        review.setAuditStatus(0); //待审核

        //2、保存评价
        reviewMapper.insert(review);
        log.info("评价发布成功，reviewId: {}, userId: {}", review.getId(), review.getUserId());

        //3、异步触发AI内容审核
        //TODO: 发送RocketMQ消息 → AI AuditAgent → 审核后回调更新audit_status
    }

    @Override
    public List<Review> listByDishId(Long dishId) {
        //1、查询该菜品的所有已审核通过的评价
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getDishId, dishId)
               .eq(Review::getAuditStatus, 1)  // 仅已审核通过
               .orderByDesc(Review::getCreateTime);

        //2、返回评价列表
        return reviewMapper.selectList(wrapper);
    }

    @Override
    public PageResult pageQuery(int page, int pageSize, Integer auditStatus) {
        //1、构建分页条件
        Page<Review> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(auditStatus != null, Review::getAuditStatus, auditStatus)
               .orderByDesc(Review::getCreateTime);

        //2、执行分页查询
        Page<Review> result = reviewMapper.selectPage(p, wrapper);
        return new PageResult(result.getTotal(), result.getRecords());
    }
}
