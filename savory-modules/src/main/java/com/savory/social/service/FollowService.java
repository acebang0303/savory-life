package com.savory.social.service;

/**
 * 关注服务接口
 */
public interface FollowService {

    /**
     * 关注/取关用户
     * @return true=已关注, false=已取关
     */
    boolean follow(Long followeeId);

    /**
     * 判断是否已关注
     */
    boolean isFollowing(Long followerId, Long followeeId);

    /**
     * 查询互相关注（SINTER）
     */
    long countMutualFollow(Long userId, Long targetUserId);
}
