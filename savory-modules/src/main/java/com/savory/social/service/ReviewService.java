package com.savory.social.service;

import com.savory.common.result.PageResult;
import com.savory.pojo.entity.Review;

import java.util.List;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /**
     * 发表评价
     * 提交后触发AI内容审核(AuditAgent)
     */
    void publish(Review review);

    /**
     * 查询菜品的评价列表(仅已审核通过)
     */
    List<Review> listByDishId(Long dishId);

    /**
     * 分页查询评价列表
     */
    PageResult pageQuery(int page, int pageSize, Integer auditStatus);
}
