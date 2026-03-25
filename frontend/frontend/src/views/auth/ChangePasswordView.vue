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
  <div class="auth-page">
    <section class="auth-shell auth-shell--password">
      <div class="auth-panel auth-panel--hero">
        <span class="page-head__eyebrow">首次登录</span>
        <h1 class="page-head__title">先完成安全设置，再进入你的课程工作台</h1>
        <p class="page-head__description">
          完成首次改密后，系统会自动跳转到当前角色的工作台。
          建议设置一个容易记住但更安全的密码，后续提交、评分和查看结果都会更顺畅。
        </p>

        <div class="page-head__stats">
          <span>首次登录必做</span>
          <span>修改后自动跳转</span>
          <span>适用于全部角色</span>
        </div>
      </div>

      <div class="auth-panel auth-panel--form">
        <div class="panel-header">
          <div>
            <span class="section-eyebrow">安全设置</span>
            <h3 style="margin-top: 14px;">修改登录密码</h3>
          </div>
        </div>

        <p class="section-subtitle" style="margin-top: 12px;">
          修改完成后即可继续进入课程项目评分平台。
        </p>

        <el-form :model="form" label-position="top" style="margin-top: 20px;">
          <el-form-item label="新密码">
            <el-input v-model="form.newPassword" type="password" show-password @keyup.enter="handleSubmit" />
          </el-form-item>
          <el-button type="primary" style="width: 100%;" @click="handleSubmit">确认修改</el-button>
        </el-form>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(96, 159, 255, 0.16) 0%, rgba(96, 159, 255, 0) 24%),
    linear-gradient(180deg, #f5f8fe 0%, #eef3fb 100%);
}

.auth-shell {
  width: min(1280px, 100%);
  margin: 0 auto;
  min-height: calc(100vh - 56px);
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) 420px;
  gap: 24px;
}

.auth-shell--password {
  grid-template-columns: minmax(0, 1fr) 400px;
  align-items: center;
}

.auth-panel {
  border-radius: var(--radius-xl);
  border: 1px solid var(--surface-border);
  background: var(--surface);
  box-shadow: var(--shadow-card);
}

.auth-panel--hero {
  padding: 32px;
}

.auth-panel--form {
  padding: 30px;
  align-self: center;
}

@media (max-width: 1080px) {
  .auth-shell,
  .auth-shell--password {
    grid-template-columns: 1fr;
  }
}
</style>
