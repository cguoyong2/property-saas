<template>
  <main class="auth-page">
    <section class="auth-panel">
      <h1>智慧物业 SaaS</h1>
      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" class="auth-panel__button" :loading="loading" @click="submit">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { login } from '@/api/auth'
import { useAuthStore } from '@/store/auth'

const form = reactive({
  username: import.meta.env.DEV ? 'tenant_a_admin' : '',
  password: import.meta.env.DEV ? 'Admin@123' : '',
})

const loading = ref(false)
const router = useRouter()
const auth = useAuthStore()

async function submit() {
  loading.value = true
  try {
    const response = await login(form)
    const data = response.data.data
    auth.setSession(data.token, data.permissions ?? [], data.userType, data.realName ?? data.username ?? '')
    await router.push(data.userType === 'PLATFORM' ? '/platform/tenants' : '/dashboard')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>
