<template>
  <view class="note-detail">
    <view class="header">
      <image class="avatar" :src="note.avatar || defaultAvatar" mode="aspectFill" />
      <view class="author-info">
        <text class="name">{{ note.nickname || '用户' }}</text>
        <text class="time">{{ formatTime(note.createTime) }}</text>
      </view>
      <button class="follow-btn" @click="follow">关注</button>
    </view>

    <view class="content">
      <text class="title">{{ note.title }}</text>
      <text class="text">{{ note.content }}</text>
    </view>

    <view class="images" v-if="note.images">
      <image v-for="(img, i) in images" :key="i" :src="img" mode="widthFix" class="img" />
    </view>

    <view class="actions">
      <view class="action" @click="like">
        <text>{{ note.isLiked ? '❤️' : '🤍' }} {{ note.likeCount || 0 }}</text>
      </view>
      <view class="action" @click="collect">
        <text>{{ note.isCollected ? '⭐' : '☆' }} {{ note.collectCount || 0 }}</text>
      </view>
      <view class="action">
        <text>💬 {{ note.commentCount || 0 }}</text>
      </view>
    </view>

    <view class="comment-section">
      <text class="section-title">评论 ({{ comments.length }})</text>
      <view class="comment" v-for="c in comments" :key="c.id">
        <text class="comment-user">{{ c.nickname || '用户' }}：</text>
        <text class="comment-text">{{ c.content }}</text>
        <text class="comment-time">{{ formatTime(c.createTime) }}</text>
      </view>
    </view>

    <view class="comment-input">
      <input v-model="commentText" placeholder="写评论..." confirm-type="send" @confirm="sendComment" />
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getNoteFeed, likeNote, collectNote, followUser } from '@/api/index.js'
import { formatTime } from '@/utils/index.js'

const defaultAvatar = '/static/icons/profile.png'
const note = ref({})
const comments = ref([])
const commentText = ref('')

const images = computed(() => {
  try { return typeof note.value.images === 'string' ? JSON.parse(note.value.images) : (note.value.images || []) } catch { return [] }
})

onMounted(async () => {
  const pages = getCurrentPages()
  const id = pages[pages.length - 1].$page.options?.id
  if (id) {
    try {
      const res = await getNoteFeed(1, 10)
      const found = (res?.records || []).find(n => n.id === Number(id))
      if (found) note.value = found
    } catch (e) { console.log(e) }
  }
})

const like = async () => {
  const res = await likeNote(note.value.id)
  note.value.isLiked = res?.liked
  note.value.likeCount += res?.liked ? 1 : -1
}

const collect = async () => {
  const res = await collectNote(note.value.id)
  note.value.isCollected = res?.collected
  note.value.collectCount += res?.collected ? 1 : -1
}

const follow = async () => { await followUser(note.value.userId) }

const sendComment = () => {
  if (commentText.value.trim()) {
    comments.value.push({
      id: Date.now(), content: commentText.value,
      nickname: '我', createTime: new Date().toISOString()
    })
    commentText.value = ''
  }
}
</script>

<style lang="scss" scoped>
.note-detail { min-height: 100vh; background: #fff; padding: 24rpx; }
.header { display: flex; align-items: center; margin-bottom: 24rpx; }
.avatar { width: 80rpx; height: 80rpx; border-radius: 50%; margin-right: 16rpx; background: #f0f0f0; }
.author-info { flex: 1; }
.name { font-size: 30rpx; font-weight: 600; display: block; }
.time { font-size: 22rpx; color: #999; }
.follow-btn { height: 52rpx; line-height: 52rpx; padding: 0 20rpx; background: $primary-color; color: #fff; border-radius: 26rpx; font-size: 22rpx; border: none; }
.content { margin-bottom: 24rpx; }
.title { font-size: 36rpx; font-weight: bold; display: block; margin-bottom: 16rpx; }
.text { font-size: 30rpx; line-height: 1.8; }
.images { margin-bottom: 24rpx; }
.img { width: 100%; border-radius: 12rpx; margin-bottom: 8rpx; }
.actions { display: flex; gap: 32rpx; padding: 16rpx 0; border-top: 1rpx solid #f0f0f0; border-bottom: 1rpx solid #f0f0f0; margin-bottom: 24rpx; font-size: 26rpx; }
.section-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 16rpx; }
.comment { padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f5; }
.comment-user { color: #1890FF; font-size: 26rpx; }
.comment-text { font-size: 28rpx; }
.comment-time { font-size: 22rpx; color: #ccc; display: block; margin-top: 4rpx; }
.comment-input { position: fixed; bottom: 0; left: 0; right: 0; padding: 16rpx 24rpx; background: #fff; border-top: 1rpx solid #f0f0f0; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); }
.comment-input input { height: 72rpx; background: #f5f5f5; border-radius: 36rpx; padding: 0 24rpx; font-size: 26rpx; }
</style>
