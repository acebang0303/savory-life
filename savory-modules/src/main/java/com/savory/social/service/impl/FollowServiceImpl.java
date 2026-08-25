package com.savory.social.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.savory.common.context.BaseContext;
import com.savory.pojo.entity.Follow;
import com.savory.social.mapper.FollowMapper;
import com.savory.social.service.FollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 关注服务实现类
 * Redis配合MySQL: 关注关系双写
 */
@Service
@DS("social")
@Slf4j
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public boolean follow(Long followeeId) {
        Long followerId = BaseContext.getCurrentId();

        //1、检查是否已关注
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, followerId)
               .eq(Follow::getFolloweeId, followeeId);
        Follow exist = followMapper.selectOne(wrapper);

        if (exist != null) {
            //2、已关注 → 取关
            followMapper.deleteById(exist.getId());
            // 从Redis关注集合中移除
            redisTemplate.opsForSet().remove("follow:" + followerId, followeeId.toString());
            redisTemplate.opsForSet().remove("fans:" + followeeId, followerId.toString());
            log.info("取关成功，follower: {}, followee: {}", followerId, followeeId);
            return false;
        } else {
            //3、未关注 → 关注
            Follow follow = Follow.builder()
                    .followerId(followerId)
                    .followeeId(followeeId)
                    .build();
            followMapper.insert(follow);
            // 写入Redis关注集合
            redisTemplate.opsForSet().add("follow:" + followerId, followeeId.toString());
            redisTemplate.opsForSet().add("fans:" + followeeId, followerId.toString());
            log.info("关注成功，follower: {}, followee: {}", followerId, followeeId);
            return true;
        }
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        String followKey = "follow:" + followerId;
        Boolean isMember = redisTemplate.opsForSet().isMember(followKey, followeeId.toString());
        return Boolean.TRUE.equals(isMember);
    }

    @Override
    public long countMutualFollow(Long userId, Long targetUserId) {
        String userFollowKey = "follow:" + userId;
        String targetFollowKey = "follow:" + targetUserId;

        //SINTER 取交集 = 互相关注的好友数
        Long count = redisTemplate.opsForSet()
                .intersectAndStore(userFollowKey, targetFollowKey, "temp:mutual:" + userId);
        return count != null ? count : 0;
    }
}
