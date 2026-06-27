<template>
  <view class="page workorder-create-page">
    <view class="hero">
      <view class="hero-copy">
        <text class="eyebrow">物业服务</text>
        <text class="hero-title">报事报修</text>
        <text class="hero-desc">提交后物业管家会受理、派单并跟进处理进度</text>
      </view>
      <view class="hero-mark">
        <text class="mark-line"></text>
        <text class="mark-dot"></text>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">服务类型</text>
        <text class="section-note">选择最接近的问题类型</text>
      </view>
      <view class="type-grid">
        <button
          v-for="item in typeOptions"
          :key="item.value"
          class="type-card"
          :class="{ active: form.orderType === item.value }"
          @click="form.orderType = item.value"
        >
          <text class="type-icon">{{ item.icon }}</text>
          <text class="type-name">{{ item.label }}</text>
          <text class="type-desc">{{ item.desc }}</text>
        </button>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">问题信息</text>
        <text class="section-note">信息越完整，处理越快</text>
      </view>
      <view class="field">
        <text class="field-label">当前房屋</text>
        <view class="house-row">
          <text class="house-text">{{ member.currentHouseNo || '请先选择房屋' }}</text>
          <button class="link-button" @click="goHouseSwitch">切换</button>
        </view>
      </view>
      <view class="field">
        <text class="field-label">问题标题</text>
        <input v-model="form.title" class="input" placeholder="例如：厨房水龙头漏水" />
      </view>
      <view class="field">
        <text class="field-label">发生位置</text>
        <input v-model="form.location" class="input" placeholder="例如：3栋2单元1801厨房" />
      </view>
      <view class="field">
        <text class="field-label">问题描述</text>
        <textarea
          v-model="form.description"
          class="textarea"
          maxlength="300"
          placeholder="请描述问题现象、影响范围、方便上门时间等"
        />
        <text class="counter">{{ form.description.length }}/300</text>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">现场照片</text>
        <text class="section-note">最多上传 3 张，便于师傅判断</text>
      </view>
      <view class="photos">
        <view v-for="(photo, index) in photos" :key="photo.fileId" class="photo">
          <image :src="photo.path" mode="aspectFill" />
          <button class="remove" @click="removePhoto(index)">×</button>
        </view>
        <button v-if="photos.length < 3" class="add-photo" :loading="uploading" @click="choosePhoto">
          <text class="plus">+</text>
          <text class="add-label">添加照片</text>
        </button>
      </view>
    </view>

    <view class="notice">
      <text class="notice-title">服务说明</text>
      <text class="notice-text">紧急漏水、停电等问题建议同时拨打物业电话，系统工单用于留痕和进度跟踪。</text>
    </view>

    <view class="submit-bar">
      <view class="submit-copy">
        <text class="submit-title">提交工单</text>
        <text class="submit-sub">预计 10 分钟内受理</text>
      </view>
      <button class="primary" :loading="loading" @click="submit">立即提交</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { createWorkOrder, uploadAppFile } from '@/api/app'
import { useMemberStore } from '@/store/member'

const member = useMemberStore()
const loading = ref(false)
const uploading = ref(false)
const typeOptions = [
  { value: 'REPAIR', label: '维修', desc: '水电门窗等', icon: '修' },
  { value: 'HOUSEKEEPING', label: '保洁', desc: '楼道环境等', icon: '洁' },
  { value: 'RENOVATION', label: '装修', desc: '装修报备等', icon: '装' },
  { value: 'SUGGESTION', label: '建议', desc: '服务建议', icon: '议' },
]
const form = reactive({ orderType: 'REPAIR', title: '', description: '', location: '' })
const photos = ref<Array<{ fileId: number, path: string }>>([])

function goHouseSwitch() {
  uni.navigateTo({ url: '/pages/house/switch' })
}

function choosePhoto() {
  if (!member.currentProjectId) {
    uni.showToast({ title: '请先选择房屋', icon: 'none' })
    return
  }
  uni.chooseImage({
    count: 3 - photos.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    async success(result) {
      uploading.value = true
      try {
        for (const path of result.tempFilePaths) {
          const uploaded = await uploadAppFile(path, { projectId: member.currentProjectId as number, moduleCode: 'workorder' })
          photos.value.push({ fileId: Number(uploaded.fileId), path })
        }
      } catch (error) {
        uni.showToast({ title: error instanceof Error ? error.message : '上传失败', icon: 'none' })
      } finally {
        uploading.value = false
      }
    },
  })
}

function removePhoto(index: number) {
  photos.value.splice(index, 1)
}

async function submit() {
  if (member.currentBindRole !== 'OWNER' && !member.currentAllowWorkOrder) {
    uni.showToast({ title: '当前房屋未授权提交报修', icon: 'none' })
    return
  }
  if (!member.currentProjectId || !member.currentHouseId) {
    uni.showToast({ title: '请先选择房屋', icon: 'none' })
    return
  }
  if (!form.title.trim() || !form.description.trim()) {
    uni.showToast({ title: '请填写标题和描述', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await createWorkOrder({
      projectId: member.currentProjectId,
      memberId: member.memberId,
      houseId: member.currentHouseId,
      ...form,
      imageFileIds: photos.value.map((photo) => photo.fileId).join(','),
      priority: 'NORMAL',
    })
    uni.showToast({ title: '已提交' })
    uni.navigateTo({ url: '/pages/workorder/list' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '提交失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  width: 100%;
  max-width: 430px;
  margin: 0 auto;
  padding: 16px 14px 104px;
  background: #eef7f4;
  box-sizing: border-box;
  overflow-x: hidden;
}

.hero {
  position: relative;
  display: flex;
  min-height: 142px;
  margin-bottom: 14px;
  padding: 22px 20px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(135deg, #0f766e 0%, #0b8a72 48%, #155e75 100%);
  border-radius: 18px;
  box-sizing: border-box;
  box-shadow: 0 16px 28px rgba(15, 118, 110, .2);
}

.hero-copy {
  position: relative;
  z-index: 1;
  max-width: 280px;
}

.eyebrow {
  margin-bottom: 8px;
  color: #bff4e7;
  font-size: 13px;
}

.hero-title {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.15;
}

.hero-desc {
  margin-top: 10px;
  color: #e2fffa;
  font-size: 14px;
  line-height: 1.55;
}

.hero-mark {
  position: absolute;
  right: -18px;
  bottom: -28px;
  width: 132px;
  height: 132px;
  border: 1px solid rgba(255, 255, 255, .28);
  border-radius: 50%;
}

.mark-line {
  position: absolute;
  left: 26px;
  top: 42px;
  width: 70px;
  height: 10px;
  background: rgba(255, 255, 255, .2);
  border-radius: 999px;
}

.mark-dot {
  position: absolute;
  left: 42px;
  top: 66px;
  width: 42px;
  height: 42px;
  background: rgba(255, 255, 255, .18);
  border-radius: 50%;
}

.section,
.notice {
  width: 100%;
  margin-bottom: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5efec;
  border-radius: 14px;
  box-sizing: border-box;
  box-shadow: 0 8px 22px rgba(15, 23, 42, .04);
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-title {
  color: #102a43;
  font-size: 17px;
  font-weight: 800;
}

.section-note {
  min-width: 0;
  color: #7b8794;
  font-size: 12px;
  text-align: right;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.type-card {
  display: grid;
  grid-template-columns: 38px 1fr;
  grid-template-rows: auto auto;
  column-gap: 10px;
  min-height: 74px;
  min-width: 0;
  width: 100%;
  margin: 0;
  padding: 12px;
  text-align: left;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.type-card.active {
  background: #ecfdf5;
  border-color: #0f766e;
  box-shadow: inset 0 0 0 1px rgba(15, 118, 110, .16);
}

.type-icon {
  display: flex;
  grid-row: span 2;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  color: #0f766e;
  background: #d9f4ee;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 800;
}

.type-name {
  min-width: 0;
  color: #1f2937;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.25;
}

.type-desc {
  min-width: 0;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.3;
}

.field {
  margin-bottom: 14px;
}

.field:last-child {
  margin-bottom: 0;
}

.field-label {
  margin-bottom: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.house-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 46px;
  padding: 0 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.house-text {
  color: #1f2937;
  font-size: 14px;
}

.link-button {
  height: 30px;
  margin: 0;
  padding: 0 10px;
  color: #0f766e;
  background: #e6f6f2;
  border: 0;
  border-radius: 999px;
  font-size: 13px;
}

.input,
.textarea {
  width: 100%;
  color: #1f2937;
  background: #f8fafc;
  border: 1px solid #d9e2e7;
  border-radius: 10px;
  box-sizing: border-box;
  font-size: 15px;
  outline: none;
}

.input {
  height: 46px;
  padding: 0 12px;
}

.textarea {
  min-height: 130px;
  padding: 12px;
  line-height: 1.55;
}

.counter {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
  text-align: right;
}

.photos {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.photo {
  position: relative;
  width: 82px;
  height: 82px;
  overflow: hidden;
  background: #f8fafc;
  border-radius: 12px;
}

.photo image {
  width: 100%;
  height: 100%;
}

.remove {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 24px;
  height: 24px;
  padding: 0;
  color: #fff;
  background: rgba(15, 23, 42, .72);
  border: 0;
  border-radius: 50%;
  font-size: 18px;
  line-height: 24px;
}

.add-photo {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 82px;
  height: 82px;
  margin: 0;
  padding: 0;
  color: #0f766e;
  background: #f8fafc;
  border: 1px dashed #8ccfc4;
  border-radius: 12px;
}

.plus {
  font-size: 26px;
  line-height: 1;
}

.add-label {
  margin-top: 5px;
  font-size: 12px;
}

.notice {
  background: #f8fffd;
}

.notice-title {
  color: #0f766e;
  font-size: 14px;
  font-weight: 800;
}

.notice-text {
  margin-top: 8px;
  color: #52616b;
  font-size: 13px;
  line-height: 1.6;
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  z-index: 10;
  width: 100%;
  max-width: 430px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 16px 18px;
  background: rgba(255, 255, 255, .96);
  border-top: 1px solid #e5efec;
  box-sizing: border-box;
  box-shadow: 0 -10px 24px rgba(15, 23, 42, .08);
  transform: translateX(-50%);
}

.submit-copy {
  min-width: 0;
  flex: 1;
}

.submit-title {
  color: #102a43;
  font-size: 15px;
  font-weight: 800;
}

.submit-sub {
  margin-top: 4px;
  color: #7b8794;
  font-size: 12px;
}

.primary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 116px;
  flex: 0 0 auto;
  height: 46px;
  margin: 0;
  padding: 0 20px;
  color: #fff;
  background: #0f766e;
  border: 0;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 800;
  box-shadow: 0 10px 18px rgba(15, 118, 110, .24);
}

@media (max-width: 460px) {
  .section-note {
    display: none;
  }

  .type-grid {
    grid-template-columns: 1fr;
  }
}
</style>
