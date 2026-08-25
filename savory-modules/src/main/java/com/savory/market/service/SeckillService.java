package com.savory.market.service;

import com.savory.common.result.PageResult;
import com.savory.market.dto.SeckillBuyDTO;
import com.savory.pojo.entity.SeckillActivity;

import java.util.List;

/**
 * 秒杀服务接口
 * 核心链路：Redis Lua原子操作 → RocketMQ异步落库
 */
public interface SeckillService {

    /**
     * 创建秒杀活动
     */
    void createActivity(SeckillActivity activity);

    /**
     * 分页查询秒杀活动
     */
    PageResult pageActivity(int page, int pageSize);

    /**
     * 查询进行中的秒杀活动
     */
    List<SeckillActivity> listRunning();

    /**
     * 查询活动详情
     */
    SeckillActivity getActivityById(Long id);

    /**
     * 秒杀抢购
     * @return 订单ID
     */
    Long seckillBuy(SeckillBuyDTO dto);
}
