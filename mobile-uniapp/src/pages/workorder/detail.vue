<template>
  <view class="page">
    <view v-if="workOrder.workOrderId" class="card">
      <view class="row">
        <text class="title">{{ workOrder.title }}</text>
        <text class="status">{{ workOrder.status }}</text>
      </view>
      <text class="meta">{{ workOrder.orderNo }} ｜ {{ workOrder.orderType }} ｜ {{ workOrder.priority }}</text>
      <text class="desc">{{ workOrder.description }}</text>
      <view class="line"><text>位置</text><text>{{ workOrder.location || '-' }}</text></view>
      <view class="line"><text>提交时间</text><text>{{ workOrder.createdAt }}</text></view>
      <view class="line"><text>SLA 截止</text><text>{{ workOrder.slaDeadline || '-' }}</text></view>
    </view>

    <view v-if="events.length" class="card">
      <text class="section-title">处理进度</text>
      <view v-for="event in events" :key="String(event.eventId)" class="event">
        <text>{{ event.action }}：{{ event.fromStatus || '开始' }} -> {{ event.toStatus }}</text>
        <text>{{ event.createdAt }}</text>
      </view>
    </view>

    <view v-if="comments.length" class="card">
      <text class="section-title">评价记录</text>
      <view v-for="comment in comments" :key="String(comment.commentId)" class="event">
        <text>{{ comment.score }} 分：{{ comment.content || '-' }}</text>
        <text>{{ comment.createdAt }}</text>
      </view>
    </view>

    <view v-if="canEvaluate" class="card">
      <text class="section-title">服务评价</text>
      <picker :range="scores" @change="score = Number(scores[Number($event.detail.value)])">
        <view class="input">评分：{{ score }} 分</view>
      </picker>
      <textarea v-model="content" class="textarea" placeholder="评价内容" />
      <button class="primary" :loading="saving" @click="submitEvaluation">提交评价</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { evaluateWorkOrder, fetchWorkOrderDetail } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const workOrderId = ref('')
const workOrder = reactive<Record<string, unknown>>({})
const events = ref<Record<string, unknown>[]>([])
const comments = ref<Record<string, unknown>[]>([])
const scores = ['5', '4', '3', '2', '1']
const score = ref(5)
const content = ref('')
const saving = ref(false)
const canEvaluate = computed(() => ['COMPLETED', 'WAIT_CONFIRM'].includes(String(workOrder.status)) && comments.value.length === 0)

onLoad(async (query) => {
  workOrderId.value = String(query?.workOrderId ?? '')
  await load()
})

async function load() {
  if (!workOrderId.value) return
  try {
    const detail = await fetchWorkOrderDetail(workOrderId.value)
    Object.assign(workOrder, detail.workOrder ?? {})
    events.value = Array.isArray(detail.events) ? detail.events as Record<string, unknown>[] : []
    comments.value = Array.isArray(detail.comments) ? detail.comments as Record<string, unknown>[] : []
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' })
  }
}

async function submitEvaluation() {
  saving.value = true
  try {
    await evaluateWorkOrder(workOrderId.value, {
      memberId: member.memberId,
      score: score.value,
      content: content.value,
    })
    uni.showToast({ title: '已评价' })
    await load()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '评价失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { margin-bottom: 18rpx; padding: 28rpx; background: #fff; border-radius: 14rpx; }
.row, .line { display: flex; justify-content: space-between; align-items: center; }
.title { max-width: 480rpx; font-size: 30rpx; font-weight: 800; }
.status { color: #0f766e; font-size: 24rpx; font-weight: 700; }
.meta, .desc { display: block; margin-top: 14rpx; color: #64748b; font-size: 24rpx; line-height: 1.6; }
.line { padding-top: 18rpx; color: #475569; font-size: 24rpx; }
.section-title { display: block; margin-bottom: 18rpx; font-size: 28rpx; font-weight: 800; }
.event { display: flex; flex-direction: column; gap: 8rpx; padding: 14rpx 0; border-top: 1rpx solid #e2e8f0; color: #64748b; font-size: 24rpx; }
.input { height: 84rpx; margin-bottom: 18rpx; padding: 0 22rpx; background: #f8fafc; border-radius: 10rpx; line-height: 84rpx; }
.textarea { width: 100%; min-height: 160rpx; margin-bottom: 18rpx; padding: 22rpx; background: #f8fafc; border-radius: 10rpx; box-sizing: border-box; }
.primary { background: #0f766e; color: #fff; }
</style>
