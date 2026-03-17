<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { roleHome } from '../../utils/menu'

const authStore = useAuthStore()
const router = useRouter()
const form = reactive({ newPassword: '' })

async function handleSubmit() {
  await authStore.updatePassword(form.newPassword)
  ElMessage.success('密码已更新')
  router.push(roleHome[authStore.user?.role])
}
</script>

<template>
  <div class="change-password-page">
    <div class="change-password-page__glow"></div>
    <div class="change-password-shell">
      <section class="change-password-hero">
        <span class="page-hero__eyebrow">first login</span>
        <h1>Set a secure password before entering your learning workspace</h1>
        <p>
          完成首次改密后，系统会自动跳转到你的角色工作台。建议设置一个容易记住但更安全的密码，
          这样后续提交、评分和查看结果时会更顺畅。
        </p>

        <div class="page-hero__meta">
          <span>首次登录必做</span>
          <span>完成后自动跳转</span>
          <span>适用于所有角色</span>
        </div>
      </section>

      <el-card class="change-password-card">
        <template #header>
          <div class="stack" style="gap: 6px;">
            <span class="section-eyebrow">security</span>
            <strong>首次登录修改密码</strong>
          </div>
        </template>
        <el-form :model="form" label-position="top">
          <el-form-item label="新密码">
            <el-input v-model="form.newPassword" type="password" show-password />
          </el-form-item>
          <el-button type="primary" @click="handleSubmit">确认修改</el-button>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.change-password-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(255, 191, 112, 0.18) 0%, rgba(255, 191, 112, 0) 22%),
    linear-gradient(180deg, #fffaf3 0%, #fff7ec 100%);
}

.change-password-page__glow {
  position: absolute;
  width: 360px;
  height: 360px;
  top: -120px;
  right: -100px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 201, 127, 0.24) 0%, rgba(255, 201, 127, 0) 70%);
}

.change-password-shell {
  width: min(1080px, calc(100vw - 56px));
  margin: 0 auto;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 28px;
  align-items: center;
}

.change-password-hero {
  padding: 36px;
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at top right, rgba(255, 213, 154, 0.28) 0%, rgba(255, 213, 154, 0) 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.84) 0%, rgba(255, 247, 233, 0.9) 100%);
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-card);
}

.change-password-hero h1 {
  margin: 18px 0 0;
  max-width: 560px;
  font-size: 48px;
  line-height: 1.06;
  letter-spacing: -0.04em;
}

.change-password-hero p {
  margin: 16px 0 0;
  max-width: 560px;
  color: var(--text-body);
  line-height: 1.8;
}

.change-password-card {
  border-radius: var(--radius-xl);
}

@media (max-width: 980px) {
  .change-password-shell {
    grid-template-columns: 1fr;
    padding: 32px 0;
  }
}
</style>
