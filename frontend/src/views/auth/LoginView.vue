<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { roleHome } from '../../utils/menu'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: 's001',
  password: 's001',
})

const features = [
  '支持学生提交作品与仓库链接',
  '匿名互评与 Rubric 结构化打分',
  '教师复核与结果看板联动',
]

const registerTabs = [
  'For new users',
  'For experienced users',
  'Subscribe',
]

const demoAccounts = [
  {
    role: '管理员',
    account: 'admin/admin',
    description: '维护课程、作业与演示数据',
    accent: 'admin',
  },
  {
    role: '教师',
    account: 't001/t001',
    description: '查看课程、评分和统计看板',
    accent: 'teacher',
  },
  {
    role: '学生',
    account: 's001/s001',
    description: '首次登录后会进入改密流程',
    accent: 'student',
  },
]

const flowSteps = [
  {
    title: '提交作品',
    description: '学生上传项目名称、仓库地址和成员信息，形成统一的展示入口。',
  },
  {
    title: '组织互评',
    description: '系统按照规则分发匿名评审任务，并记录每一维度的评分依据。',
  },
  {
    title: '生成结果',
    description: '教师复核后发布结果，成绩、评语和排行榜自动进入看板。',
  },
]

async function handleLogin() {
  const user = await authStore.loginByPassword(form)
  ElMessage.success(`欢迎回来，${user.displayName}`)
  router.push(user.firstLoginResetRequired ? '/change-password' : roleHome[user.role])
}
</script>

<template>
  <div class="login-page">
    <div class="login-page__glow login-page__glow--left"></div>
    <div class="login-page__glow login-page__glow--right"></div>

    <header class="topbar">
      <div class="brand-mark">
        <span class="brand-mark__icon">C</span>
        <div>
          <strong>Course Demo</strong>
          <small>education review platform</small>
        </div>
      </div>

      <nav class="topbar__nav">
        <a href="#intro">Home</a>
        <a href="#features">Features</a>
        <a href="#accounts">Accounts</a>
        <a href="#community">Community</a>
      </nav>

      <button class="topbar__cta" type="button" @click="handleLogin">Subscribe</button>
    </header>

    <main class="landing-shell">
      <section class="hero-section" id="intro">
        <div class="hero-copy">
          <span class="hero-copy__badge">education landing page</span>
          <h1>Start learning with a cleaner course review workflow</h1>
          <p>
            为课程项目提供统一的提交、互评、教师评分与结果发布入口，
            让学习流程更清晰，反馈更及时，协作更顺畅。
          </p>

          <div class="hero-copy__actions">
            <button class="primary-cta" type="button" @click="handleLogin">Start learning</button>
            <span class="hero-copy__note">Start learning with just one click</span>
          </div>

          <ul class="hero-copy__list">
            <li v-for="item in features" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="login-card">
          <div class="login-card__illustration">
            <div class="login-card__bubble login-card__bubble--large"></div>
            <div class="login-card__bubble login-card__bubble--small"></div>
            <div class="login-card__spark login-card__spark--one"></div>
            <div class="login-card__spark login-card__spark--two"></div>
            <div class="login-card__avatar">
            <span>学</span>
          </div>
          <div class="login-card__card">
              <strong>Course review board</strong>
              <span>提交作品后自动进入互评、教师评分与结果反馈流程</span>
          </div>
        </div>

        <div class="login-card__body">
          <span class="login-card__eyebrow">sign in</span>
          <h3>进入演示环境</h3>
          <p>登录后根据角色进入对应工作台，快速体验课程评审完整流程。</p>

            <el-form :model="form" label-position="top">
              <el-form-item label="用户名">
                <el-input v-model="form.username" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="form.password" type="password" show-password @keyup.enter="handleLogin" />
              </el-form-item>
              <el-button type="primary" class="login-submit" @click="handleLogin">进入 demo</el-button>
            </el-form>
          </div>
        </div>
      </section>

      <section class="content-section content-section--split" id="features">
        <div class="media-card media-card--photo">
          <div class="media-card__portrait media-card__portrait--teacher"></div>
          <div class="media-card__floating-note">
            <strong>Welcome to the online center</strong>
            <p>课堂作业、过程评价和结果反馈统一在一个入口里查看。</p>
          </div>
        </div>

        <div class="section-copy">
          <span class="section-copy__eyebrow">welcome</span>
          <h2>Welcome to the online centers</h2>
          <p>
            在一个入口中查看课程任务、提交记录、过程评价和结果反馈，
            让学生、教师和管理员都能更快找到自己关心的信息。
          </p>
          <ul class="feature-list">
            <li>统一展示课程、作业、评审和统计结果</li>
            <li>适合学生、教师、管理员三个角色切换体验</li>
            <li>首屏信息更完整，也更利于演示讲解</li>
          </ul>
        </div>
      </section>

      <section class="content-section content-section--reverse" id="accounts">
        <div class="section-copy">
          <span class="section-copy__eyebrow">demo accounts</span>
          <h2>Start learning by selecting the role you want to preview</h2>
          <p>
            不同角色拥有不同的任务视角。选择学生、教师或管理员，
            即可查看对应的课程操作、评审流程和结果看板。
          </p>

          <div class="register-strip">
            <button
              v-for="item in registerTabs"
              :key="item"
              type="button"
              :class="['register-strip__item', { 'register-strip__item--active': item === 'Subscribe' }]"
            >
              {{ item }}
            </button>
          </div>

          <div class="account-grid">
            <div v-for="item in demoAccounts" :key="item.role" class="account-item" :data-accent="item.accent">
              <span class="account-item__role">{{ item.role }}</span>
              <strong>{{ item.account }}</strong>
              <p>{{ item.description }}</p>
            </div>
          </div>
        </div>

        <div class="media-card media-card--signup">
          <div class="media-card__portrait media-card__portrait--student"></div>
          <div class="signup-strip">
            <span>Role preview</span>
            <strong>Student / Teacher / Admin</strong>
          </div>
        </div>
      </section>

      <section class="community-section" id="community">
        <div class="section-copy section-copy--centered">
          <span class="section-copy__eyebrow">community</span>
          <h2>Start growing with our community-style demo flow</h2>
          <p>
            从作品提交到结果发布，平台把每个阶段的动作与反馈连接起来，
            帮助课程组织者建立更稳定、更透明的项目学习体验。
          </p>
          <button class="secondary-cta" type="button">Join community</button>
        </div>

        <div class="community-visual">
          <div class="community-visual__badge community-visual__badge--left">peer review</div>
          <div class="community-visual__badge community-visual__badge--right">teacher feedback</div>
          <div class="community-visual__person"></div>
          <div class="flow-grid">
            <div v-for="step in flowSteps" :key="step.title" class="flow-card">
              <span class="flow-card__index">{{ step.title }}</span>
              <p>{{ step.description }}</p>
            </div>
          </div>
        </div>
      </section>

      <footer class="landing-footer">
        <div class="brand-mark brand-mark--footer">
          <span class="brand-mark__icon">C</span>
          <div>
            <strong>Course Demo</strong>
            <small>education review platform</small>
          </div>
        </div>

        <div class="landing-footer__group">
          <strong>Follow us</strong>
          <span>Dribbble</span>
          <span>Behance</span>
          <span>Twitter</span>
        </div>

        <div class="landing-footer__group">
          <strong>Useful links</strong>
          <span>Course review demo</span>
          <span>Student workflow</span>
          <span>Teacher dashboard</span>
        </div>

        <div class="landing-footer__group">
          <strong>Contacts</strong>
          <span>support@course-demo.local</span>
          <span>Shanghai / Remote demo</span>
        </div>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  background:
    radial-gradient(circle at left top, rgba(255, 186, 124, 0.18) 0%, rgba(255, 186, 124, 0) 20%),
    radial-gradient(circle at right 18%, rgba(255, 221, 168, 0.22) 0%, rgba(255, 221, 168, 0) 22%),
    linear-gradient(180deg, #fffaf2 0%, #fff7ea 100%);
}

.login-page__glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(10px);
  pointer-events: none;
}

.login-page__glow--left {
  width: 360px;
  height: 360px;
  top: -120px;
  left: -80px;
  background: radial-gradient(circle, rgba(255, 171, 76, 0.2) 0%, rgba(255, 171, 76, 0) 72%);
}

.login-page__glow--right {
  width: 320px;
  height: 320px;
  right: -90px;
  top: 260px;
  background: radial-gradient(circle, rgba(255, 208, 102, 0.2) 0%, rgba(255, 208, 102, 0) 72%);
}

.topbar {
  width: min(1020px, calc(100vw - 56px));
  margin: 0 auto;
  padding: 24px 0 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  position: relative;
  z-index: 1;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ffab43 0%, #ff8a1d 100%);
  color: #fff;
  font-weight: 800;
  font-size: 20px;
  box-shadow: 0 14px 28px rgba(255, 140, 34, 0.28);
}

.brand-mark strong,
.brand-mark small {
  display: block;
}

.brand-mark strong {
  color: #2e261d;
  font-size: 16px;
}

.brand-mark small {
  color: #8a7763;
  text-transform: lowercase;
}

.topbar__nav {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6f5f50;
  font-size: 13px;
  font-weight: 600;
}

.topbar__nav a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 82px;
  height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  background: linear-gradient(180deg, #fff4e0 0%, #ffe5bf 100%);
  border: 1px solid rgba(229, 171, 93, 0.72);
  box-shadow: 0 10px 18px rgba(222, 162, 80, 0.12);
  color: #7a5426;
}

.topbar__cta {
  border: none;
  border-radius: 999px;
  padding: 10px 18px;
  background: #ff8b1f;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 16px 28px rgba(255, 139, 31, 0.24);
}

.landing-shell {
  width: min(1020px, calc(100vw - 56px));
  margin: 0 auto;
  padding: 8px 0 56px;
  position: relative;
  z-index: 1;
}

.hero-section,
.content-section,
.community-section {
  display: grid;
  align-items: center;
}

.hero-section {
  grid-template-columns: minmax(0, 1fr) 332px;
  gap: 52px;
  padding: 28px 0 70px;
}

.hero-copy__badge,
.section-copy__eyebrow,
.login-card__eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: #fff2d8;
  color: #d87512;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.hero-copy h1,
.section-copy h2,
.login-card h3 {
  margin: 0;
  color: #1f170f;
}

.hero-copy h1 {
  margin-top: 16px;
  font-size: 52px;
  line-height: 1.02;
  letter-spacing: -0.04em;
  max-width: 480px;
}

.hero-copy p,
.section-copy p,
.login-card p,
.feature-list li,
.flow-card p {
  color: #7e6e5f;
  line-height: 1.8;
}

.hero-copy > p {
  max-width: 460px;
  margin: 14px 0 0;
  font-size: 14px;
}

.hero-copy__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 24px;
}

.primary-cta {
  border: none;
  border-radius: 999px;
  padding: 11px 18px;
  background: #7f3d12;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.hero-copy__note {
  color: #a18d78;
  font-size: 13px;
}

.hero-copy__list,
.feature-list {
  padding: 0;
  margin: 22px 0 0;
  list-style: none;
  max-width: 420px;
}

.hero-copy__list li,
.feature-list li {
  position: relative;
  padding-left: 24px;
  margin-bottom: 10px;
}

.hero-copy__list li::before,
.feature-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffb54b 0%, #ff8a1d 100%);
}

.login-card,
.media-card,
.community-section,
.account-item,
.flow-card {
  box-shadow: 0 28px 60px rgba(193, 145, 82, 0.12);
}

.login-card {
  align-self: start;
  border-radius: 30px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 236, 210, 0.85);
}

.login-card__illustration {
  position: relative;
  min-height: 210px;
  padding: 28px;
  background: linear-gradient(180deg, #ffe8b9 0%, #ffd07d 100%);
  overflow: hidden;
}

.login-card__bubble {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
}

.login-card__bubble--large {
  width: 168px;
  height: 168px;
  top: -36px;
  right: -24px;
}

.login-card__bubble--small {
  width: 76px;
  height: 76px;
  left: 28px;
  bottom: 30px;
}

.login-card__spark {
  position: absolute;
  background: #fff7e7;
  opacity: 0.9;
}

.login-card__spark--one {
  top: 36px;
  left: 42px;
  width: 18px;
  height: 18px;
  clip-path: polygon(50% 0, 62% 38%, 100% 50%, 62% 62%, 50% 100%, 38% 62%, 0 50%, 38% 38%);
}

.login-card__spark--two {
  right: 116px;
  top: 62px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.login-card__avatar {
  position: relative;
  z-index: 1;
  width: 102px;
  height: 102px;
  margin-left: auto;
  border-radius: 32px;
  background: linear-gradient(180deg, #fff6e4 0%, #ffffff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff8b1f;
  font-size: 38px;
  font-weight: 800;
  box-shadow: 0 20px 40px rgba(197, 126, 25, 0.2);
}

.login-card__card {
  position: relative;
  z-index: 1;
  width: min(100%, 230px);
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(10px);
}

.login-card__card strong,
.login-card__card span {
  display: block;
}

.login-card__card strong {
  margin-bottom: 8px;
  color: #392617;
}

.login-card__card span {
  color: #7f6851;
  line-height: 1.6;
}

.login-card__body {
  padding: 24px 24px 26px;
}

.login-card__body h3 {
  margin-top: 16px;
  font-size: 28px;
}

.login-card__body p {
  margin: 10px 0 18px;
}

.login-submit {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #ffac44 0%, #ff8a1d 100%);
  box-shadow: 0 18px 30px rgba(255, 142, 41, 0.24);
}

.content-section {
  grid-template-columns: minmax(0, 300px) minmax(0, 1fr);
  gap: 58px;
  padding: 42px 0;
}

.content-section--reverse {
  grid-template-columns: minmax(0, 1fr) minmax(0, 300px);
}

.media-card {
  position: relative;
  min-height: 280px;
  border-radius: 32px;
  background: linear-gradient(180deg, #fff5de 0%, #ffe7b0 100%);
  overflow: hidden;
  padding: 20px;
}

.media-card__portrait {
  position: absolute;
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.28) 0%, rgba(255, 255, 255, 0) 100%),
    linear-gradient(135deg, #f6c792 0%, #ffe9c2 100%);
}

.media-card__portrait--teacher {
  left: 22px;
  bottom: 22px;
  width: 152px;
  height: 188px;
}

.media-card__portrait--student {
  right: 22px;
  top: 24px;
  width: 146px;
  height: 186px;
}

.media-card__floating-note,
.signup-strip {
  position: absolute;
  right: 24px;
  bottom: 26px;
  width: 220px;
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(10px);
}

.media-card__floating-note strong,
.signup-strip strong,
.signup-strip span {
  display: block;
}

.media-card__floating-note strong {
  margin-bottom: 8px;
  color: #2e261d;
}

.media-card__floating-note p {
  margin: 0;
  color: #7e6e5f;
  line-height: 1.7;
}

.section-copy h2 {
  margin-top: 16px;
  max-width: 420px;
  font-size: 42px;
  line-height: 1.12;
  letter-spacing: -0.03em;
}

.section-copy p {
  max-width: 620px;
  margin: 18px 0 0;
}

.register-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 560px;
  padding: 12px;
  margin-top: 24px;
  border-radius: 14px;
  background: #ffe9b9;
}

.register-strip__item {
  height: 42px;
  border: none;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.85);
  color: #8b755e;
  font-size: 12px;
  font-weight: 700;
}

.register-strip__item--active {
  background: #cb5b17;
  color: #fff;
}

.account-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 24px;
  max-width: 640px;
}

.account-item {
  padding: 20px 18px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 237, 214, 0.9);
}

.account-item[data-accent='admin'] {
  background: linear-gradient(180deg, #fffdf9 0%, #fff3dc 100%);
}

.account-item[data-accent='teacher'] {
  background: linear-gradient(180deg, #fffdf7 0%, #ffeccd 100%);
}

.account-item[data-accent='student'] {
  background: linear-gradient(180deg, #fffdf8 0%, #fff0d5 100%);
}

.account-item__role {
  display: inline-flex;
  margin-bottom: 12px;
  color: #d37b18;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.account-item strong {
  display: block;
  margin-bottom: 8px;
  color: #24190f;
  font-size: 22px;
}

.account-item p {
  margin: 0;
  color: #806d58;
}

.signup-strip {
  bottom: 24px;
  right: 24px;
  width: auto;
  min-width: 224px;
}

.signup-strip span {
  margin-bottom: 8px;
  color: #af7c3a;
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.signup-strip strong {
  color: #2f2316;
  line-height: 1.5;
}

.section-copy--centered {
  text-align: center;
  justify-self: center;
}

.section-copy--centered p {
  max-width: 520px;
}

.secondary-cta {
  margin-top: 22px;
  border: none;
  border-radius: 999px;
  padding: 10px 18px;
  background: #ff8b1f;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.community-section {
  gap: 34px;
  padding: 54px 34px 38px;
  margin-top: 18px;
  border-radius: 36px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(255, 236, 210, 0.82);
}

.community-visual {
  position: relative;
  min-height: 360px;
}

.community-visual__person {
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 250px;
  height: 280px;
  border-radius: 48px 48px 20px 20px;
  background:
    radial-gradient(circle at 50% 20%, #ffe7c4 0%, #ffe7c4 18%, transparent 19%),
    linear-gradient(180deg, #ff5c1d 0%, #ea4611 100%);
  box-shadow: 0 22px 38px rgba(226, 102, 36, 0.18);
}

.community-visual__badge {
  position: absolute;
  top: 54px;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #a86a20;
  font-size: 12px;
  font-weight: 700;
}

.community-visual__badge--left {
  left: 160px;
}

.community-visual__badge--right {
  right: 160px;
}

.flow-grid {
  position: absolute;
  inset: 0;
}

.flow-card {
  position: absolute;
  width: 220px;
  padding: 22px 20px;
  border-radius: 24px;
  background: linear-gradient(180deg, #fffdf9 0%, #fff0d2 100%);
  border: 1px solid rgba(255, 235, 208, 0.92);
}

.flow-card:nth-child(1) {
  left: 80px;
  top: 128px;
}

.flow-card:nth-child(2) {
  right: 72px;
  top: 132px;
}

.flow-card:nth-child(3) {
  left: 50%;
  bottom: 30px;
  transform: translateX(-50%);
}

.flow-card__index {
  display: inline-flex;
  margin-bottom: 10px;
  color: #d97716;
  font-size: 14px;
  font-weight: 700;
}

.flow-card p {
  margin: 0;
}

.landing-footer {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr 1.2fr;
  gap: 24px;
  align-items: start;
  padding: 28px 34px;
  margin-top: 22px;
  border-radius: 0 0 28px 28px;
  background: #ffe9b9;
  color: #7b654e;
}

.brand-mark--footer .brand-mark__icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  font-size: 18px;
}

.landing-footer__group strong,
.landing-footer__group span {
  display: block;
}

.landing-footer__group strong {
  margin-bottom: 10px;
  color: #37261a;
}

.landing-footer__group span {
  margin-bottom: 6px;
  font-size: 14px;
}

:deep(.login-card .el-form-item__label) {
  color: #745f4c;
  font-weight: 600;
}

:deep(.login-card .el-input__wrapper) {
  min-height: 48px;
  border-radius: 16px;
  background: #fffaf3;
  box-shadow: inset 0 0 0 1px rgba(237, 205, 163, 0.56);
}

@media (max-width: 1180px) {
  .hero-section,
  .content-section,
  .content-section--reverse {
    grid-template-columns: 1fr;
  }

  .account-grid,
  .flow-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .topbar {
    flex-wrap: wrap;
  }

  .topbar__nav {
    order: 3;
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }

  .hero-copy h1 {
    font-size: 42px;
  }

  .section-copy h2 {
    font-size: 34px;
  }

  .register-strip,
  .landing-footer {
    grid-template-columns: 1fr;
  }

  .community-visual {
    min-height: 540px;
  }

  .community-visual__badge--left,
  .community-visual__badge--right,
  .flow-card,
  .flow-card:nth-child(1),
  .flow-card:nth-child(2),
  .flow-card:nth-child(3) {
    position: static;
    transform: none;
    width: auto;
  }

  .flow-grid {
    position: static;
    display: grid;
    gap: 16px;
    margin-top: 24px;
  }
}
</style>
