<template>
  <el-card>
    <template #header><span>🤖 AI 经营助手</span></template>
    <div class="chat-container">
      <div class="chat-messages" ref="chatRef">
        <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role === 'user' ? 'msg-user' : 'msg-ai']">
          <div class="msg-content"><span v-if="msg.role==='ai'">🤖 </span>{{ msg.content }}</div>
        </div>
        <div v-if="loading" class="msg msg-ai"><div class="msg-content">🤖 正在思考中...</div></div>
      </div>
      <div class="chat-input">
        <el-input v-model="input" placeholder="输入问题，如：上周什么菜卖得最好？帮我分析一下差评原因？" size="large" @keyup.enter="send">
          <template #append><el-button :loading="loading" @click="send">发送</el-button></template>
        </el-input>
        <div class="quick-questions">
          <el-tag v-for="q in quickQuestions" :key="q" style="cursor:pointer;margin:4px" @click="input=q;send()">{{ q }}</el-tag>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'

const chatRef = ref<HTMLDivElement>()
const input = ref('')
const loading = ref(false)
const messages = reactive<{role:string,content:string}[]>([
  { role: 'ai', content: '你好！我是你的AI经营助手。你可以问我任何关于店铺经营的问题，比如销售数据、菜品分析、运营建议。' }
])
const quickQuestions = ['上周什么菜卖得最好？', '分析一下最近的差评原因', '帮我生成本周促销文案', '这个月的营收趋势怎么样？']

async function send() {
  if (!input.value.trim() || loading.value) return
  const question = input.value.trim()
  messages.push({ role: 'user', content: question })
  input.value = ''
  loading.value = true
  await nextTick(); scrollToBottom()

  const empId = localStorage.getItem('empId')
  if (!empId) {
    messages.push({ role: 'ai', content: '未获取到商家身份，请重新登录。' })
    loading.value = false
    return
  }

  const aiMsg = reactive({ role: 'ai', content: '' })
  messages.push(aiMsg)

  const es = new EventSource(`/ai/merchant/stream?question=${encodeURIComponent(question)}&empId=${empId}`)
  let finished = false
  es.addEventListener('message', (e) => {
    try {
      const data = JSON.parse(e.data)
      if (data.content) {
        aiMsg.content += data.content
        nextTick(() => scrollToBottom())
      }
    } catch { /* 忽略无法解析的帧 */ }
  })
  es.addEventListener('done', () => {
    finished = true
    es.close()
    loading.value = false
    nextTick(() => scrollToBottom())
  })
  es.onerror = () => {
    es.close()
    if (!finished && !aiMsg.content) aiMsg.content = '连接中断，请稍后重试。'
    loading.value = false
    nextTick(() => scrollToBottom())
  }
}

function scrollToBottom() {
  if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight
}
</script>

<style scoped>
.chat-container { display: flex; flex-direction: column; height: 500px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; background: #f5f7fa; border-radius: 8px; margin-bottom: 12px; }
.msg { margin-bottom: 12px; display: flex; }
.msg-user { justify-content: flex-end; }
.msg-content { max-width: 75%; padding: 10px 16px; border-radius: 12px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.msg-user .msg-content { background: #409eff; color: #fff; border-bottom-right-radius: 4px; }
.msg-ai .msg-content { background: #fff; border: 1px solid #e4e7ed; border-bottom-left-radius: 4px; }
.quick-questions { margin-top: 8px; }
</style>
