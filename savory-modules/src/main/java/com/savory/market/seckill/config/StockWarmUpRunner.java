package com.savory.market.seckill.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.savory.market.mapper.SeckillActivityMapper;
import com.savory.market.seckill.cache.RedisExecutor;
import com.savory.pojo.entity.SeckillActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀库存预热：应用启动时把进行中的活动库存预热到 Redis（DB 为准，setIfAbsent 不覆盖）。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockWarmUpRunner implements ApplicationRunner {

    private final SeckillActivityMapper activityMapper;
    private final RedisExecutor redisExecutor;

    @Override
    public void run(ApplicationArguments args) {
        List<SeckillActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getStatus, 1)
                        .gt(SeckillActivity::getEndTime, LocalDateTime.now()));
        for (SeckillActivity a : activities) {
            String stockKey = "seckill:stock:" + a.getId() + ":" + a.getDishId();
            redisExecutor.setIfAbsent(stockKey, String.valueOf(a.getStock()),
                    Duration.between(LocalDateTime.now(), a.getEndTime()));
            log.info("库存预热(DB为准,setIfAbsent不覆盖): activityId={}, stock={}", a.getId(), a.getStock());
        }
    }
}
